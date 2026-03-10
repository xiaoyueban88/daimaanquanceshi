package edp.davinci.service;

import com.iflytek.edu.elp.common.dto.page.Pager;
import com.iflytek.edu.zx.etl.model.ElTask;

import edp.core.exception.ServerException;
import edp.davinci.dto.DataTransDto.ElTaskLogQueryParams;
import edp.davinci.dto.DataTransDto.ProcessDeployParams;
import edp.davinci.dto.DataTransDto.ProcessDetailDto;
import edp.davinci.dto.DataTransDto.ProcessUpdateDto;
import edp.davinci.dto.DataTransDto.ProcessWithLatestStatusDto;
import edp.davinci.model.User;


/**
 * @Description ${description}
 * @author zswu3
 * @date 2021/5/8
 */
public interface DataTransService {
    /**
     * 获取所有导出任务列表
     * @param processName
     * @param pageIndex
     * @param pageSize
     * @return
     */
    Pager<ProcessWithLatestStatusDto> getPagerProcessWithLatestStatus(String processName, int pageIndex, int pageSize);

    /**
     * 获取process 详细信息
     * @param processId
     * @return
     */
    ProcessDetailDto getProcessDetail(String processId);

    /**
     * 更新流程
     * @param processUpdate
     * @return
     */
    void updateProcess(ProcessUpdateDto processUpdate);

    /**
     * 删除流程
     * @param processId
     */
    void deleteProcess(String processId);

    /**
     * 查询日志
     * @param params
     * @return
     */
    Pager<ElTask> getPagerLog(ElTaskLogQueryParams params);

    /**
     * 任务重试
     * @param processId
     * @param dateParts
     * @param params
     */
    void taskRetry(String processId,String dateParts,String params) throws ServerException;

    /**
     *
     * @param params
     */
    void deploy(ProcessDeployParams params, User user);

}
