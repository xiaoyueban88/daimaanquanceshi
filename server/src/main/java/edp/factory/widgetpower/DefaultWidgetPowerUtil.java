package edp.factory.widgetpower;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import edp.core.utils.TokenUtils;
import edp.davinci.core.common.Constants;
import edp.davinci.dto.widgetDto.WidgetPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/18
 */
@Component("defaultWidgetPowerUtil")
public class DefaultWidgetPowerUtil implements WidgetPowerUtil {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private TokenUtils tokenUtils;


    @Override
    public WidgetPermission getWidgetPermission(String accessToken) {
        String username = tokenUtils.getUsername(accessToken);
        if (username == null) {
            return WidgetPermission.builder().build();
        }
        String[] userNameSplit = username.split(Constants.SPLIT_CHAR_STRING);
        if (userNameSplit.length != 2) {
            return WidgetPermission.builder().build();
        }
        String extraInfo = username.split(Constants.SPLIT_CHAR_STRING)[1];
        Boolean hasDownloadPermission = null;
        try {
            JSONObject extra = JSON.parseObject(extraInfo);
            hasDownloadPermission = extra.getBoolean(Constants.HAS_DOWNLOAD_PERMISSION);
        } catch (Exception e) {
            logger.warn("extraInfo json error");
        }
        //hasDownloadPermission如果为false，则无下载权限按钮
        if (Boolean.FALSE.equals(hasDownloadPermission)) {
            return WidgetPermission.builder().hasDownloadPermission(false).build();
        } else {
            return WidgetPermission.builder().build();
        }

    }
}
