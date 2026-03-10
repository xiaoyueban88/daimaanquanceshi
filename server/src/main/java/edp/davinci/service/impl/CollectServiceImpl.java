package edp.davinci.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.core.model.Paginate;
import edp.core.utils.CollectionUtils;
import edp.core.utils.DateUtils;
import edp.core.utils.ExcelUtils;
import edp.core.utils.TokenUtils;
import edp.davinci.dao.CollectPaperApproverMapper;
import edp.davinci.dao.CollectPaperMapper;
import edp.davinci.dao.CollectPaperSubmitMapper;
import edp.davinci.dao.CollectPaperTopicAnswerMapper;
import edp.davinci.dao.CollectPaperTopicMapper;
import edp.davinci.dao.UserMapper;
import edp.davinci.dto.CollectDto.CollectPaperDto;
import edp.davinci.dto.CollectDto.PaperAnswerInfoDto;
import edp.davinci.dto.CollectDto.PaperApproveDto;
import edp.davinci.dto.CollectDto.PaperConfigDto;
import edp.davinci.model.CollectPaper;
import edp.davinci.model.CollectPaperApprover;
import edp.davinci.model.CollectPaperSubmit;
import edp.davinci.model.CollectPaperTopic;
import edp.davinci.model.CollectPaperTopicAnswer;
import edp.davinci.model.User;
import edp.davinci.service.CollectService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/9
 */
@Service
public class CollectServiceImpl implements CollectService {

    @Autowired
    private CollectPaperMapper collectPaperMapper;

    @Autowired
    private CollectPaperTopicMapper collectPaperTopicMapper;

    @Autowired
    private CollectPaperApproverMapper collectPaperApproverMapper;

    @Autowired
    private CollectPaperTopicAnswerMapper collectPaperTopicAnswerMapper;

    @Autowired
    private CollectPaperSubmitMapper collectPaperSubmitMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    public TokenUtils tokenUtils;

    @Override
    @Transactional
    public void createCollectPaper(CollectPaperDto collectPaperDto, User user) {
        CollectPaper collectPaper = collectPaperDto.getCollectPaper();
        List<CollectPaperTopic> topics = collectPaperDto.getTopics();
        if (null == collectPaper) {
            throw new ServerException("问卷信息不能为空");
        }
        if (CollectionUtils.isEmpty(topics)) {
            throw new ServerException("问卷题目不能为空");
        }

        // 设置问卷创建人
        collectPaper.setCreateBy(user.getUsername());
        // 设置uuid
        collectPaper.setUuid(UUID.randomUUID().toString());
        // 插入问卷信息
        int insert = collectPaperMapper.insert(collectPaper);
        if (insert < 1) {
            throw new ServerException("问卷创建失败");
        }

        // 设置题目排序和paper id
        for (int i = 0; i < topics.size(); i++) {
            topics.get(i).setOrder(i);
            topics.get(i).setPaperId(collectPaper.getId());
        }
        // 批量插入问卷题目
        int insertBatch = collectPaperTopicMapper.insertBatch(topics);
        if (insertBatch < 1) {
            throw new ServerException("问卷创建失败");
        }
    }

    @Override
    public Paginate<CollectPaper> getPagerPapers(Long projectId, String keyword, Integer pageNo, Integer pageSize, User user) {
        // 获取当前用户创建的问卷总数
        Integer count = collectPaperMapper.getMyPaperCount(projectId, user.getUsername(), keyword);

        // 分页获取问卷
        Integer start = (pageNo - 1) * pageSize;
        Integer end = pageNo * pageSize > count ? count : pageNo * pageSize;
        List<CollectPaper> myPaperByPager = collectPaperMapper.getMyPaperByPager(projectId, user.getUsername(), keyword, start, end);
        // 获取token
        myPaperByPager.forEach(paper -> {
            paper.setToken(getPaperToken(paper.getId()));
        });
        Paginate<CollectPaper> result = new Paginate();
        result.setResultList(myPaperByPager);
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        result.setTotalCount(count);
        return result;
    }

    @Override
    @Transactional
    public void updateConfigPaper(PaperConfigDto configDto) {
        // 删除问卷之前的审批人,并插入新的审批人
        collectPaperApproverMapper.delete(configDto.getPaperId());
        if (!StringUtils.isEmpty(configDto.getApprover())) {
            Long idByName = userMapper.getIdByName(configDto.getApprover());
            if (idByName == null) {
                throw new ServerException("审批人不存在");
            }

            CollectPaperApprover collectPaperApprover = new CollectPaperApprover();
            collectPaperApprover.setPaperId(configDto.getPaperId());
            collectPaperApprover.setUser(configDto.getApprover());
            collectPaperApproverMapper.insert(collectPaperApprover);
        }

        // 更改问卷配置
        configDto.setUpdateTime(new Date());
        collectPaperMapper.updatePaperConfig(configDto);
    }

