package edp.davinci.dao;

import java.util.List;
import java.util.Set;

import edp.davinci.dto.shareDto.ShareWidget;
import edp.davinci.model.Widget;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/1/17
 */
@Component
public interface WidgetPublishMapper {

    @Delete({"delete from widget_publish where id = #{id}"})
    int deleteById(@Param("id") Long id);

    @Select({"SELECT w.*, v.model FROM mem_dashboard_widget_publish m ",
            "LEFT JOIN widget_publish w on w.id = m.widget_Id ",
            "LEFT JOIN `view` v on v.id = w.view_id",
            "WHERE m.dashboard_id = #{dashboardId} and m.widget_type = 0"})
    Set<ShareWidget> getShareWidgetsByDashboard(@Param("dashboardId") Long dashboardId);

    @Select({"select w.*,v.model from widget_publish w LEFT JOIN `view` v on v.id = w.view_id where w.id = #{id}"})
    ShareWidget getShareWidgetById(@Param("id") Long id);

    @Select({"select * from widget_publish where id = #{id}"})
    Widget getById(@Param("id") Long id);

    int insertBatch(@Param("list") List<Widget> list);

    @Delete({"delete from widget where project_id = #{projectId}"})
    int deleteByProject(@Param("projectId") Long projectId);

    int deleteBatch(@Param("set") Set<Long> ids);

    Set<ShareWidget> getShareWidgetsByIds(@Param("list") Set<Long> ids);

    @Select({
            "<script>",
            "select w.id as widgetId, v.id as viewId, v.sql as sql from widget_publish w left join view v on w.view_id=v.id where w.view_id in",
            "<foreach collection='viewIds' item='item' open='(' separator=',' close=')'>",
            "#{item}",
            "</foreach>",
            "</script>"
    })
    Set<Long> getIdsByViewIds(@Param("viewIds") Set<Long> viewIds);

    Set<Widget> getSimpleWidgetInfo(@Param("list") List<Long> dashboardId);

    Set<Widget> getWidgetNamesByIds(@Param("list") Set<Long> ids);


}
