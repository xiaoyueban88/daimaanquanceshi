package edp.davinci.dto.DataTransDto;

import java.util.Date;

import com.iflytek.edu.zx.etl.dto.ProcessDTO;

import lombok.Data;

/**
 * @Description 导出任务状态信息
 * @author zswu3
 * @date 2021/5/8
 */
public class ProcessWithLatestStatusDto extends ProcessDTO{

    public ProcessWithLatestStatusDto(ProcessDTO processDTO) {
        this.setId(processDTO.getId());
        this.setInstanceUrl(processDTO.getInstanceUrl());
        this.setDisplayName(processDTO.getDisplayName());
        this.setCreator(processDTO.getCreator());
        this.setCreateTime(processDTO.getCreateTime());
        this.setName(processDTO.getName());
        this.setJob(processDTO.getJob());
        this.setVersion(processDTO.getVersion());
        this.setState(processDTO.getState());
        this.setType(processDTO.getType());
    }

    public static Builder newBuilder (ProcessDTO processDTO) {
        return new Builder(processDTO);
    }

    /**
     * 最新的执行状态
     */
    private String latestStatus;

    /**
     * 最后更新时间
     */
    private Date latestUpdateTime;

    /**
     * 最新执行时间
     */
    private Date latestExecuteTime;

    public String getLatestStatus() {
        return latestStatus;
    }

    public void setLatestStatus(String latestStatus) {
        this.latestStatus = latestStatus;
    }

    public Date getLatestUpdateTime() {
        return latestUpdateTime;
    }

    public void setLatestUpdateTime(Date latestUpdateTime) {
        this.latestUpdateTime = latestUpdateTime;
    }

    public Date getLatestExecuteTime() {
        return latestExecuteTime;
    }

    public void setLatestExecuteTime(Date latestExecuteTime) {
        this.latestExecuteTime = latestExecuteTime;
    }

    public static final class Builder {
        private ProcessDTO processDTO;

        private String latestStatus;

        private Date latestUpdateTime;

        private Date latestExecuteTime;

        private Builder(ProcessDTO processDTO) {
            this.processDTO = processDTO;
        }

        public Builder withLatestStatus(String latestStatus) {
            this.latestStatus = latestStatus;
            return this;
        }

        public Builder withLatestUpdateTime(Date latestUpdateTime) {
            this.latestUpdateTime = latestUpdateTime;
            return this;
        }

        public Builder withLatestExecuteTime(Date latestExecuteTime) {
            this.latestExecuteTime = latestExecuteTime;
            return this;
        }

        public ProcessWithLatestStatusDto build() {
            ProcessWithLatestStatusDto pro = new ProcessWithLatestStatusDto(processDTO);
            pro.setLatestStatus(latestStatus);
            pro.setLatestUpdateTime(latestUpdateTime);
            pro.setLatestExecuteTime(latestExecuteTime);
            return pro;
        }
    }
}