    @Override
    @Transactional
    public void removePaper(Long paperId) {
        collectPaperMapper.delete(paperId);
        collectPaperApproverMapper.delete(paperId);
        collectPaperTopicMapper.delete(paperId);
        collectPaperSubmitMapper.deleteByPaperId(paperId);
    }

    @Override
    public Pair<String, List<CollectPaperTopic>> getPaperTopics(String token) {
        String paperIdStr = tokenUtils.getUsername(token);
        Long paperId = Long.parseLong(paperIdStr);
        // 判断问卷是否存在
        CollectPaper paper = collectPaperMapper.getById(paperId);
        if (paper == null) {
            throw new ServerException("问卷不存在");
        }

        judgePaperStatus(paper);

        return Pair.of(paper.getTitle(), collectPaperTopicMapper.getTopicsByPaper(paperId));
    }

    @Override
    public void submitPaperAnswer(List<CollectPaperTopicAnswer> answers, String token, User user) {
        if (CollectionUtils.isEmpty(answers)) {
            throw new ServerException("提交不能为空");
        }

        String paperIdStr = tokenUtils.getUsername(token);
        if (StringUtils.isEmpty(paperIdStr)) {
            throw new ServerException("token无效");
        }

        Long paperId = Long.parseLong(paperIdStr);
        CollectPaper paper = collectPaperMapper.getById(paperId);
        if (paper == null) {
            throw new ServerException("问卷不存在");
        }

        judgePaperStatus(paper);

        String username = null;
        if (paper.getType() == 1) {
            username = user == null ? null : user.getUsername();

            // 判断是否在第三方提交的记录
            String password = tokenUtils.getPassword(token);
            if (!StringUtils.isEmpty(password)) {
                JSONObject parse = JSONObject.parseObject(password);
                if ("2".equals(parse.get("type").toString())) {
                    username = parse.get("username").toString();
                }
            }
        }

        CollectPaperSubmit submit = new CollectPaperSubmit();
        submit.setUser(username);
        submit.setPaperId(paperId);
        // 判断是否有审批人
        String approverByPaperId = collectPaperApproverMapper.getApproverByPaperId(paperId);
        if (StringUtils.isEmpty(approverByPaperId)) {
            Short status = 1;
            submit.setStatus(status);
            submit.setApproveTime(new Date());
        }
        int insert = collectPaperSubmitMapper.insert(submit);
        if (insert <= 0) {
            throw new ServerException("提交失败");
        }

        // 插入本次提交的答案
        answers.forEach(a -> {
            a.setSubmitId(submit.getId());
        });
        collectPaperTopicAnswerMapper.insertBatch(answers);
    }

    @Override
    public Paginate<PaperApproveDto> getApproveInfo(Long projectId, User user, String keyword, Integer pageNo, Integer pageSize) {
        int count = collectPaperApproverMapper.getCount(projectId, user.getUsername(), keyword);
        Paginate<PaperApproveDto> result = new Paginate();
        result.setTotalCount(count);
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        if (count == 0) {
            return result;
        }
        Integer start = (pageNo - 1) * pageSize;
        Integer end = pageNo * pageSize > count ? count : pageNo * pageSize;
        List<Long> paperIds = collectPaperApproverMapper.getPaperIdByUser(projectId, user.getUsername(), keyword, start, end);
        if (CollectionUtils.isEmpty(paperIds)) {
            return result;
        }
        List<PaperApproveDto> paperApproveInfo = collectPaperMapper.getPaperApproveInfo(paperIds);
        result.setResultList(paperApproveInfo);
        return result;
    }

    @Override
    public PaperAnswerInfoDto getTopicAnswers(Long paperId, Integer pageNo, Integer pageSize, Short status) {
        CollectPaper paper = collectPaperMapper.getById(paperId);
        if (paper == null) {
            throw new ServerException("paper is no exist");
        }

        List<CollectPaperTopic> topics = collectPaperTopicMapper.getTopicsByPaper(paperId);

        PaperAnswerInfoDto paperAnswerInfoDto = new PaperAnswerInfoDto();
        paperAnswerInfoDto.setTopic(topics);
        paperAnswerInfoDto.setType(paper.getType());

        Integer count = collectPaperSubmitMapper.getCount(paperId, status);
        paperAnswerInfoDto.setCount(count);
        if (count == 0) {
            return paperAnswerInfoDto;
        }

        int start = (pageNo - 1) * pageSize;
        int end = pageNo * pageSize >= count ? count : pageNo * pageSize;

        List<CollectPaperSubmit> submits = collectPaperSubmitMapper.getListByPage(paperId, status, start, end);
        if (CollectionUtils.isEmpty(submits)) {
            return paperAnswerInfoDto;
        }
        List<Long> submitIds = Lists.newArrayList();
        submits.forEach(submit -> {
            submitIds.add(submit.getId());
        });

        List<CollectPaperTopicAnswer> answers = collectPaperTopicAnswerMapper.getBySubmitId(submitIds);
        List<Map<String, Object>> answerMap = getAnswerMap(answers, submits, false);

        paperAnswerInfoDto.setAnswerMap(answerMap);

        return paperAnswerInfoDto;
    }

