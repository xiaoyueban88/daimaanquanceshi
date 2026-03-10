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

package edp.davinci.service;

import java.util.List;
import java.util.Set;

import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.davinci.core.service.CheckEntityService;
import edp.davinci.dto.dashboardDto.DashboardCreate;
import edp.davinci.dto.dashboardDto.DashboardDto;
import edp.davinci.dto.dashboardDto.DashboardWithMem;
import edp.davinci.dto.dashboardDto.MemDashboardWidgetCreate;
import edp.davinci.dto.dashboardDto.MemDashboardWidgetDto;
import edp.davinci.dto.roleDto.VizVisibility;
import edp.davinci.model.Dashboard;
import edp.davinci.model.MemDashboardWidget;
import edp.davinci.model.Role;
import edp.davinci.model.User;

public interface DashboardService extends CheckEntityService {

    List<Dashboard> getDashboards(Long portalId, User user, Boolean requirePublish) throws NotFoundException, UnAuthorizedException, ServerException;

    DashboardWithMem getDashboardMemWidgets(Long portalId, Long dashboardId, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    Dashboard createDashboard(DashboardCreate dashboardCreate, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    void updateDashboards(Long portalId, DashboardDto[] dashboards, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean deleteDashboard(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    List<MemDashboardWidget> createMemDashboardWidget(Long portalId, Long dashboardId, MemDashboardWidgetCreate[] memDashboardWidgetCreates, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean updateMemDashboardWidgets(Long portalId, User user, MemDashboardWidgetDto[] memDashboardWidgets) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean deleteMemDashboardWidget(Long relationId, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    String shareDashboard(Long dashboardId, String username, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    void deleteDashboardAndPortalByProject(Long projectId) throws RuntimeException;

    List<Long> getExcludeRoles(Long id);

    boolean postDashboardVisibility(Role role, VizVisibility vizVisibility, User user) throws NotFoundException, UnAuthorizedException, ServerException;


    Dashboard getDashboardById(Long id);

    /**
     * 根据指定tableName找出相关联的dashboard
     *
     * @return
     */
    Set<Long> getDashboardIdsByTableName(Set<Long> dashboardIds, String tableName);

    /**
     * 复制dashboard及关联的widget
     *
     * @param dashboardId
     * @param suffix
     * @param user
     */
    void copyDashboard(Long dashboardId, String suffix, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    /**
     * 获取project下的dashboard
     *
     * @param projectId
     * @return id, name
     */
    List<Dashboard> getDashboardByProjectId(Long projectId);
}
