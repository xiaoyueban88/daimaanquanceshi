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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edp.core.exception.UnAuthorizedException;
import edp.core.utils.CollectionUtils;
import edp.core.utils.DateUtils;
import edp.core.utils.TokenUtils;
import edp.davinci.core.enums.DownloadType;
import edp.davinci.dao.*;
import edp.davinci.dto.projectDto.ProjectDetail;
import edp.davinci.dto.projectDto.ProjectPermission;
import edp.davinci.dto.viewDto.DownloadViewExecuteParam;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.dto.widgetDto.WidgetTab;
import edp.davinci.model.Dashboard;
import edp.davinci.model.MemDashboardWidget;
import edp.davinci.model.User;
import edp.davinci.model.Widget;
import edp.davinci.model.WidgetContainer;
import edp.davinci.service.ProjectService;
import edp.davinci.service.excel.WidgetContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static edp.core.consts.Consts.NORMAL_WIDGET_TYPE;
import static edp.core.consts.Consts.UNDERLINE;

@Component
@Slf4j
public class DownloadCommonService {

    @Autowired
    protected MemDashboardWidgetMapper memDashboardWidgetMapper;

    @Autowired
    protected MemDashboardWidgetPublishMapper memDashboardWidgetPublishMapper;

    @Autowired
    protected ProjectService projectService;

    @Autowired
    protected WidgetMapper widgetMapper;

    @Autowired
    protected WidgetContainerMapper widgetContainerMapper;

    @Autowired
    protected DashboardMapper dashboardMapper;

    @Autowired
    protected TokenUtils tokenUtils;

    @Value("${source.result-limit:1000000}")
    protected int resultLimit;

