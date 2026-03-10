package edp.davinci.dto.DataTransDto;

import java.io.Serializable;
import java.util.List;

/**
 * @author wenzhang8
 * @date 2019/2/26
 * @description
 */
public class NodeList implements Serializable {
    private static final long serialVersionUID = 1772331796488396790L;

    private String nodeId;
    private List<String> uuid;
    private List<String> anchor;
    private boolean isSource;
    private boolean isTarget;
    private String type;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public List<String> getUuid() {
        return uuid;
    }

    public void setUuid(List<String> uuid) {
        this.uuid = uuid;
    }

    public List<String> getAnchor() {
        return anchor;
    }

    public void setAnchor(List<String> anchor) {
        this.anchor = anchor;
    }

    public boolean isSource() {
        return isSource;
    }

    public void setSource(boolean source) {
        isSource = source;
    }

    public boolean isTarget() {
        return isTarget;
    }

    public void setTarget(boolean target) {
        isTarget = target;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
