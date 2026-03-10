package edp.davinci.service;

import java.util.List;

import edp.core.model.Paginate;
import edp.davinci.dto.CollectDto.CollectPaperDto;
import edp.davinci.dto.CollectDto.PaperAnswerInfoDto;
import edp.davinci.dto.CollectDto.PaperApproveDto;
import edp.davinci.dto.CollectDto.PaperConfigDto;
import edp.davinci.model.CollectPaper;
import edp.davinci.model.CollectPaperTopic;
import edp.davinci.model.CollectPaperTopicAnswer;
import edp.davinci.model.User;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/9
 */
public interface CollectService {
    /**
     * 创建数据收集问卷
     *
     * @param collectPaperDto
     * @param user
     */
    void createCollectPaper(CollectPaperDto collectPaperDto, User user);

    /**
     * 分页获取创建的列表
     *
     * @param projectId
     * @param pageNo
     * @param pageSize
     * @return
     */
    Paginate<CollectPaper> getPagerPapers(Long projectId, String keyword, Integer pageNo, Integer pageSize, User user);

    /**
     * 更新问卷配置
     *
     * @param configDto
     */
    void updateConfigPaper(PaperConfigDto configDto);

    /**
     * 删除问卷
     *
     * @param paperId
     */
    void removePaper(Long paperId);

    /**
     * 获取问卷题目信息
     *
     * @param token
     * @return
     */
    Pair<String, List<CollectPaperTopic>> getPaperTopics(String token);

    /**
     * 提交答案
     *
     * @param answers
     */
    void submitPaperAnswer(List<CollectPaperTopicAnswer> answers, String token, User user);

    /**
     * 获取审批信息
     *
     * @param user
     * @return
     */
    Paginate<PaperApproveDto> getApproveInfo(Long projectId, User user, String keyword, Integer pageNo, Integer pageSize);

    /**
     * 获取问卷答题情况
     *
     * @param paperId
     * @param pageNo
     * @param pageSize
     * @param status
     * @return
     */
    PaperAnswerInfoDto getTopicAnswers(Long paperId, Integer pageNo, Integer pageSize, Short status);

    /**
     * 提交审批
     *
     * @param submitIds 提交id集合
     * @param user      用户
     */
    void approveSubmit(Long paperId, List<Long> submitIds, User user);


    /**
     * 获取问卷的题目信息
     *
     * @param paperId
     * @param user
     * @return
     */
    CollectPaperDto getPaperTopicInfo(Long paperId, User user);

    /**
     * 更新问卷题目
     *
     * @param paperId
     * @param collectPaperDto
     */
    void updatePaperTopic(Long paperId, CollectPaperDto collectPaperDto);

    /**
     * 下载问卷答题数据
     *
     * @param paperId
     * @return
     */
    Pair<String, Workbook> download(Long paperId);

    /**
     * 移除提交记录
     *
     * @param submitId
     * @param user
     */
    void deleteSubmitRecord(Long submitId, User user);

    /**
     * 修改提交内容
     *
     * @param submitId
     * @param answers
     * @param user
     */
    void updateSubmitRecord(Long submitId, List<CollectPaperTopicAnswer> answers, User user);

    /**
     * 根据submitId查询提交内容
     *
     * @param submitId
     * @return
     */
    List<CollectPaperTopicAnswer> getSubmitRecord(Long submitId);
}
