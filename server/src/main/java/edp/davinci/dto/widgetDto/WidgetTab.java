package edp.davinci.dto.widgetDto;

import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/2/28
 */
@Data
public class WidgetTab {
    /**
     * 序号
     */
    Integer index;

    /**
     * widget tab页名称
     */
    String name;

    /**
     * tab页的widgtId
     */
    Long widgetId;

    /**
     * 当前tab页是否为默认激活页面
     */
    Boolean active;
}
