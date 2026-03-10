package edp.davinci.service.impl;

import java.util.List;
import java.util.Set;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.davinci.core.enums.LogNameEnum;
import edp.davinci.core.enums.VizEnum;
import edp.davinci.dao.DashboardMapper;
import edp.davinci.dao.DashboardPublishMapper;
import edp.davinci.dao.MemDashboardWidgetMapper;
import edp.davinci.dao.MemDashboardWidgetPublishMapper;
import edp.davinci.dao.WidgetContainerMapper;
import edp.davinci.dao.WidgetContainerPublishMapper;
import edp.davinci.dao.WidgetMapper;
import edp.davinci.dao.WidgetPublishMapper;
import edp.davinci.dto.dashboardDto.DashboardWithPortal;
import edp.davinci.dto.projectDto.ProjectDetail;
import edp.davinci.dto.projectDto.ProjectPermission;
import edp.davinci.dto.widgetDto.WidgetTab;
import edp.davinci.model.Dashboard;
import edp.davinci.model.MemDashboardWidget;
import edp.davinci.model.User;
import edp.davinci.model.Widget;
import edp.davinci.model.WidgetContainer;
import edp.davinci.service.DeployService;
import edp.davinci.service.ProjectService;
import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/1/18
 */
@Service("deployService")
@Slf4j
public class DeployServiceImpl extends VizCommonService implements DeployService {

    private static final Logger optLogger = LoggerFactory.getLogger(LogNameEnum.BUSINESS_OPERATION.getName());

    @Autowired
    private WidgetPublishMapper widgetPublishMapper;

    @Autowired
    private WidgetMapper widgetMapper;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private DashboardMapper dashboardMapper;

    @Autowired
    private DashboardPublishMapper dashboardPublishMapper;

    @Autowired
    private MemDashboardWidgetMapper memDashboardWidgetMapper;

    @Autowired
    private MemDashboardWidgetPublishMapper memDashboardWidgetPublishMapper;

    @Autowired
    private WidgetContainerPublishMapper widgetContainerPublishMapper;

    @Autowired
    private WidgetContainerMapper widgetContainerMapper;

    @Override
    @Transactional
    public boolean deployWidget(Long id, User user) {
        Widget widget = widgetMapper.getById(id);
        if (null == widget) {
            log.info("widget (:{}) is not found", id);
            throw new NotFoundException("widget is not found");
        }

        ProjectPermission projectPermission = projectService.getProjectPermission(projectService.getProjectDetail(widget.getProjectId(), user, false), user);

        //校验权限
        if (!projectPermission.getDeployPermission()) {
            log.info("user {} have not permisson to deploy the widget {}", user.getUsername(), id);
            throw new UnAuthorizedException("you have not permission to deploy the widget");
        }

        // 删除widget_publish表中的widget
        widgetPublishMapper.deleteById(id);

        // 向widget_publish表中插入要发布的widget
        widget.createdBy(user.getId());
        int insert = widgetPublishMapper.insertBatch(Lists.newArrayList(widget));

        if (insert > 0) {
            optLogger.info("widget ({}) deploy by user(:{})", widget.toString());
            return true;
        } else {
            throw new ServerException("deploy widget fail");
        }
    }

    @Override
    public boolean deployWidgetContainer(Long id, User user) throws NotFoundException, ServerException, UnAuthorizedException {
        WidgetContainer widgetContainer = widgetContainerMapper.getById(id);
        if (null == widgetContainer) {
            log.info("widgetContainer (:{}) is not found", id);
            throw new NotFoundException("widgetContainer is not found");
        }

        ProjectPermission projectPermission = projectService.getProjectPermission(projectService.getProjectDetail(widgetContainer.getProjectId(), user, false), user);

        //校验权限
        if (!projectPermission.getDeployPermission()) {
            log.info("user {} have not permisson to deploy the widget {}", user.getUsername(), id);
            throw new UnAuthorizedException("you have not permission to deploy the widget");
        }

        //  删除widget_container表中的widget
        widgetContainerPublishMapper.deleteById(id);
        // 向widget_container_publish表中插入要发布的widgetcontainer记录
        widgetContainer.createdBy(user.getId());
        List<WidgetContainer> list = Lists.newArrayList(widgetContainer);
        int insertA = widgetContainerPublishMapper.insertBatch(list);

        // 删除widgetcontainer中包含的widget
        Set<Long> widgetIds = Sets.newHashSet();
        List<WidgetTab> widgetTabs = JSONArray.parseArray(widgetContainer.getConfig(), WidgetTab.class);
        widgetTabs.forEach(w -> {
            widgetIds.add(w.getWidgetId());
        });
        widgetPublishMapper.deleteBatch(widgetIds);
        // 插入widget
        List<Widget> widgets = widgetMapper.getByIds(widgetIds);
        widgets.forEach(w -> {
            w.createdBy(user.getId());
        });
        int insertB = widgetPublishMapper.insertBatch(Lists.newArrayList(widgets));

        if (insertA > 0 && insertB > 0) {
            optLogger.info("widgetContainer ({}) deploy by user(:{})", widgetContainer.toString());
            return true;
        } else {
            throw new ServerException("deploy widgetContainer fail");
        }
    }

