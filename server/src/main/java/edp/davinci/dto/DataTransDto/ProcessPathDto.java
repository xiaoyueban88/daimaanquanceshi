package edp.davinci.dto.DataTransDto;

import java.io.Serializable;

/**
 * @author wenzhang8
 * @date 2019/2/26
 * @description 流程路径dto
 */
public class ProcessPathDto implements Serializable {
    private static final long serialVersionUID = 5994465264982257915L;

    // 名称
    private String name;
    // 出发点
    private String from;
    // 目的点
    private String to;

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
}
