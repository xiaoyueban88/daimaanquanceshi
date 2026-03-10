package edp.factory.widgetpower;


import edp.davinci.dto.widgetDto.WidgetPermission;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/6/15
 */
public interface WidgetPowerUtil {
    WidgetPermission getWidgetPermission(String accessToken);
}
