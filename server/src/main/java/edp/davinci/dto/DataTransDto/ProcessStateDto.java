package edp.davinci.dto.DataTransDto;

import java.io.Serializable;

/**
 * @author wenzhang8
 * @date 2019/2/26
 * @description 流程节点dto
 */
public class ProcessStateDto implements Serializable {
    private static final long serialVersionUID = 4270707502250727228L;

    private String name;
    private String text;
    private String displayName;
    private String type;
    // 受理人
    private String assignee;
    // 自动执行值
    private String autoExecute;
    // 执行类型
    private String performType;
    // 任务类型
    private String taskType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getAutoExecute() {
        return autoExecute;
    }

    public void setAutoExecute(String autoExecute) {
        this.autoExecute = autoExecute;
    }

    public String getPerformType() {
        return performType;
    }

    public void setPerformType(String performType) {
        this.performType = performType;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
}