    @Override
    public void approveSubmit(Long paperId, List<Long> submitIds, User user) {
        CollectPaper paper = collectPaperMapper.getById(paperId);
        if (paper == null) {
            throw new ServerException("paper is not exist");
        }
        String approver = collectPaperApproverMapper.getApproverByPaperId(paperId);
        if (!user.getUsername().equals(approver)) {
            throw new UnAuthorizedException("无权限进行审批");
        }

        // 对提交的记录进行审批
        collectPaperSubmitMapper.approveSubmits(approver, paperId, submitIds, new Date());
    }

    @Override
    public CollectPaperDto getPaperTopicInfo(Long paperId, User user) {
        CollectPaper paper = collectPaperMapper.getById(paperId);
        if (paper == null) {
            throw new NotFoundException("paper is not exist");
        }
        List<CollectPaperTopic> topicsByPaper = collectPaperTopicMapper.getTopicsByPaper(paperId);
        CollectPaperDto result = new CollectPaperDto();
        result.setCollectPaper(paper);
        result.setTopics(topicsByPaper);
        return result;
    }

    @Override
    public void updatePaperTopic(Long paperId, CollectPaperDto collectPaperDto) {
        String title = collectPaperDto.getCollectPaper().getTitle();
        // 修改标题
        collectPaperMapper.updateTitle(paperId, title);
        List<CollectPaperTopic> topics = collectPaperDto.getTopics();
        if (CollectionUtils.isEmpty(topics)) {
            throw new ServerException("topic is empty");
        }

        // 为topics排序并设置paperId
        for (int i = 0; i < topics.size(); i++) {
            CollectPaperTopic topic = topics.get(i);
            topic.setOrder(i);
            topic.setPaperId(paperId);
        }

        // 删除旧的topic, 插入新的topic
        collectPaperTopicMapper.delete(paperId);
        collectPaperTopicMapper.insertBatch(topics);
    }

    @Override
    public Pair<String, Workbook> download(Long paperId) {
        CollectPaper paper = collectPaperMapper.getById(paperId);
        if (paper == null) {
            throw new NotFoundException("paper is not exist");
        }
        // 获取以通过审批的提交数据
        Short status = 1;
        int count = collectPaperSubmitMapper.getCount(paperId, status);
        List<CollectPaperSubmit> submitList = collectPaperSubmitMapper.getListByPage(paperId, status, 0, count);
        List<Long> submitIds = Lists.newArrayList();
        submitList.forEach(submit -> {
            submitIds.add(submit.getId());
        });
        List<CollectPaperTopicAnswer> answers = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(submitIds)) {
            answers = collectPaperTopicAnswerMapper.getBySubmitId(submitIds);
        }

        // 获取问卷topic
        List<CollectPaperTopic> topics = collectPaperTopicMapper.getTopicsByPaper(paperId);

        // 获取excel标题列表
        List<String> titleList = Lists.newArrayList();
        topics.forEach(topic -> {
            titleList.add(topic.getFieldName());
        });
        // 添加额外属性title
        Map<String, String> extraTitleMap = Maps.newHashMap();
        extraTitleMap.put("createTime_", "提交时间");
        titleList.add("createTime_");

        if (paper.getType() == 1) {
            extraTitleMap.put("createUser_", "提交人");
            titleList.add("createUser_");
        }

        // 获取excel数据列表
        List<Map<String, Object>> dataList = getAnswerMap(answers, submitList, true);
        Workbook workbook = ExcelUtils.exportSimpleExcel(titleList, extraTitleMap, dataList);

