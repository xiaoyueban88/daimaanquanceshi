package edp.davinci.dto.widgetDto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/6/14
 */
@Data
@Builder
public class WidgetPermission {
    /**
     * 报表中展示的widgetId
     */
    List<Long> showWidgetIds;

    /**
     * 报表中屏蔽的widget
     */
    List<Long> shieldWidgetIds;

    /**
     * 报表中隐藏的widget
     */
    List<Long> hideWidgetIds;

    /**
     * 可下载列表
     */
    List<Long> downloadableIds;

    /**
     * 不可下载的
     */
    List<Long> undownloadableId;

    /**
    * 是否有看板下载权限
    */
    Boolean hasDownloadPermission;
}
