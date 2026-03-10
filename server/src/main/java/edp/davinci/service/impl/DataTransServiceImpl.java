package edp.davinci.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.iflytek.edu.elp.common.dto.page.PageParam;
import com.iflytek.edu.elp.common.dto.page.Pager;
import com.iflytek.edu.elp.common.util.JSONUtils;
import com.iflytek.edu.elp.common.util.StringUtils;
import com.iflytek.edu.zx.etl.constant.ElLogQuery;
import com.iflytek.edu.zx.etl.constant.TaskStatus;
import com.iflytek.edu.zx.etl.dto.ProcessDTO;
import com.iflytek.edu.zx.etl.model.ElProcess;
import com.iflytek.edu.zx.etl.model.ElTask;
import com.iflytek.edu.zx.etl.model.Job;
import com.iflytek.edu.zx.etl.model.Node;
import com.iflytek.edu.zx.etl.model.ProcessNode;
import com.iflytek.edu.zx.etl.model.odeon.TableMetaInfoModel;
import com.iflytek.edu.zx.etl.service.ElTaskService;
import com.iflytek.edu.zx.etl.service.EtlProcessService;
import com.iflytek.edu.zx.etl.service.JobService;
import com.iflytek.edu.zx.etl.service.NodeService;
import com.iflytek.edu.zx.etl.service.OdeonApiService;
import com.iflytek.edu.zx.etl.service.ProcessNodeService;

import edp.core.exception.ServerException;
import edp.core.utils.CollectionUtils;
import edp.core.utils.KafkaUtil;
import edp.davinci.common.utils.ToolUtils;
import edp.davinci.dto.DataTransDto.ElTaskLogQueryParams;
import edp.davinci.dto.DataTransDto.NodeLine;
import edp.davinci.dto.DataTransDto.NodeList;
import edp.davinci.dto.DataTransDto.ProcessDeployParams;
import edp.davinci.dto.DataTransDto.ProcessDetailDto;
import edp.davinci.dto.DataTransDto.ProcessDto;
import edp.davinci.dto.DataTransDto.ProcessUpdateDto;
import edp.davinci.dto.DataTransDto.ProcessWithLatestStatusDto;
import edp.davinci.model.User;
import edp.davinci.service.DataTransService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @Description ${description}
 * @author zswu3
 * @date 2021/5/8
 */
