package edp.davinci.dto.DataTransDto;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.iflytek.edu.zx.etl.model.ElProcess;
import com.iflytek.edu.zx.etl.model.Job;
import com.iflytek.edu.zx.etl.model.Node;

/**
 * @Description ${description}
 * @author zswu3
 * @date 2021/5/10
 */
@NotNull(message = "processUpdateDto cannot be null")
public class ProcessUpdateDto {
    /**
     * 导出任务流程配置
     */
    @NotNull(message = "process cannot be null")
    private ElProcess process;

    /**
     * 任务
     */
    @NotNull(message = "job cannot be null")
    private Job job;

    /**
     * 流程节点
     */
    @NotNull(message = "nodeDetails cannot be null")
    private List<Node> nodeDetails;

    public ElProcess getProcess() {
        return process;
    }

    public void setProcess(ElProcess process) {
        this.process = process;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public List<Node> getNodeDetails() {
        return nodeDetails;
    }

    public void setNodeDetails(List<Node> nodeDetails) {
        this.nodeDetails = nodeDetails;
    }
}
