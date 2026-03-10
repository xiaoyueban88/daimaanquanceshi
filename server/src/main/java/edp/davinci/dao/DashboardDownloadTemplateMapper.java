package edp.davinci.dao;

import edp.davinci.model.DashboardDownloadTemplate;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/11/10
 */
@Component
public interface DashboardDownloadTemplateMapper {

    int insert(DashboardDownloadTemplate template);

    @Update({
            "update dashboard_download_template set is_delete=1 where dashboard_id=#{dashboardId}"
    })
    int delete(@Param("dashboardId") Long dashboardId);

    @Select({
            "select dashboard_id, path from dashboard_download_template " +
                    "where dashboard_id=#{dashboardId} and is_delete=0"
    })
    DashboardDownloadTemplate query(@Param("dashboardId") Long dashboardId);
}
