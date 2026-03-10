package edp.davinci.dto.DataTransDto;

import java.util.List;

import com.iflytek.edu.zx.etl.model.Job;
import com.iflytek.edu.zx.etl.model.Node;

/**
 * @Description ${description}
 * @author zswu3
 * @date 2021/5/8
 */
public class ProcessDetailDto {
    /**
     * processId
     */
    private String id;
    /**
     * process 标识名
     */
    private String name;
    /**
     * process 展示名
     */
    private String displayName;
    /**
     * process 对应的job
     */
    private Job job;

    /**
     * process关联的节点信息
     */
    private List<Node> nodeDetails;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder{
        private String id;
        private String name;
        private String displayName;
        private Job job;
        private List<Node> nodeDetails;

        public ProcessDetailDto build() {
            ProcessDetailDto processDetailDto = new ProcessDetailDto();
            processDetailDto.setId(id);
            processDetailDto.setName(name);
            processDetailDto.setDisplayName(displayName);
            processDetailDto.setJob(job);
            processDetailDto.setNodeDetails(nodeDetails);
            return processDetailDto;
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder withJob(Job job) {
            this.job = job;
            return this;
        }

        public Builder withNodeDetails(List<Node> nodeDetails) {
            this.nodeDetails = nodeDetails;
            return this;
        }
    }
}
