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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.pagehelper.PageInfo;
import com.iflytek.edu.elp.common.dto.page.PageParam;

import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.core.model.Paginate;
import edp.davinci.core.service.CheckEntityService;
import edp.davinci.dto.projectDto.ProjectDetail;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.dto.widgetDto.WidgetCreate;
import edp.davinci.dto.widgetDto.WidgetQueryDto;
import edp.davinci.dto.widgetDto.WidgetUpdate;
import edp.davinci.model.User;
import edp.davinci.model.Widget;

public interface WidgetService extends CheckEntityService {
    List<Widget> getWidgets(Long projectId, Long folderId, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    Widget createWidget(WidgetCreate widgetCreate, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean updateWidget(WidgetUpdate widgetUpdate, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean batchUpdateConfig(List<WidgetUpdate> batchUpdate, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean deleteWidget(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    String shareWidget(Long id, User user, String clientId) throws NotFoundException, UnAuthorizedException, ServerException;

    Widget getWidget(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    String generationFile(Long id, ViewExecuteParam executeParam, User user, String type) throws NotFoundException, ServerException, UnAuthorizedException;

    File writeExcel(Set<Widget> widgets, ProjectDetail projectDetail, Map<Long, ViewExecuteParam> executeParamMap, String filePath, User user, boolean containType) throws Exception;

    void changeFolder(Long id, Long folderId, User user);

    boolean isExistNameByFolder(String name, Long id, Long folderId, Long projectId);

    List<Widget> getWidgetList(Long projectId, Long folderId, User user);

    /**
     * 获取dashboar关联的所有widget信息
     * @param dashboardId
     * @param user
     * @return
     */
    List<Widget> getDashboardWidgets(Long dashboardId, User user);

    /**
     * 分页获取项目下的widget信息
     * @param projectId 项目id
     * @param queryParam 查询参数
     * @param user 当前用户
     * @return
     */
    PageInfo<Widget> getPagerWidgets(Long projectId, WidgetQueryDto queryParam, User user);
}
