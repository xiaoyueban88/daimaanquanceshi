package edp.core.enums;

/**
 * @author wenzhang8
 * @date 2019/2/27
 * @description
 */
public enum NodeType {
    START("start"),
    TASK("task"),
    END("end");

    NodeType(String type) {
        this.type = type;
    }

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
