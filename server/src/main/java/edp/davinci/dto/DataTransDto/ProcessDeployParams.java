package edp.davinci.dto.DataTransDto;

import java.util.List;


import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.iflytek.edu.zx.etl.model.Job;
import com.iflytek.edu.zx.etl.model.Node;


/**
 * @Description ${description}
 * @author zswu3
 * @date 2021/5/24
 */
public class ProcessDeployParams {
    /**
     * 流程id
     */
    String id;

    @NotNull(message = "processProps cannot be null")
    ProcessDto processProps;

    @NotNull(message = "nodeList cannot be null")
    List<NodeList> nodeList;

    @NotNull(message = "nodeLine cannot be null")
    List<NodeLine> nodeLine;

    @NotNull(message = "formDataList cannot be null")
    List<Node> formDataList;

    Job etlJob;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ProcessDto getProcessProps() {
        return processProps;
    }

    public void setProcessProps(ProcessDto processProps) {
        this.processProps = processProps;
    }

    public List<NodeList> getNodeList() {
        return nodeList;
    }

    public void setNodeList(List<NodeList> nodeList) {
        this.nodeList = nodeList;
    }

    public List<NodeLine> getNodeLine() {
        return nodeLine;
    }

    public void setNodeLine(List<NodeLine> nodeLine) {
        this.nodeLine = nodeLine;
    }

    public List<Node> getFormDataList() {
        return formDataList;
    }

    public void setFormDataList(List<Node> formDataList) {
        this.formDataList = formDataList;
    }

    public Job getEtlJob() {
        return etlJob;
    }

    public void setEtlJob(Job etlJob) {
        this.etlJob = etlJob;
    }
}