        return Pair.of(paper.getTitle(), workbook);

    }

    @Override
    public void deleteSubmitRecord(Long submitId, User user) {
        CollectPaperSubmit submit = collectPaperSubmitMapper.getSubmitById(submitId);
        if (submit == null) {
            throw new ServerException("未找到对应的提交记录");
        }
        Long paperId = submit.getPaperId();
        CollectPaper paper = collectPaperMapper.getById(paperId);
        if (paper == null) {
            throw new ServerException("问卷不存在");
        }
        if (!user.getUsername().equals(paper.getCreateBy())) {
            throw new UnAuthorizedException("无权限删除");
        }
        int remove = collectPaperSubmitMapper.removeBatch(Lists.newArrayList(submitId));
        if (remove <= 0) {
            throw new ServerException("删除失败");
        }
    }

    @Override
    @Transactional
    public void updateSubmitRecord(Long submitId, List<CollectPaperTopicAnswer> answers, User user) {
        CollectPaperSubmit submit = collectPaperSubmitMapper.getSubmitById(submitId);
        if (submit == null) {
            throw new ServerException("未找到对应的提交记录");
        }
        Long paperId = submit.getPaperId();
        CollectPaper paper = collectPaperMapper.getById(paperId);
        if (!user.getUsername().equals(paper.getCreateBy())) {
            throw new UnAuthorizedException("当前用户暂无权限修改");
        }

        // 移除submitId对应的提交内容
        collectPaperTopicAnswerMapper.deleteBySubmitId(submitId);

        // 更新新的提交内容
        answers.forEach(an -> {
            an.setSubmitId(submitId);
        });
        collectPaperTopicAnswerMapper.insertBatch(answers);
    }

    @Override
    public List<CollectPaperTopicAnswer> getSubmitRecord(Long submitId) {
        CollectPaperSubmit submit = collectPaperSubmitMapper.getSubmitById(submitId);
        if (submit == null) {
            throw new ServerException("未发现提交记录");
        }
        List<CollectPaperTopicAnswer> answers = collectPaperTopicAnswerMapper.getBySubmitId(Lists.newArrayList(submitId));
        return answers;
    }

    private void judgePaperStatus(CollectPaper paper) {
        // 判断paper是否结束征集
        if (paper.getStatus() == 0) {
            throw new ServerException("该问卷已结束征集");
        }
        if (paper.getDeadline() != null && paper.getDeadline().getTime() <= System.currentTimeMillis()) {
            throw new ServerException("该问卷已结束征集");
        }
    }

    private String getPaperToken(Long paperId) {
        // 获取问卷配置信息
        CollectPaper paper = collectPaperMapper.getById(paperId);
        // 判断是否超过截止日期
        Date deadline = paper.getDeadline();

        if (deadline != null && deadline.getTime() <= System.currentTimeMillis()) {
            return null;
        }
        // 判断是否处于数据征集状态
        if (paper.getStatus() != 1) {
            return null;
        }

        JSONObject object = new JSONObject();
        object.put("type", paper.getType());
        // 生成token
        return tokenUtils.generatePaperToken(paperId, object.toJSONString(), deadline);
    }

    private List<Map<String, Object>> getAnswerMap(List<CollectPaperTopicAnswer> answers, List<CollectPaperSubmit> submits, Boolean dateform) {
        // 生成答案map
        List<Map<String, Object>> answerMap = Lists.newArrayList();

        if (CollectionUtils.isEmpty(answers)) {
            return answerMap;
        }

        // 遍历提交记录获取id->submit映射
        Map<Long, CollectPaperSubmit> submitMap = Maps.newHashMap();
        submits.forEach(submit -> {
            submitMap.put(submit.getId(), submit);
        });

        // answers按照submitId分类, submitId -> answers
        Map<Long, List<CollectPaperTopicAnswer>> tempMap = Maps.newHashMap();
        answers.forEach(answer -> {
            List<CollectPaperTopicAnswer> list = tempMap.get(answer.getSubmitId());
            if (CollectionUtils.isEmpty(list)) {
                List<CollectPaperTopicAnswer> objects = Lists.newArrayList(answer);
                tempMap.put(answer.getSubmitId(), objects);
            } else {
                list.add(answer);
            }
        });


        tempMap.forEach((key, value) -> {
            Map<String, Object> map = Maps.newHashMap();
            CollectPaperSubmit submit = submitMap.get(key);
            if (submit != null) {
                if (dateform) {
                    map.put("createTime_", DateUtils.toDateString(submit.getCreateTime()));
                    map.put("approveTime_", DateUtils.toDateString(submit.getApproveTime()));
                } else {
                    map.put("createTime_", submit.getCreateTime());
                    map.put("approveTime_", submit.getApproveTime());
                }
                map.put("createUser_", submit.getUser());
                map.put("status_", submit.getStatus());
                map.put("submitId_", submit.getId());
                map.put("approver_", submit.getApprover());

                value.forEach(item -> {
                    map.put(item.getFieldName(), item.getAnswer());

                });
                answerMap.add(map);
            }
        });
        return answerMap;
    }


}