@Slf4j
@Service("dataTransService")
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class DataTransServiceImpl implements DataTransService {

    private final EtlProcessService etlProcessService;

    private final JobService jobService;

    private final ElTaskService elTaskService;

    private final ProcessNodeService processNodeService;

    private final NodeService nodeService;

    private final OdeonApiService odeonApiService;

    private String addConfigTopic;

    private String delConfigTopic;

    @Override
    public Pager<ProcessWithLatestStatusDto> getPagerProcessWithLatestStatus(String processName, int pageIndex, int pageSize) {
        Pager<ProcessDTO> processPager = etlProcessService.getPagerProcess(new PageParam(pageIndex, pageSize), processName);
        Pager<ProcessWithLatestStatusDto> result = new Pager<>(processPager.getTotalCount(),
                Lists.<ProcessWithLatestStatusDto>newArrayList());
        if(CollectionUtils.isEmpty(processPager.getList())) {
            return result;
        }
        List<ProcessWithLatestStatusDto> proList = processPager.getList().stream()
                .map(p -> ProcessWithLatestStatusDto.newBuilder(p).build()).collect(Collectors.toList());
        result.setList(proList);


        List<String> processIds = result.getList().stream().map(ProcessDTO::getId).collect(Collectors.toList());
        // processId -> Job
        Map<String, Job> jobMap = jobService.getJobsByProcessIds(processIds);
        // 更新processDto job属性
        if(!CollectionUtils.isEmpty(jobMap)) {
            result.getList().forEach(processDTO -> processDTO.setJob(jobMap.get(processDTO.getId())));
        }

        // 获取processId对应的最新的任务
        List<ElTask> elTasks = elTaskService.getLatestLog(processIds);
        if(CollectionUtils.isEmpty(elTasks)) {
            return result;
        }
        // 移除null
        elTasks = elTasks.stream().filter(Objects::nonNull).collect(Collectors.toList());
        // processId -> ElTask
        Map<String, ElTask> latestTaskMap = Maps.newHashMap();
        elTasks.forEach(elTask -> {
            latestTaskMap.put(elTask.getProcessId(), elTask);
        });

        // 更新返回结果的状态信息及更新时间
        result.getList().forEach(pro -> {
            ElTask elTask = latestTaskMap.get(pro.getId());
            if(elTask != null) {
                pro.setLatestStatus(elTask.getStatus());
                pro.setLatestUpdateTime(elTask.getUpdateTime());
                pro.setLatestExecuteTime(elTask.getCreateTime());
            }
        });
        return result;
    }

    @Override
    public ProcessDetailDto getProcessDetail(String processId) {
        ElProcess elProcess = etlProcessService.getProcessDtoById(processId);
        if(null == elProcess) {
            throw new ServerException("流程不存在");
        }
        Job job = jobService.getJobByProcessId(processId);
        List<String> nodeIds = processNodeService.getAllNodeIds(processId);
        List<Node> nodeDetails = CollectionUtils.isEmpty(nodeIds)
                ? null : nodeService.getByNodeIds(nodeIds);
        return ProcessDetailDto.newBuilder()
                .withId(elProcess.getId())
                .withName(elProcess.getName())
                .withDisplayName(elProcess.getDisplayName())
                .withJob(job)
                .withNodeDetails(nodeDetails)
                .build();
    }

    @Override
    public void updateProcess(ProcessUpdateDto processUpdate) {
        // node properties 添加addFileType属性
        try {
            addFileTypeForProperties(processUpdate.getNodeDetails());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        etlProcessService.updateProcess(processUpdate.getProcess());
        jobService.updateJob(processUpdate.getJob());
        nodeService.updateProcessNode(processUpdate.getProcess().getId(), processUpdate.getNodeDetails());
    }

    @Override
    public void deleteProcess(String processId) {
        Job job = jobService.getJobByProcessId(processId);
        ElProcess elProcess = etlProcessService.getProcessDtoById(processId);
        if(null != elProcess) {
            etlProcessService.deleteProcess(processId);
            KafkaUtil.produceMessage(delConfigTopic, job.getId().toString());
        }
    }

    @Override
    public Pager<ElTask> getPagerLog(ElTaskLogQueryParams params) {
        ElLogQuery query = params.getJobQuery();
        String processId = params.getProcessId();
        if(StringUtils.isNotEmpty(query.getProcessName())){
            processId = etlProcessService.getProcessIdByName(query.getProcessName());
            if(StringUtils.isEmpty(processId)){
                return new Pager<>(0, new ArrayList<>());
            }
        }
        query.setProcessId(processId);
        Pager<ElTask> logPager = elTaskService
                .getPagerLog(new PageParam(params.getPageIndex(),params.getPageSize()),query);
        List<ElTask> logList = logPager.getList();
        // 查询流程名称
        List<String> processIds = new ArrayList<>();
        for (ElTask elTask : logList) {
            processIds.add(elTask.getProcessId());
        }
        Map<String, String> processMap = etlProcessService.getNamesByProcessIds(processIds);
        for (ElTask elTask : logList) {
            elTask.setProcessName(processMap.get(elTask.getProcessId()));
        }
        return logPager;
    }

    @Override
    public void taskRetry(String processId, String dateParts, String params) throws ServerException {
        List<String> datePartList = Lists.newArrayList(dateParts.split(","));
        Map<String, String> paramsMap = JSONUtils.parseObject(params, HashMap.class);
        //校验参数是否完整
        for(String datePart : datePartList){
            if(!"limit".equals(datePart)  && (!paramsMap.containsKey(datePart) || StringUtils.isEmpty(paramsMap.get(datePart)))){
                log.error("流程"+processId+"日期参数缺失"+datePart);
                throw new ServerException("日期参数缺失"+datePart);
            }
        }
        //执行，task表插入任务,update by tingzhang7 2019-09，任务以process为单位
        Integer id = elTaskService.getNotEndTaskByProcessId(processId);
        if(id != null){
            throw new ServerException("当前有正在执行的任务，请稍后再试");
        }
        ElTask task = new ElTask();
        task.setProcessId(processId);
        task.setStatus(TaskStatus.READY.toString());
        task.setParams(params);
        task.setStepType("TXT_TO_CLICKHOUSE");
        elTaskService.insert(task);
    }

    @Override
    public void deploy(ProcessDeployParams params, User user) throws ServerException{
        List<Node> nodeDataList = params.getFormDataList();
        ProcessDto processDto = params.getProcessProps();
        processDto.setCreator(user.getUsername());
        List<NodeList> nodeLists = params.getNodeList();
        List<NodeLine> nodeLines = params.getNodeLine();
        Job etlJob = params.getEtlJob();
        String id = params.getId();
        String model = "";
        try {
            addFileTypeForProperties(nodeDataList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        try {
            model = ToolUtils.object2XML(processDto, nodeLists, nodeLines);
            nodeService.batchInsert(nodeDataList);
            // 部署流程
            String processId = etlProcessService.deployOrRedeploy(id, model, etlJob);
            // 添加流程-节点关系，解决流程部署Job启动失败问题
            List<ProcessNode> processNodes = new ArrayList<>();
            int order = 0;
            for (Node node : nodeDataList) {
                order++;
                ProcessNode processNode = new ProcessNode();
                processNode.setNodeId(node.getId());
                processNode.setOrder(order);
                processNodes.add(processNode);
            }
            processNodeService.batchInsert(processNodes, processId);
            if (StringUtils.isNotEmpty(processId)) {// 实时入库发送job
                KafkaUtil.produceMessage(addConfigTopic, JSONObject.toJSONString(etlJob));
            }
        } catch (Exception e) {
            log.error("部署流程定义异常：" + e.getMessage() + ";model=" + model, e);
            throw new ServerException("部署流程定义异常：" + e.getMessage() + ";model=" + model, e);
        }

    }

    /**
     * 节点属性添加fileType
     * @param nodeDetails
     */
    private void addFileTypeForProperties(List<Node> nodeDetails) {
        // nodeDetails 仅有一个node元素
        for (Node nodeDetail : nodeDetails) {
            Map<String, String> properties = nodeDetail.getProperties();
            String odeonProject = properties.get("odeonProject");
            String odeonUser = properties.get("odeonUser");
            String tableName = properties.get("tableName");
            TableMetaInfoModel hiveTableMetadata = odeonApiService.getHiveTableMetadata(odeonProject, odeonUser, tableName);
            if(null == hiveTableMetadata || null == hiveTableMetadata.getOutputformat()) {
                continue;
            }
            String outputformat = hiveTableMetadata.getOutputformat();
            properties.put("fileType", outputformat.contains("parquet") ? "parquet" : "txt");
        }
    }
}
