package edp.factory.widgetpower;

import javax.annotation.Resource;

import com.iflytek.edu.zx.table.admin.module.ReportWidgetPower;
import com.iflytek.edu.zx.table.admin.service.ReportService;

import edp.core.utils.TokenUtils;
import edp.davinci.core.common.Constants;
import edp.davinci.dto.widgetDto.WidgetPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/6/15
 */
@Component("datavWidgetPowerUtil")
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class DatavWidgetPowerUtil implements WidgetPowerUtil {

    @Autowired
    private TokenUtils tokenUtils;

    private final ReportService reportService;


    @Override
    public WidgetPermission getWidgetPermission(String acessToken) {
        String username = tokenUtils.getUsername(acessToken);
        String[] userNameSplit = username.split(Constants.SPLIT_CHAR_STRING);
        if (userNameSplit.length != 3) {
            return WidgetPermission.builder().build();
        }
        String extraInfo = username.split(Constants.SPLIT_CHAR_STRING)[1];
        String[] split = extraInfo.split(",");
        if (split.length < 2) {
            return WidgetPermission.builder().build();
        }
        Integer reportId = Integer.parseInt(split[0]);
        Integer userId = Integer.parseInt(split[1]);
        ReportWidgetPower widgetPower = reportService.getWidgetPower(userId, reportId);
        return WidgetPermission.builder().downloadableIds(widgetPower.getDownloadableIds())
                .hideWidgetIds(widgetPower.getHideWidgetIds()).shieldWidgetIds(widgetPower.getShieldWidgetIds())
                .showWidgetIds(widgetPower.getShowWidgetIds()).undownloadableId(widgetPower.getUndownloadableId()).build();
    }
}
