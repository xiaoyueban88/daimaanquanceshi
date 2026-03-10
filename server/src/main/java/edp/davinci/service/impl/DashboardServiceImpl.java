/*
 * <<
 *  Davinci
 *  ==
 *  Copyright (C) 2016 - 2019 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package edp.davinci.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.iflytek.edu.zx.redis.client.RedisClient;

import edp.core.consts.Consts;
import edp.core.consts.RedisConsts;
import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.core.utils.CollectionUtils;
import edp.core.utils.SqlUtils;
import edp.davinci.core.enums.LogNameEnum;
import edp.davinci.core.enums.UserPermissionEnum;
import edp.davinci.core.enums.VizEnum;
import edp.davinci.dao.DashboardPortalMapper;
import edp.davinci.dao.MemDashboardWidgetMapper;
import edp.davinci.dao.MemDashboardWidgetPublishMapper;
import edp.davinci.dao.RelRoleDashboardWidgetMapper;
import edp.davinci.dao.ViewMapper;
import edp.davinci.dao.WidgetContainerMapper;
import edp.davinci.dao.WidgetContainerPublishMapper;
import edp.davinci.dao.WidgetMapper;
import edp.davinci.dao.WidgetPublishMapper;
import edp.davinci.dto.dashboardDto.DashboardCreate;
import edp.davinci.dto.dashboardDto.DashboardDto;
import edp.davinci.dto.dashboardDto.DashboardWithMem;
import edp.davinci.dto.dashboardDto.DashboardWithPortal;
import edp.davinci.dto.dashboardDto.MemDashboardWidgetCreate;
import edp.davinci.dto.dashboardDto.MemDashboardWidgetDto;
import edp.davinci.dto.projectDto.ProjectDetail;
import edp.davinci.dto.projectDto.ProjectPermission;
import edp.davinci.dto.roleDto.VizVisibility;
import edp.davinci.dto.viewDto.ViewSqlInfo;
import edp.davinci.dto.widgetDto.WidgetTab;
import edp.davinci.model.Dashboard;
import edp.davinci.model.DashboardPortal;
import edp.davinci.model.MemDashboardWidget;
import edp.davinci.model.RelRoleDashboard;
import edp.davinci.model.RelRoleDashboardWidget;
import edp.davinci.model.Role;
import edp.davinci.model.User;
import edp.davinci.model.View;
import edp.davinci.model.Widget;
import edp.davinci.model.WidgetContainer;
import edp.davinci.service.DashboardService;
import edp.davinci.service.ProjectService;
import edp.davinci.service.ShareService;
import edp.davinci.service.WidgetContainerService;
import edp.davinci.service.WidgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static edp.core.consts.Consts.COMMA;
import static edp.core.consts.Consts.CONTAINER_WIDGET_TYPE;

@Slf4j
@Service("dashboardService")
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class DashboardServiceImpl extends VizCommonService implements DashboardService {
    private static final Logger optLogger = LoggerFactory.getLogger(LogNameEnum.BUSINESS_OPERATION.getName());

    @Autowired
    private ProjectService projectService;

    @Autowired
    private DashboardPortalMapper dashboardPortalMapper;

    @Autowired
    private RelRoleDashboardWidgetMapper relRoleDashboardWidgetMapper;

    @Autowired
    private MemDashboardWidgetMapper memDashboardWidgetMapper;

    @Autowired
    private MemDashboardWidgetPublishMapper memDashboardWidgetPublishMapper;

    @Autowired
    private ViewMapper viewMapper;

    @Autowired
    private WidgetMapper widgetMapper;

    @Autowired
    private WidgetPublishMapper widgetPublishMapper;

    @Autowired
    private ShareService shareService;

    @Autowired
    private WidgetContainerMapper widgetContainerMapper;

    @Autowired
    private WidgetContainerPublishMapper widgetContainerPublishMapper;

    private final RedisClient redisClient;

    @Autowired
    private SqlUtils sqlUtils;

    @Autowired
    private WidgetService widgetService;

    @Autowired
    private WidgetContainerService widgetContainerService;


    @Override
    public synchronized boolean isExist(String name, Long id, Long portalId) {
        Long dashboardId = dashboardMapper.getByNameWithPortalId(name, portalId);
        if (null != id && null != dashboardId) {
            return !id.equals(dashboardId);
        }
        return null != dashboardId && dashboardId.longValue() > 0L;
    }

    /**
     * 获取dashboard列表
     *
     * @param portalId
     * @param user
     * @param requiredPublish 是否要求已发布
     * @return
     */
    @Override
    public List<Dashboard> getDashboards(Long portalId, User user, Boolean requiredPublish) throws NotFoundException, UnAuthorizedException, ServerException {

        DashboardPortal dashboardPortal = dashboardPortalMapper.getById(portalId);
        if (dashboardPortal == null) {
            return null;
        }

        ProjectDetail projectDetail = null;
        try {
            projectDetail = projectService.getProjectDetail(dashboardPortal.getProjectId(), user, false);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException e) {
            return null;
        }

        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        List<Long> disablePortals = getDisableVizs(user.getId(), dashboardPortal.getProjectId(), null, VizEnum.PORTAL);

        boolean isDisable = disablePortals.contains(portalId);

        boolean hidden = projectPermission.getVizPermission() < UserPermissionEnum.READ.getPermission();
        boolean noRublish = projectPermission.getVizPermission() < UserPermissionEnum.WRITE.getPermission() && !dashboardPortal.getPublish();

        if (hidden || (!projectPermission.isProjectMaintainer() && isDisable) || noRublish) {
            return null;
        }

        List<Dashboard> dashboardList;
        if (Boolean.TRUE.equals(requiredPublish)) {
            dashboardList = dashboardPublishMapper.getByPortalId(portalId);
        } else {
            dashboardList = dashboardMapper.getByPortalId(portalId);
        }

        if (!CollectionUtils.isEmpty(dashboardList)) {
            List<Long> allDashboards = dashboardList.stream().map(Dashboard::getId).collect(Collectors.toList());
            List<Long> disableDashboards = getDisableVizs(user.getId(), portalId, allDashboards, VizEnum.DASHBOARD);
            dashboardList.removeIf(dashboard -> !projectPermission.isProjectMaintainer() && disableDashboards.contains(dashboard.getId()));
        }

        return dashboardList;
    }

    /**
     * 获取dashboard下widgets关联信息列表
     *
     * @param portalId
     * @param dashboardId
     * @param user
     * @return
     */
    @Override
    public DashboardWithMem getDashboardMemWidgets(Long portalId, Long dashboardId, User user) throws NotFoundException, UnAuthorizedException, ServerException {

        Dashboard dashboard = dashboardMapper.getById(dashboardId);
        if (null == dashboard) {
            throw new NotFoundException("dashboard is not found, please check publish status");
        }

        DashboardPortal portal = dashboardPortalMapper.getById(dashboard.getDashboardPortalId());
        if (null == portal || !portal.getId().equals(portalId)) {
            throw new ServerException("Invalid dashboard");
        }

        ProjectDetail projectDetail = null;
        try {
            projectDetail = projectService.getProjectDetail(portal.getProjectId(), user, false);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException e) {
            return null;
        }

        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        List<Long> disablePortals = getDisableVizs(user.getId(), projectDetail.getId(), null, VizEnum.PORTAL);
        boolean isDisable = disablePortals.contains(portalId);

        if (projectPermission.getVizPermission() < UserPermissionEnum.READ.getPermission() || (!projectPermission.isProjectMaintainer() && isDisable)) {
            return null;
        }

        List<MemDashboardWidget> memDashboardWidgets = memDashboardWidgetMapper.getByDashboardId(dashboardId);

        List<Long> disableDashboards = getDisableVizs(user.getId(), portalId, null, VizEnum.DASHBOARD);
        List<Long> disableMemDashboardWidget = relRoleDashboardWidgetMapper.getDisableByUser(user.getId());

        if (!CollectionUtils.isEmpty(disableDashboards)) {
            memDashboardWidgets.removeIf(memDashboardWidget -> projectPermission.getVizPermission() == UserPermissionEnum.READ.getPermission() &&
                    (disableDashboards.contains(memDashboardWidget.getDashboardId()) || disableMemDashboardWidget.contains(memDashboardWidget.getId())));
        }

        Set<Long> widgetIds = Sets.newHashSet();
        Set<Long> widgetContainerIds = Sets.newHashSet();
        memDashboardWidgets.forEach(mem -> {
            if (Consts.NORMAL_WIDGET_TYPE.equals(mem.getWidgetType())) {
                widgetIds.add(mem.getWidgetId());
            } else {
                widgetContainerIds.add(mem.getWidgetId());
            }
        });
        List<WidgetContainer> widgetContainerList = widgetContainerMapper.getByIds(widgetContainerIds);
        widgetContainerList.forEach(w -> {
            List<WidgetTab> widgetTabs = JSONArray.parseArray(w.getConfig(), WidgetTab.class);
            widgetTabs.forEach(tab -> {
                widgetIds.add(tab.getWidgetId());
            });
        });

        Set<View> views = new HashSet<>();
        if (!CollectionUtils.isEmpty(widgetIds)) {
            views = viewMapper.selectByWidgetIds(widgetIds);
        }

        DashboardWithMem dashboardWithMem = new DashboardWithMem();
        BeanUtils.copyProperties(dashboard, dashboardWithMem);
        dashboardWithMem.setWidgets(memDashboardWidgets);
        dashboardWithMem.setViews(views);

        return dashboardWithMem;
    }


    /**
     * 新建dashboard
     *
     * @param dashboardCreate
     * @param user
     * @return
     */
    @Override
    @Transactional
    public Dashboard createDashboard(DashboardCreate dashboardCreate, User user) throws NotFoundException, UnAuthorizedException, ServerException {

        DashboardPortal dashboardPortal = dashboardPortalMapper.getById(dashboardCreate.getDashboardPortalId());
        if (dashboardPortal == null) {
            throw new NotFoundException("the dashboard portal is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(dashboardPortal.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        List<Long> disablePortals = getDisableVizs(user.getId(), projectDetail.getId(), null, VizEnum.PORTAL);
        boolean isDisable = disablePortals.contains(dashboardPortal.getId());


        //校验权限
        if (projectPermission.getVizPermission() < UserPermissionEnum.WRITE.getPermission() || (!projectPermission.isProjectMaintainer() && isDisable)) {
            log.info("user {} have not permisson to create dashboard", user.getUsername());
            throw new UnAuthorizedException("you have not permission to create dashboard");
        }

        if (isExist(dashboardCreate.getName(), null, dashboardCreate.getDashboardPortalId())) {
            log.info("the dashboard \"{}\" name is already taken", dashboardCreate.getName());
            throw new ServerException("the dashboard name is already taken");
        }

        Dashboard dashboard = new Dashboard().createdBy(user.getId());
        BeanUtils.copyProperties(dashboardCreate, dashboard);

        if (null != dashboard.getParentId() && dashboard.getParentId() > 0L) {
            String fullParentId = dashboardMapper.getFullParentId(dashboard.getParentId());
            dashboard.setFullParentId(StringUtils.isEmpty(fullParentId) ? dashboard.getParentId().toString() : dashboard.getParentId() + COMMA + fullParentId);
        }

        int insert = dashboardMapper.insert(dashboard);
        if (insert > 0) {
            optLogger.info("dashboard ({}) is create by (:{})", dashboard.toString(), user.getId());
            if (!CollectionUtils.isEmpty(dashboardCreate.getRoleIds())) {
                List<Role> roles = roleMapper.getRolesByIds(dashboardCreate.getRoleIds());

                List<RelRoleDashboard> list = roles.stream()
                        .map(r -> new RelRoleDashboard(dashboard.getId(), r.getId()).createdBy(user.getId()))
                        .collect(Collectors.toList());

                if (!CollectionUtils.isEmpty(list)) {
                    relRoleDashboardMapper.insertBatch(list);
                    optLogger.info("dashboard (:{}) limit role ({}) access", dashboard.getId(), roles.stream().map(r -> r.getId()).collect(Collectors.toList()));
                }
            }

            return dashboard;
        } else {
            throw new ServerException("create dashboard fail");
        }
    }

    /**
     * 修改dashboard
     *
     * @param portalId
     * @param dashboards
     * @param user
     * @return
     */
    @Override
    @Transactional
    public void updateDashboards(Long portalId, DashboardDto[] dashboards, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        DashboardPortal dashboardPortal = dashboardPortalMapper.getById(portalId);

        if (null == dashboardPortal) {
            throw new NotFoundException("dashboard portal id not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(dashboardPortal.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);


        List<Long> disablePortals = getDisableVizs(user.getId(), projectDetail.getId(), null, VizEnum.PORTAL);
        boolean isDisable = disablePortals.contains(portalId);


        //校验权限
        if (projectPermission.getVizPermission() < UserPermissionEnum.WRITE.getPermission() || (!projectPermission.isProjectMaintainer() && isDisable)) {
            log.info("user {} have not permisson to update dashboard", user.getUsername());
            throw new UnAuthorizedException("you have not permission to update dashboard");
        }

        List<Dashboard> dashboardList = new ArrayList<>();
        Map<Long, List<Long>> rolesMap = new HashMap<>();

        Set<Long> parentIds = Arrays.stream(dashboards).map(Dashboard::getParentId).filter(pId -> pId > 0).collect(Collectors.toSet());
        Map<Long, String> parentMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(parentIds)) {
            List<Dashboard> parents = dashboardMapper.queryByParentIds(parentIds);
            if (!CollectionUtils.isEmpty(parents)) {
                Map<Long, List<Dashboard>> longListMap = parents.stream().collect(Collectors.groupingBy(Dashboard::getId));
                longListMap.forEach((k, v) -> v.stream().findFirst().ifPresent(d -> parentMap.put(k, d.getFullParentId())));
            }
        }

        List<Long> disableDashboards = getDisableVizs(user.getId(), portalId, null, VizEnum.DASHBOARD);

        for (DashboardDto dashboardDto : dashboards) {
            if (!projectPermission.isProjectMaintainer() && disableDashboards.contains(dashboardDto.getId())) {
                throw new UnAuthorizedException("you have not permission to update dashboard: \"" + dashboardDto.getName() + "\"");
            }

            if (!dashboardDto.getDashboardPortalId().equals(portalId)) {
                throw new ServerException("Invalid dashboard portal id");
            }

            if (isExist(dashboardDto.getName(), dashboardDto.getId(), dashboardDto.getDashboardPortalId())) {
                log.info("the dashboard \"{}\" name is already taken", dashboardDto.getName());
                throw new ServerException("the dashboard name \"" + dashboardDto.getName() + "\" is already taken");
            }

            dashboardDto.updatedBy(user.getId());

            if (null != dashboardDto.getParentId() && dashboardDto.getParentId() > 0L && parentMap.containsKey(dashboardDto.getParentId())) {
                String fullParentId = parentMap.get(dashboardDto.getParentId());
                dashboardDto.setFullParentId(StringUtils.isEmpty(fullParentId) ? dashboardDto.getParentId().toString() : dashboardDto.getParentId() + COMMA + fullParentId);
            } else {
                dashboardDto.setFullParentId(null);
            }

            dashboardList.add(dashboardDto);
            rolesMap.put(dashboardDto.getId(), dashboardDto.getRoleIds());
        }

        int i = dashboardMapper.updateBatch(dashboardList);

        if (i > 0) {
            optLogger.info("dashboard [{}]  is update by (:{}), origin : {}", dashboardList.toString(), user.getId(), dashboards);

            if (!CollectionUtils.isEmpty(rolesMap)) {

                Set<Long> ids = rolesMap.keySet();
                relRoleDashboardMapper.deleteByDashboardIds(ids);

                List<RelRoleDashboard> list = new ArrayList<>();
                rolesMap.forEach((dashboardId, roles) -> {
                    if (!CollectionUtils.isEmpty(roles)) {
                        list.addAll(roles.stream().map(roleId -> new RelRoleDashboard(dashboardId, roleId)).collect(Collectors.toList()));
                    }
                });
                if (!CollectionUtils.isEmpty(list)) {
                    relRoleDashboardMapper.insertBatch(list);
                }
            }
        }
    }

    /**
     * 删除dashboard
     *
     * @param id
     * @param user
     * @return
     */
    @Override
    @Transactional
    public boolean deleteDashboard(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        DashboardWithPortal dashboardWithPortalAndProject = dashboardMapper.getDashboardWithPortalAndProject(id);

        if (null == dashboardWithPortalAndProject) {
            log.info("dashboard (:{}) not found", id);
            return true;
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(dashboardWithPortalAndProject.getProject().getId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        List<Long> disableDashboards = getDisableVizs(user.getId(), dashboardWithPortalAndProject.getDashboardPortalId(), null, VizEnum.DASHBOARD);

        //校验权限
        if (projectPermission.getVizPermission() < UserPermissionEnum.WRITE.getPermission() || (!projectPermission.isProjectMaintainer() && disableDashboards.contains(id))) {
            log.info("user {} have not permisson to create dashboard", user.getUsername());
            throw new UnAuthorizedException("you have not permission to create dashboard");
        }

        List<Dashboard> deletingDashboards;
        if (0 == dashboardWithPortalAndProject.getType()) {   //folder
            deletingDashboards = dashboardMapper.getByParentId(dashboardWithPortalAndProject.getId());
        } else {
            deletingDashboards = new ArrayList<Dashboard>(1) {
                {
                    add(dashboardWithPortalAndProject);
                }
            };
        }

        if (deletingDashboards.isEmpty()) {
            return true;
        }
        for (Dashboard deletingDashboard : deletingDashboards) {
            //delete mem_dashboard_widget_publish
//            memDashboardWidgetPublishMapper.deleteByDashboardId(deletingDashboard.getId());

            //delete dashboard_publish
            dashboardPublishMapper.logicDeleteById(deletingDashboard.getId());

            //delete rel_role_dashboard_widget
//            relRoleDashboardWidgetMapper.deleteByDashboardId(deletingDashboard.getId());

            //delete mem_dashboard_widget
//            memDashboardWidgetMapper.deleteByDashboardId(deletingDashboard.getId());

            //delete rel_role_dashboard
//            relRoleDashboardMapper.deleteByDashboardId(deletingDashboard.getId());

            //delete dashboard
            dashboardMapper.logicDeleteById(deletingDashboard.getId());
        }

        optLogger.info("dashboard ({}) id delete by (:{})", JSON.toJSON(deletingDashboards), user.getId());

        return true;
    }

    /**
     * 在dashboard下新建widget关联
     *
     * @param portalId
     * @param dashboardId
     * @param memDashboardWidgetCreates
     * @param user
     * @return
     */
    @Override
    @Transactional
    public List<MemDashboardWidget> createMemDashboardWidget(Long portalId, Long dashboardId, MemDashboardWidgetCreate[] memDashboardWidgetCreates, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        DashboardWithPortal dashboardWithPortalAndProject = dashboardMapper.getDashboardWithPortalAndProject(dashboardId);

        if (null == dashboardWithPortalAndProject) {
            log.info("dashboard (:{}) not found", dashboardId);
            throw new NotFoundException("dashboard is not found, please check publish status");
        }

        if (!dashboardWithPortalAndProject.getDashboardPortalId().equals(portalId)) {
            throw new ServerException("Invalid dashboard");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(dashboardWithPortalAndProject.getProject().getId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        List<Long> disablePortals = getDisableVizs(user.getId(), projectDetail.getId(), null, VizEnum.PORTAL);
        boolean isDisable = disablePortals.contains(portalId);


        //校验权限
        if (projectPermission.getVizPermission() < UserPermissionEnum.WRITE.getPermission() || (!projectPermission.isProjectMaintainer() && isDisable)) {
            log.info("user {} have not permisson to do this operation", user.getUsername(), dashboardId);
            throw new UnAuthorizedException("Insufficient permissions");
        }

        List<Long> disableDashboards = getDisableVizs(user.getId(), portalId, null, VizEnum.DASHBOARD);

        Set<Long> ids = new HashSet<>();
        Set<Long> containerIds = Sets.newHashSet();
        List<MemDashboardWidget> list = new ArrayList<>();
        for (MemDashboardWidgetCreate memDashboardWidgetCreate : memDashboardWidgetCreates) {

            if (!projectPermission.isProjectMaintainer() && disableDashboards.contains(memDashboardWidgetCreate.getDashboardId())) {
                throw new UnAuthorizedException("Insufficient permissions");
            }

            if (memDashboardWidgetCreate.getPolling() && memDashboardWidgetCreate.getFrequency() < 1) {
                throw new ServerException("Invalid frequency");
            }

            if (Consts.NORMAL_WIDGET_TYPE.equals(memDashboardWidgetCreate.getWidgetType())) {
                ids.add(memDashboardWidgetCreate.getWidgetId());
            } else {
                containerIds.add(memDashboardWidgetCreate.getWidgetId());
            }


            MemDashboardWidget memDashboardWidget = new MemDashboardWidget().createdBy(user.getId());
            BeanUtils.copyProperties(memDashboardWidgetCreate, memDashboardWidget);
            list.add(memDashboardWidget);
        }


        List<Widget> widgets = widgetMapper.getByIds(ids);
        if (null == widgets || widgets.size() != ids.size()) {
            throw new ServerException("Invalid widget id");
        }

        for (Widget widget : widgets) {
            if (!widget.getProjectId().equals(dashboardWithPortalAndProject.getProject().getId())) {
                throw new ServerException("Invalid project id");
            }
        }

        List<WidgetContainer> widgetContainers = widgetContainerMapper.getByIds(containerIds);
        for (WidgetContainer widgetContainer : widgetContainers) {
            if (!widgetContainer.getProjectId().equals(dashboardWithPortalAndProject.getProject().getId())) {
                throw new ServerException("Invalid project id");
            }
        }

        int insert = memDashboardWidgetMapper.insertBatch(list);
        if (insert > 0) {
            optLogger.info("MemDashboardWidgets ({}) batch insert by (:{})", list.toString(), user.getId());

            List<RelRoleDashboardWidget> relRoleDashboardWidgetList = new ArrayList<>();
            for (MemDashboardWidget memDashboardWidget : list) {
                MemDashboardWidgetCreate memDashboardWidgetCreate = Arrays.stream(memDashboardWidgetCreates).filter(
                        (item -> (item.getDashboardId().longValue() == memDashboardWidget.getDashboardId().longValue()
                                && item.getWidgetId().longValue() == memDashboardWidget.getWidgetId().longValue()))
                ).findFirst().get();

                if (!CollectionUtils.isEmpty(memDashboardWidgetCreate.getRoleIds())) {
                    List<Role> roles = roleMapper.getRolesByIds(memDashboardWidgetCreate.getRoleIds());
                    relRoleDashboardWidgetList.addAll(roles.stream()
                            .map(r -> new RelRoleDashboardWidget(r.getId(), memDashboardWidget.getId()).createdBy(user.getId())).collect(Collectors.toList()));
                }
            }

            if (!CollectionUtils.isEmpty(relRoleDashboardWidgetList)) {
                relRoleDashboardWidgetMapper.insertBatch(relRoleDashboardWidgetList);
                optLogger.info("RelRoleDashboardWidgets ({}) batch insert by (:{})", relRoleDashboardWidgetList.toString(), user.getId());
            }

            return list;
        } else {
            throw new ServerException("unkown fail");
        }
    }

    /**
     * 修改dashboard下的widget关联信息
     *
     * @param portalId
     * @param user
     * @param memDashboardWidgets
     * @return
     */
    @Override
    @Transactional
    public boolean updateMemDashboardWidgets(Long portalId, User user, MemDashboardWidgetDto[] memDashboardWidgets) throws NotFoundException, UnAuthorizedException, ServerException {

        DashboardPortal dashboardPortal = dashboardPortalMapper.getById(portalId);
        if (null == dashboardPortal) {
            throw new NotFoundException("dashboard portal is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(dashboardPortal.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        List<Long> disablePortals = getDisableVizs(user.getId(), projectDetail.getId(), null, VizEnum.PORTAL);
        boolean isDisable = disablePortals.contains(portalId);

        //校验权限
        if (projectPermission.getVizPermission() < UserPermissionEnum.WRITE.getPermission() || (!projectPermission.isProjectMaintainer() && isDisable)) {
            log.info("user (:{}) have not permission to update memDashboardWidget", user.getId());
            throw new UnAuthorizedException("Insufficient permissions");
        }

        List<MemDashboardWidgetDto> dtoList = Arrays.asList(memDashboardWidgets);
        Set<Long> dIds = Sets.newHashSet();
        Set<Long> wIds = Sets.newHashSet();
        // widgetcontainer id集合
        Set<Long> cIds = Sets.newHashSet();
        // widgetcontainer 和 widget分开校验
        dtoList.forEach(dto -> {
            dIds.add(dto.getDashboardId());
            if (Consts.NORMAL_WIDGET_TYPE.equals(dto.getWidgetType())) {
                wIds.add(dto.getWidgetId());
            } else {
                cIds.add(dto.getWidgetId());
            }
        });


        Set<Long> dashboardIds = dashboardMapper.getIdSetByIds(dIds);
        Set<Long> widgetIds = widgetMapper.getIdSetByIds(wIds);
        Set<Long> widgetContainerIds = widgetContainerMapper.getIdSetByIds(cIds);

        String befor = dtoList.toString();

        List<Long> disableDashboards = getDisableVizs(user.getId(), portalId, null, VizEnum.DASHBOARD);

        List<MemDashboardWidget> memDashboardWidgetList = new ArrayList<>(dtoList.size());
        Map<Long, List<Long>> rolesMap = new HashMap<>();
        dtoList.forEach(m -> {
            if (!projectPermission.isProjectMaintainer() && disableDashboards.contains(m.getDashboardId())) {
                throw new UnAuthorizedException("Insufficient permissions");
            }

            if (!dashboardIds.contains(m.getDashboardId())) {
                throw new ServerException("Invalid dashboard id");
            }

            if (Consts.NORMAL_WIDGET_TYPE.equals(m.getWidgetType()) && !widgetIds.contains(m.getWidgetId())) {
                throw new ServerException("Invalid widget id");
            }

            if (Consts.CONTAINER_WIDGET_TYPE.equals(m.getWidgetType()) && !widgetContainerIds.contains(m.getWidgetId())) {
                throw new ServerException("Invalid widget id");
            }

            m.updatedBy(user.getId());

            memDashboardWidgetList.add(m);
            rolesMap.put(m.getId(), m.getRoleIds());
        });

        int i = memDashboardWidgetMapper.updateBatch(memDashboardWidgetList);
        if (i > 0) {
            optLogger.info("MemDashboardWidgets ({}) is update by (:{}), origin: ({})", memDashboardWidgetList.toString(), user.getId(), befor);

            if (!CollectionUtils.isEmpty(rolesMap)) {
                Set<Long> memDashboardWidgetIds = rolesMap.keySet();
                relRoleDashboardWidgetMapper.deleteByMemDashboardWidgetIds(memDashboardWidgetIds);

                List<RelRoleDashboardWidget> relRoleDashboardWidgetList = new ArrayList<>();
                for (MemDashboardWidget memDashboardWidget : memDashboardWidgetList) {
                    MemDashboardWidgetDto memDashboardWidgetDto = Arrays.stream(memDashboardWidgets).filter(
                            (item -> (item.getDashboardId().longValue() == memDashboardWidget.getDashboardId().longValue()
                                    && item.getWidgetId().longValue() == memDashboardWidget.getWidgetId().longValue()))
                    ).findFirst().get();

                    if (!CollectionUtils.isEmpty(memDashboardWidgetDto.getRoleIds())) {
                        List<Role> roles = roleMapper.getRolesByIds(memDashboardWidgetDto.getRoleIds());
                        relRoleDashboardWidgetList.addAll(roles.stream()
                                .map(r -> new RelRoleDashboardWidget(r.getId(), memDashboardWidget.getId()).createdBy(user.getId())).collect(Collectors.toList()));
                    }
                }

                if (!CollectionUtils.isEmpty(relRoleDashboardWidgetList)) {
                    relRoleDashboardWidgetMapper.insertBatch(relRoleDashboardWidgetList);
                    optLogger.info("RelRoleDashboardWidgets ({}) batch insert by (:{})", relRoleDashboardWidgetList.toString(), user.getId());
                }
            }

            return true;
        } else {
            throw new ServerException("unknown fail");
        }

    }

    /**
     * 删除dashboard下的widget关联信息
     *
     * @param relationId
     * @param user
     * @return
     */
    @Override
    @Transactional
    public boolean deleteMemDashboardWidget(Long relationId, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        MemDashboardWidget memDashboardWidget = memDashboardWidgetMapper.getById(relationId);
        if (null == memDashboardWidget) {
            optLogger.warn("MemDashboardWidget (:{}) is not found", relationId);
            return true;
        }

        DashboardWithPortal dashboardWithPortalAndProject = dashboardMapper.getDashboardWithPortalAndProject(memDashboardWidget.getDashboardId());

        if (null == dashboardWithPortalAndProject) {
            throw new ServerException("Invalid dashboard id");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(dashboardWithPortalAndProject.getProject().getId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        List<Long> disablePortals = getDisableVizs(user.getId(), dashboardWithPortalAndProject.getProject().getId(), null, VizEnum.PORTAL);
        boolean isDisable = disablePortals.contains(dashboardWithPortalAndProject.getDashboardPortalId());

        List<Long> disableDashboards = getDisableVizs(user.getId(), dashboardWithPortalAndProject.getDashboardPortalId(), null, VizEnum.DASHBOARD);


        //校验权限
        if (projectPermission.getVizPermission() < UserPermissionEnum.DELETE.getPermission()
                || (!projectPermission.isProjectMaintainer() && (isDisable || disableDashboards.contains(dashboardWithPortalAndProject.getId())))) {
            log.info("user ({}) have not permission to delete memDashboardWidget ({})", user.getId(), memDashboardWidget.getId());
            throw new UnAuthorizedException("Insufficient permissions");
        }

        relRoleDashboardWidgetMapper.deleteByMemDashboardWidgetId(relationId);

        int i = memDashboardWidgetMapper.deleteById(relationId);
        if (i > 0) {
            optLogger.info("MemDashboardWidget ({}) is delete by (:{})", memDashboardWidget.toString(), user.getId());
            return true;
        } else {
            throw new ServerException("unknown fail");
        }
    }

    /**
     * 分享dashboard
     *
     * @param dashboardId
     * @param username
     * @param user
     * @return
     */
    @Override
    public String shareDashboard(Long dashboardId, String username, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        DashboardWithPortal dashboardWithPortalAndProject = dashboardPublishMapper.getDashboardWithPortalAndProject(dashboardId);

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
        if (!projectPermission.getSharePermission() ||
                (!projectPermission.isProjectMaintainer() && (isDisable || disableDashboards.contains(dashboardWithPortalAndProject.getId())))) {
            log.info("user {} have not permisson to share the dashboard {}", user.getUsername(), user.getId());
            throw new UnAuthorizedException("you have not permission to share the dashboard");
        }


        if (dashboardWithPortalAndProject.getType() == 0) {
            throw new ServerException("dashboard folder cannot be shared");
        }

        return shareService.generateShareToken(dashboardId, username, user.getId());
    }

    @Override
    @Transactional
    public void deleteDashboardAndPortalByProject(Long projectId) throws RuntimeException {
        //delete rel_role_dashboard_widget
        relRoleDashboardWidgetMapper.deleteByProjectId(projectId);
        //删除dashboard与widget关联
        memDashboardWidgetMapper.deleteByProject(projectId);
        memDashboardWidgetPublishMapper.deleteByProject(projectId);
        //删除 rel_role_dashboard
        relRoleDashboardMapper.deleteByProject(projectId);
        //删除dashaboard
        dashboardMapper.deleteByProject(projectId);
        dashboardPublishMapper.deleteByPortalId(projectId);
        //删除 rel_role_portal
        relRolePortalMapper.deleteByProject(projectId);
        //删除dashboardPortal
        dashboardPortalMapper.deleteByProject(projectId);
        //删除widget
        widgetMapper.deleteByProject(projectId);
        widgetPublishMapper.deleteByProject(projectId);
    }

    @Override
    public List<Long> getExcludeRoles(Long id) {
        return relRoleDashboardMapper.getExecludeRoles(id);
    }

    @Override
    @Transactional
    public boolean postDashboardVisibility(Role role, VizVisibility vizVisibility, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        DashboardWithPortal dashboard = dashboardMapper.getDashboardWithPortalAndProject(vizVisibility.getId());
        if (null == dashboard) {
            throw new NotFoundException("dashboard is not found, please check publish status");
        }

        if (vizVisibility.isVisible()) {
            int delete = relRoleDashboardMapper.delete(dashboard.getId(), role.getId());
            if (delete > 0) {
                optLogger.info("dashboard ({}) can be accessed by role ({}), update by (:{})", (Dashboard) dashboard, role, user.getId());
            }
        } else {
            RelRoleDashboard relRoleDashboard = new RelRoleDashboard(dashboard.getId(), role.getId()).createdBy(user.getId());
            relRoleDashboardMapper.insert(relRoleDashboard);
            optLogger.info("dashboard ({}) limit role ({}) access, create by (:{})", (Dashboard) dashboard, role, user.getId());
        }

        return true;
    }

    @Override
    public Dashboard getDashboardById(Long id) {
        Dashboard dashboard = dashboardMapper.getById(id);
        if (null == dashboard) {
            throw new NotFoundException("dashboard not found");
        }
        return dashboard;
    }

    @Override
    public Set<Long> getDashboardIdsByTableName(Set<Long> dashboardIds, String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            return null;
        }

        Set<Long> updateDashboardIds = Sets.newHashSet();

        // viewId -> tableNames
        Map<Long, Set<String>> viewTableNameMap = Maps.newHashMap();

        // 获取dashboardId关联的table
        for (Long dashboardId : dashboardIds) {
            // 获取dashboardIds关联的widget
            Set<Long> widgetIds = Sets.newHashSet();
            Set<Long> widgetIdByDashboardId = memDashboardWidgetPublishMapper.getWidgetIdByDashboardId(dashboardId);
            if (!CollectionUtils.isEmpty(widgetIdByDashboardId)) {
                widgetIds.addAll(widgetIdByDashboardId);
            }
            // 获取dashboardIds关联的widgetContainer
            Set<String> configs = widgetContainerPublishMapper.getConfigByDashboardId(dashboardId);
            if (!CollectionUtils.isEmpty(configs)) {
                for (String config : configs) {
                    List<WidgetTab> widgetTabs = JSONArray.parseArray(config, WidgetTab.class);
                    widgetTabs.forEach(tab -> {
                        widgetIds.add(tab.getWidgetId());
                    });
                }
            }
            Set<String> tableNames = Sets.newHashSet();
            Set<ViewSqlInfo> views = viewMapper.getViewSqlInfo(widgetIds);
            if (!CollectionUtils.isEmpty(views)) {
                views.forEach(view -> {
                    Set<String> names = viewTableNameMap.get(view.getId());
                    if (names != null) {
                        tableNames.addAll(names);
                    } else {
                        Set<String> tables = sqlUtils.getTableNames(view.getId());
                        tableNames.addAll(tables);
                        viewTableNameMap.put(view.getId(), tables);
                    }
                });
            }

            if (tableNames.contains(tableName)) {
                // 判断dashboard是否完全更新
                String cache = redisClient.get(RedisConsts.DAVINCI_DASHBOARD_UPDATE_INFO, dashboardId.toString());
                if (cache == null) {
                    if (tableNames.size() == 1) {
                        updateDashboardIds.add(dashboardId);
                    } else {
                        String str = JSONObject.toJSONString(Sets.newHashSet(tableName));
                        redisClient.setex(RedisConsts.DAVINCI_DASHBOARD_UPDATE_INFO, Consts.DAY_SECOND_TIME, str, dashboardId.toString());
                    }
                } else {
                    Set<String> sets = JSONArray.parseObject(cache, Set.class);
                    sets.add(tableName);
                    if (sets.containsAll(tableNames)) {
                        updateDashboardIds.add(dashboardId);
                        redisClient.del(RedisConsts.DAVINCI_DASHBOARD_UPDATE_INFO, dashboardId.toString());
                    } else {
                        String str = JSONObject.toJSONString(sets);
                        redisClient.setex(RedisConsts.DAVINCI_DASHBOARD_UPDATE_INFO, Consts.DAY_SECOND_TIME, str, dashboardId.toString());
                    }
                }
            }
        }

        return updateDashboardIds;
    }

    @Override
    @Transactional
    public void copyDashboard(Long dashboardId, String suffix, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        Dashboard dashboard = dashboardMapper.getById(dashboardId);
        if (dashboard == null) {
            log.info("dashboard (:{}) not found", dashboardId);
            throw new NotFoundException("dashboard is not found");
        }
        dashboard.setName(dashboard.getName() + suffix);
        if (isExist(dashboard.getName(), null, dashboard.getDashboardPortalId())) {
            log.info("the dashboard \"{}\" name is already taken", dashboard.getName());
            throw new ServerException("the dashboard name is already taken");
        }

        // 复制dashboard
        int dashboardInsert = dashboardMapper.insert(dashboard);
        if (dashboardInsert <= 0) {
            log.info("dashboard inserrrt error", dashboardId);
            throw new ServerException("dashboard inserrrt error, dashboardId=" + dashboardId);
        }

        List<MemDashboardWidget> memDashboardWidgets = memDashboardWidgetMapper.getByDashboardId(dashboardId);
        if (CollectionUtils.isEmpty(memDashboardWidgets)) {
            return;
        }

        // widgetId -> newWidgetId 避免一个widget重复复制
        Map<Long, Long> widgetIdMap = Maps.newHashMap();

        // widgetcontainerId -> newWidgetContainerId 避免一个widgetcontainer重复复制
        Map<Long, Long> widgetContainerIdMap = Maps.newHashMap();

        // 重新建立关联关系
        memDashboardWidgets.forEach(mem -> {
            Long widgetId = mem.getWidgetId();
            Long newWidgetId = null;
            Integer widgetType = mem.getWidgetType();
            if (Objects.equals(Consts.NORMAL_WIDGET_TYPE, widgetType)) {
                if (widgetIdMap.get(widgetId) == null) {
                    Widget widget = widgetMapper.getById(widgetId);
                    if (widget == null) {
                        log.info("widget (:{}) not found", widgetId);
                        throw new NotFoundException("widget is not found, widgetId=" + widgetId);
                    }
                    widget.setName(widget.getName() + suffix);
                    if (widgetService.isExistNameByFolder(widget.getName(), null, widget.getFolderId(), widget.getProjectId())) {
                        log.info("the widget \"{}\" name is already taken", widget.getName());
                        throw new ServerException("the widget name is already taken");
                    }
                    widgetMapper.insert(widget);
                    newWidgetId = widget.getId();
                    widgetIdMap.put(widgetId, newWidgetId);
                } else {
                    newWidgetId = widgetIdMap.get(widgetId);
                }

            } else if (Objects.equals(CONTAINER_WIDGET_TYPE, mem.getWidgetType())) {
                if (widgetContainerIdMap.get(widgetId) == null) {
                    WidgetContainer widgetContainer = widgetContainerMapper.getById(widgetId);
                    if (widgetContainer == null) {
                        log.info("widgetContainer (:{}) not found", widgetId);
                        throw new NotFoundException("widgetContainer is not found, widgetId=" + widgetId);
                    }
                    widgetContainer.setName(widgetContainer.getName() + suffix);
                    if (widgetContainerService.isExistNameByFolder(widgetContainer.getName(), null, widgetContainer.getFolderId(),
                            widgetContainer.getProjectId())) {
                        log.info("the widgetContainer \"{}\" name is already taken", widgetContainer.getName());
                        throw new ServerException("the widgetContainer name is already taken");
                    }
                    // copy widgetcontainer中包含的widget
                    String config = widgetContainer.getConfig();
                    List<WidgetTab> widgetTabs = JSONArray.parseArray(config, WidgetTab.class);
                    widgetTabs.forEach(tab -> {
                        Widget subWidget = widgetMapper.getById(tab.getWidgetId());
                        if (subWidget != null) {
                            if (widgetIdMap.get(tab.getWidgetId()) == null) {
                                subWidget.setName(subWidget.getName() + suffix);
                                if (widgetService.isExistNameByFolder(subWidget.getName(), null, subWidget.getFolderId(),
                                        subWidget.getProjectId())) {
                                    log.info("the widget \"{}\" name is already taken", subWidget.getName());
                                    throw new ServerException("the widget name is already taken");
                                }
                                int insert = widgetMapper.insert(subWidget);
                                if (insert <= 0) {
                                    throw new ServerException("widget insert error, widgetId=" + subWidget.getId());
                                }
                                tab.setWidgetId(subWidget.getId());
                                widgetIdMap.put(tab.getWidgetId(), subWidget.getId());
                            } else {
                                tab.setWidgetId(widgetIdMap.get(tab.getWidgetId()));
                            }
                        }
                    });
                    widgetContainer.setConfig(JSONArray.toJSONString(widgetTabs));

                    widgetContainerMapper.insert(widgetContainer);
                    newWidgetId = widgetContainer.getId();
                } else {
                    newWidgetId = widgetContainerIdMap.get(widgetId);
                }

            }
            if (newWidgetId == null || newWidgetId <= 0) {
                throw new ServerException("widget insert error, widgetId=" + widgetId + ",widgetType=" + widgetType);
            }
            // 修改关联的widgetId
            mem.setWidgetId(newWidgetId);
            // 修改关联的dashboardId
            mem.setDashboardId(dashboard.getId());
            int memInsert = memDashboardWidgetMapper.insert(mem);
            if (memInsert <= 0) {
                throw new ServerException("memDashboardWidget insert error, memDashboardWidgetId=" + mem.getId());
            }
        });
    }

    @Override
    public List<Dashboard> getDashboardByProjectId(Long projectId) {
        List<DashboardPortal> portals = dashboardPortalMapper.getByProject(projectId);
        List<Long> portalIds = Lists.newArrayList();
        portals.forEach(portal -> {
            portalIds.add(portal.getId());
        });
        return dashboardPublishMapper.getListByPortalIds(portalIds);
    }
}
