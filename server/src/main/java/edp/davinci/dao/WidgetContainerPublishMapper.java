package edp.davinci.dao;

import java.util.List;
import java.util.Set;

import edp.davinci.dto.shareDto.ShareWidget;
import edp.davinci.model.WidgetContainer;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/3/1
 */
@Component
public interface WidgetContainerPublishMapper {

    int insertBatch(@Param("list") List<WidgetContainer> list);

    @Delete({"delete from widget_container_publish where id = #{id}"})
    int deleteById(@Param("id") Long id);

    @Select({"select * from widget_container_publish where id = #{id}"})
    WidgetContainer getById(@Param("id") Long id);

    @Update({
            "update widget_container_publish",
            "set `name` = #{name,jdbcType=VARCHAR},",
            "`description` = #{description,jdbcType=VARCHAR},",
            "`project_id` = #{projectId,jdbcType=BIGINT},",
            "`config` = #{config,jdbcType=LONGVARCHAR},",
            "`dispose` = #{dispose, jdbcType=LONGVARCHAR}",
            "`update_by` = #{updateBy,jdbcType=BIGINT},",
            "`update_time` = #{updateTime,jdbcType=TIMESTAMP}",
            "where id = #{id,jdbcType=BIGINT}"
    })
    int update(WidgetContainer widgetContainer);

    @Select({"select * from widget_container_publish where project_id = #{projectId} order by update_time desc"})
    List<WidgetContainer> getByProject(@Param("projectId") Long projectId);

    @Select({"SELECT w.*, m.widget_type FROM mem_dashboard_widget_publish m ",
            "LEFT JOIN widget_container_publish w on w.id = m.widget_Id ",
            "WHERE m.dashboard_id = #{dashboardId} and m.widget_type = 1"})
    Set<ShareWidget> getShareWidgetsByDashboard(@Param("dashboardId") Long dashboardId);

    int deleteBatch(@Param("set") Set<Long> ids);

    @Select({"SELECT w.config FROM mem_dashboard_widget_publish m ",
            "LEFT JOIN widget_container_publish w on w.id = m.widget_Id ",
            "WHERE m.dashboard_id = #{dashboardId} and m.widget_type = 1"})
    Set<String> getConfigByDashboardId(@Param("dashboardId") Long dashboardId);
}