    protected List<WidgetContext> getWidgetContextListByDashBoardId(List<Long> dashboardIds, List<DownloadViewExecuteParam> params) {
        List<WidgetContext> widgetList = Lists.newArrayList();
        if (CollectionUtils.isEmpty(dashboardIds)) {
            return widgetList;
        }
        for (Long dashboardId : dashboardIds) {
            if (dashboardId == null || dashboardId.longValue() <= 0) {
                continue;
            }
            Dashboard dashboard = dashboardMapper.getById(dashboardId);
            if (dashboard == null) {
                continue;
            }
            List<MemDashboardWidget> mdw = memDashboardWidgetPublishMapper.getByDashboardId(dashboardId);
            if (CollectionUtils.isEmpty(mdw)) {
                continue;
            }
            // 去重
            mdw = mdw.stream().collect(
                    Collectors.collectingAndThen(
                            Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(MemDashboardWidget::getWidgetId))),
                            ArrayList::new
                    )
            );
            Set<Long> widgetIds = Sets.newHashSet();
            Set<Long> widgetContainIds = Sets.newHashSet();
            mdw.forEach(m -> {
                if(NORMAL_WIDGET_TYPE.equals(m.getWidgetType())) {
                    widgetIds.add(m.getWidgetId());
                } else {
                    widgetContainIds.add(m.getWidgetId());
                }
            });
            List<WidgetContainer> widgetContainers = widgetContainerMapper.getByIds(widgetContainIds);
            widgetContainers.forEach(w -> {
                List<WidgetTab> widgetTabs = JSONArray.parseArray(w.getConfig(), WidgetTab.class);
                widgetTabs.forEach(tab -> {
                    widgetIds.add(tab.getWidgetId());
                });
            });
            List<Widget> widgets = widgetMapper.getByIds(widgetIds);
            if (!CollectionUtils.isEmpty(widgets)) {
//                Map<Long, MemDashboardWidget> map = mdw.stream().collect(Collectors.toMap(o -> o.getWidgetId(), o -> o));
                widgets.forEach(t -> {
                    ViewExecuteParam executeParam = null;
                    if (!CollectionUtils.isEmpty(params)) {
//                        MemDashboardWidget memDashboardWidget = map.get(t.getId());
                        try {
                            executeParam = params.stream().filter(p -> null != p.getParam() && p.getId().equals(t.getId())).findFirst().get().getParam();
                        } catch (Exception e) {
                            log.error("widget参数获取异常, widgetId:" + t.getId());
                        }
                    }
                    // 兼容原地跳转下载
                    Widget realWidget = executeParam != null && executeParam.getWidget() != null
                            && !ObjectUtils.equals(executeParam.getWidget().getId(), t.getId()) ? executeParam.getWidget() : t;
                    widgetList.add(new WidgetContext(realWidget, dashboard, null, executeParam));
                });
            }
        }
        return widgetList;
    }

    protected List<WidgetContext> getWidgetContextListByFolderDashBoardId(Long id) {
        List<WidgetContext> widgetList = Lists.newArrayList();
        if (id == null || id.longValue() < 0L) {
            return widgetList;
        }
        List<Dashboard> dashboardList = dashboardMapper.getSubDashboardById(id);
        if (CollectionUtils.isEmpty(dashboardList)) {
            return widgetList;
        }
        List<Long> dashboardIds = dashboardList.stream().filter(x -> x != null).map(x -> x.getId()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(dashboardIds)) {
            return widgetList;
        }
        return getWidgetContextListByDashBoardId(dashboardIds, null);
    }

    protected String getDownloadFileName(DownloadType downloadType, Long id) {
        String fileName;
        switch (downloadType) {
            case Widget:
                Widget widget = widgetMapper.getById(id);
                fileName = widget.getName();
                break;
            case DashBoard:
            case DashBoardFolder:
                Dashboard dashboard = dashboardMapper.getById(id);
                fileName = dashboard.getName();
                break;
            default:
                throw new IllegalArgumentException("unsupported DownloadType=" + downloadType.name());
        }
        return fileName + UNDERLINE + DateUtils.toyyyyMMddHHmmss(System.currentTimeMillis());
    }

    protected List<WidgetContext> getWidgetContexts(DownloadType downloadType, Long id, User user, List<DownloadViewExecuteParam> params) {
        List<WidgetContext> widgetList = Lists.newArrayList();
        switch (downloadType) {
            case Widget:
                ViewExecuteParam executeParam = null;
                if (!CollectionUtils.isEmpty(params)) {
                    try {
                        executeParam = params.stream()
                                .filter(p -> null != p.getParam() && p.getId().equals(id)).findFirst()
                                .get().getParam();
                    } catch (Exception e) {
                        // ignore
                    }
                }
                // 兼容原地跳转
                Widget realWidget;
                if(executeParam != null && executeParam.getWidget() != null
                        && ObjectUtils.notEqual(executeParam.getWidget().getId(), id)) {
                    realWidget = executeParam.getWidget();
                } else {
                    realWidget = widgetMapper.getById(id);
                }
                widgetList.add(new WidgetContext(realWidget, null, null, executeParam));
                break;
            case DashBoard:
                List<WidgetContext> widgets = getWidgetContextListByDashBoardId(Lists.newArrayList(id), params);
                if (!CollectionUtils.isEmpty(widgets)) {
                    widgetList.addAll(widgets);
                }
                break;
            case DashBoardFolder:
                List<WidgetContext> widgets1 = getWidgetContextListByFolderDashBoardId(id);
                if (!CollectionUtils.isEmpty(widgets1)) {
                    widgetList.addAll(widgets1);
                }
                break;
            default:
                throw new IllegalArgumentException("unsupported DownloadType=" + downloadType.name());
        }
        if (CollectionUtils.isEmpty(widgetList)) {
            throw new IllegalArgumentException("has no widget to download");
        }
        for (WidgetContext context : widgetList) {
            ProjectDetail projectDetail = projectService.getProjectDetail(context.getWidget().getProjectId(), user, false);
            ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);
            //校验权限
            if (!projectPermission.getDownloadPermission()) {
                log.info("user {} have not permisson to download the widget {}", user.getUsername(), id);
                throw new UnAuthorizedException("you have not permission to download the widget");
            }
            context.setIsMaintainer(projectService.isMaintainer(projectDetail, user));
            //判断表是图标还是驱动
            boolean isGroupsEmpty  = CollectionUtils.isEmpty(context.getExecuteParam().getRowGroups())
                    ||CollectionUtils.isEmpty(context.getExecuteParam().getColGroups())
                    ||CollectionUtils.isEmpty(context.getExecuteParam().getAggregators());
            if(isGroupsEmpty){
                context.setShowMode(1);
            }else {
                context.setShowMode(1);
            }

        }
        return widgetList;
    }

}
