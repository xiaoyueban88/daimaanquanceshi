package edp.davinci.dao;

import java.util.List;
import java.util.Set;

import edp.davinci.model.WidgetContainer;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/2/27
 */
@Component
public interface WidgetContainerMapper {
    int insert(WidgetContainer widgetContainer);

    @Delete({"delete from widget_container where id = #{id}"})
    int deleteById(@Param("id") Long id);

    @Select({"select * from widget_container where id = #{id}"})
    WidgetContainer getById(@Param("id") Long id);

    @Select({"select * from widget_container where project_id = #{projectId} and folder_id = #{folderId}"})
    List<WidgetContainer> getByFolderId(@Param("projectId") Long projectId, @Param("folderId") Long folderId);

    @Update({
            "update widget_container",
            "set `name` = #{name,jdbcType=VARCHAR},",
            "`description` = #{description,jdbcType=VARCHAR},",
            "`project_id` = #{projectId,jdbcType=BIGINT},",
            "`config` = #{config,jdbcType=LONGVARCHAR},",
            "`dispose` = #{dispose, jdbcType=LONGVARCHAR},",
            "`update_by` = #{updateBy,jdbcType=BIGINT},",
            "`update_time` = #{updateTime,jdbcType=TIMESTAMP}",
            "where id = #{id,jdbcType=BIGINT}"
    })
    int update(WidgetContainer widgetContainer);

    @Select({"select id from widget_container where project_id = #{projectId} and `name` = #{name}"})
    Long getByNameWithProjectId(@Param("name") String name, @Param("projectId") Long projectId);

    @Select({"select id from widget_container where project_id = #{projectId} and folder_id = #{folderId} and `name` = #{name}"})
    Long getByNameWithFolderId(@Param("name") String name, @Param("folderId") Long folderId, @Param("projectId") Long projectId);

    @Select({"select * from widget_container where project_id = #{projectId} order by update_time desc"})
    List<WidgetContainer> getByProject(@Param("projectId") Long projectId);

    List<WidgetContainer> getByIds(@Param("list") Set<Long> ids);

    Set<Long> getIdSetByIds(@Param("set") Set<Long> ids);

    /**
     * 根据dashboardId查询关联的所有widgetId
     *
     * @param dashboardId
     * @return
     */
    @Select({"SELECT w.* FROM mem_dashboard_widget m LEFT JOIN widget_container w on w.id = m.widget_Id WHERE m.dashboard_id = #{dashboardId} && m.widget_type=1"})
    Set<WidgetContainer> getWidgetsByDashboard(@Param("dashboardId") Long dashboardId);

    int updateFolderIdBatch(@Param("list") List<WidgetContainer> list);

    @Update({"update widget_container set `folder_id` = #{folderId} where id = #{id}"})
    int updateFolder(@Param("id") Long id, @Param("folderId") Long folderId);

    int updateDisposeBatch(@Param("list") List<WidgetContainer> list);
}
