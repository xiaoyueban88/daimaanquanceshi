package edp.factory.widgetpower;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/6/15
 */
@Component("widgetPowerFactory")
public class WidgetPowerFactory {

    @Autowired
    private DatavWidgetPowerUtil datavWidgetPowerUtil;

    @Autowired
    private DefaultWidgetPowerUtil defaultWidgetPowerUtil;

    public WidgetPowerUtil getWidgetPowerUtil(String type) {
        if ("datav".equals(type)) {
            return datavWidgetPowerUtil;
        } else {
            return defaultWidgetPowerUtil;
        }
    }
}
