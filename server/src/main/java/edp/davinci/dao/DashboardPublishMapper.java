package edp.davinci.dao;

import java.util.List;
import java.util.Set;

import edp.davinci.dto.dashboardDto.DashboardWithPortal;
import edp.davinci.model.Dashboard;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/1/17
 */
@Component
public interface DashboardPublishMapper {
    @Delete({"delete from dashboard_publish where id = #{id}"})
    int deleteById(@Param("id") Long id);

    @Update({"update dashboard_publish set is_delete = 1 where id = #{id}"})
    int logicDeleteById(@Param("id") Long id);

    @Delete({"delete from dashboard_publish where dashboard_portal_id = #{portalId}"})
    int deleteByPortalId(@Param("portalId") Long portalId);

    @Select({"select * from dashboard_publish where id = #{id} and is_delete = 0"})
    Dashboard getById(@Param("id") Long id);

    @Select({
            "SELECT ",
            "	d.*,",
            "	dp.id 'portal.id',",
            "	dp.`name` 'portal.name',",
            "	dp.description 'portal.description',",
            "	dp.project_id 'portal.projectId',",
            "	dp.avatar 'portal.avatar',",
            "	dp.publish 'portal.publish',",
            "	p.id 'project.id',",
            "	p.`name` 'project.name',",
            "	p.description 'project.description',",
            "	p.pic 'project.pic',",
            "	p.org_id 'project.orgId',",
            "	p.user_id 'project.userId',",
            "	p.visibility 'p.visibility'",
            "from dashboard_publish d ",
            "LEFT JOIN dashboard_portal dp on dp.id = d.dashboard_portal_id",
            "LEFT JOIN project p on p.id = dp.project_id",
            "WHERE d.id = #{dashboardId} and d.is_delete = 0"
    })
    DashboardWithPortal getDashboardWithPortalAndProject(@Param("dashboardId") Long dashboardId);

    @Delete({"delete from dashboard WHERE dashboard_portal_id in (SELECT id FROM dashboard_portal WHERE project_id = #{projectId})"})
    int deleteByProject(@Param("projectId") Long projectId);

    int insert(Dashboard dashboard);

    @Select({
            "select * from dashboard_publish where dashboard_portal_id = #{portalId} and is_delete = 0 order by `index`"
    })
    List<Dashboard> getByPortalId(@Param("portalId") Long portalId);

    @Select({"select id from dashboard_publish where is_delete = 0"})
    Set<Long> getAllDashboardIds();

    @Select({"select id from dashboard_publish where is_delete = 1"})
    Set<Long> getLogicDeletedIds();

    List<Dashboard> getListByPortalIds(@Param("list") List<Long> portalIds);

    @Select({
            "select id from dashboard_publish where dashboard_portal_id = #{portalId} and is_delete = 0"
    })
    List<Long> getIdsByPoralId(@Param("portalId") Long portalId);

}
