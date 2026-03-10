package edp.davinci.dto.DataTransDto;

import java.io.Serializable;

/**
 * @author wenzhang8
 * @date 2019/2/26
 * @description
 */
public class NodeLine implements Serializable {
    private static final long serialVersionUID = 7332345111949381085L;

    private String name;
    private String from;
    private String to;
    private String source;
    private String target;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
