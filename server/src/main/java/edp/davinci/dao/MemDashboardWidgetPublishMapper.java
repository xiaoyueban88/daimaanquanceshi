package edp.davinci.dao;

import java.util.List;
import java.util.Set;

import edp.davinci.model.MemDashboardWidget;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description MemDashboardWidgetDeploy发布表
 * @date 2020/1/17
 */
@Component
public interface MemDashboardWidgetPublishMapper {
    @Delete("delete from mem_dashboard_widget_publish where widget_Id = #{widgetId} and widget_type= #{widgetType}")
    int deleteByWidget(@Param("widgetId") Long widgetId, @Param("widgetType") Integer widgetType);

    @Delete("delete from mem_dashboard_widget_publish where dashboard_id = #{dashboardId}")
    int deleteByDashboardId(@Param("dashboardId") Long dashboardId);

    @Delete({"DELETE mdw FROM mem_dashboard_widget_publish mdw WHERE mdw.dashboard_id IN " +
            "( " +
            "SELECT d.id " +
            "FROM dashboard_publish d " +
            "WHERE d.dashboard_portal_id = #{portalId} " +
            ") "})
    int deleteByPortalId(@Param("portalId") Long portalId);

    @Select({"select * from mem_dashboard_widget_publish where dashboard_id = #{dashboardId}"})
    List<MemDashboardWidget> getByDashboardId(@Param("dashboardId") Long dashboardId);

    @Delete({
            "delete from mem_dashboard_widget where dashboard_id in ",
            "(SELECT d.id FROM dashboard d LEFT JOIN dashboard_portal p on d.dashboard_portal_id = p.id where p.project_id = #{projectId})"
    })
    int deleteByProject(@Param("projectId") Long projectId);

    int insertBatch(@Param("list") List<MemDashboardWidget> list);

    @Select({
            "<script>",
            "select DISTINCT dashboard_id from mem_dashboard_widget_publish where widget_id in",
            "<foreach collection='widgetIds' item='item' open='(' separator=',' close=')'>",
            "#{item}",
            "</foreach>",
            "</script>"
    })
    Set<Long> getDashboardIdsByWidgetIds(@Param("widgetIds") Set<Long> widgetIds);

    @Select({"select widget_id from mem_dashboard_widget_publish where widget_type=0 and dashboard_id = #{dashboardId} "})
    Set<Long> getWidgetIdByDashboardId(@Param("dashboardId") Long dashboardId);

    @Select({"select config from mem_dashboard_widget_publish where widget_type=1 and dashboard_id = #{dashboardId} "})
    Set<Long> getWidgetContainerIdByDashboardId(@Param("dashboardId") Long dashboardId);
}