    @Override
    @Transactional
    public boolean deployDashboard(Long dashboardId, User user) throws NotFoundException, ServerException, UnAuthorizedException {
        DashboardWithPortal dashboardWithPortalAndProject = dashboardMapper.getDashboardWithPortalAndProject(dashboardId);

        if (null == dashboardWithPortalAndProject) {
            log.info("dashboard (:{}) not found", dashboardId);
            throw new NotFoundException("dashboard is not found, please check publish status");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(dashboardWithPortalAndProject.getProject().getId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        List<Long> disablePortals = getDisableVizs(user.getId(), projectDetail.getId(), null, VizEnum.PORTAL);
        boolean isDisable = disablePortals.contains(dashboardWithPortalAndProject.getDashboardPortalId());

        List<Long> disableDashboards = getDisableVizs(user.getId(), dashboardWithPortalAndProject.getDashboardPortalId(), null, VizEnum.DASHBOARD);

        //校验权限
        if (!projectPermission.getDeployPermission() ||
                (!projectPermission.isProjectMaintainer() && (isDisable || disableDashboards.contains(dashboardWithPortalAndProject.getId())))) {
            log.info("user {} have not permisson to share the dashboard {}", user.getUsername(), user.getId());
            throw new UnAuthorizedException("you have not permission to share the dashboard");
        }

        //删除发布表中要发布的dashboard和widget的关系
        memDashboardWidgetPublishMapper.deleteByDashboardId(dashboardId);

        //删除发布表中要发布的dashboard
        dashboardPublishMapper.deleteById(dashboardId);

        // dashboard下的widgetId + dashboard下的widgetcontainer包含的widgetId
        Set<Long> widgetIds = Sets.newHashSet();
        Set<Widget> widgetSet = Sets.newHashSet();

        // 删除发布表中本次需要发布的widgetcontainer
        Set<WidgetContainer> widgetContainers = widgetContainerMapper.getWidgetsByDashboard(dashboardId);
        Set<Long> widgetContainerIds = Sets.newHashSet();

        widgetContainers.forEach((elem) -> {
            elem.createdBy(user.getId());
            widgetContainerIds.add(elem.getId());
            List<WidgetTab> widgetTabs = JSONArray.parseArray(elem.getConfig(), WidgetTab.class);
            widgetTabs.forEach(w -> {
                widgetIds.add(w.getWidgetId());
            });
        });
        widgetContainerPublishMapper.deleteBatch(widgetContainerIds);
        // widgetcontainer 发布表中插入本次要更新的widgetcontainer
        int widgetContainerInsert = 1;
        if (!Collections.isEmpty(widgetContainers)) {
            widgetContainerInsert = widgetContainerPublishMapper.insertBatch(Lists.newArrayList(widgetContainers));
        }
        // 获取widgetcontainer下的widge
        if (!Collections.isEmpty(widgetIds)) {
            widgetSet.addAll(widgetMapper.getByIds(widgetIds));
        }

        //删除发布表中本次需要发布的widget
        int widgetInsert = 1;
        Set<Widget> widgets = widgetMapper.getWidgetsByDashboard(dashboardId);
        if (!Collections.isEmpty(widgets)) {
            widgets.forEach((elem) -> {
                elem.createdBy(user.getId());
                if (!widgetIds.contains(elem.getId())) {
                    widgetIds.add(elem.getId());
                    widgetSet.add(elem);
                }
            });
        }

        widgetPublishMapper.deleteBatch(widgetIds);
        //发布表中批量插入本次发布的widget
        if (!Collections.isEmpty(widgetSet)) {
            widgetInsert = widgetPublishMapper.insertBatch(Lists.newArrayList(widgetSet));
        }

        //发布表中插入本次要发布的dashboard
        Dashboard dashboard = dashboardMapper.getById(dashboardId);
        dashboard.createdBy(user.getId());
        int dashBoardInsert = dashboardPublishMapper.insert(dashboard);

        //发布表中插入本次要发布的dashboard和widget的关系
        List<MemDashboardWidget> memLists = memDashboardWidgetMapper.getByDashboardId(dashboardId);
        memLists.forEach(mem -> {
            mem.createdBy(user.getId());
        });
        int memInsert = memDashboardWidgetPublishMapper.insertBatch(memLists);

        if (widgetInsert > 0 && widgetContainerInsert > 0 && dashBoardInsert > 0 && memInsert > 0) {
            optLogger.info("dashboard ({}) deploy by user(:{})", dashboard.toString());
            return true;
        } else {
            throw new ServerException("deploy dashboard fail");
        }
    }
}
