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

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.core.model.Paginate;
import edp.core.model.PaginateWithQueryColumns;
import edp.davinci.core.service.CheckEntityService;
import edp.davinci.dto.viewDto.DistinctParam;
import edp.davinci.dto.viewDto.DistinctQueryParam;
import edp.davinci.dto.viewDto.ViewBaseInfo;
import edp.davinci.dto.viewDto.ViewCreate;
import edp.davinci.dto.viewDto.ViewDistinctParam;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.dto.viewDto.ViewExecuteSql;
import edp.davinci.dto.viewDto.ViewUpdate;
import edp.davinci.dto.viewDto.ViewWithSource;
import edp.davinci.dto.viewDto.ViewWithSourceBaseInfo;
import edp.davinci.model.User;
import edp.davinci.service.excel.SQLContext;
import org.apache.commons.lang3.tuple.Pair;

public interface ViewService extends CheckEntityService {

    List<ViewBaseInfo> getViews(Long projectId, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    ViewWithSourceBaseInfo createView(ViewCreate viewCreate, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean updateView(ViewUpdate viewUpdate, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean deleteView(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    PaginateWithQueryColumns executeSql(ViewExecuteSql executeSql, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    Paginate<Map<String, Object>> getData(Long id, ViewExecuteParam executeParam, User user) throws NotFoundException, UnAuthorizedException, ServerException, SQLException;

    PaginateWithQueryColumns getResultDataList(boolean isMaintainer, ViewWithSource viewWithSource, ViewExecuteParam executeParam, User user) throws ServerException, SQLException;

    List<Map<String, Object>> getDistinctValue(Long id, DistinctParam param, User user) throws NotFoundException, ServerException, UnAuthorizedException;

    List getDistinctValueData(boolean isMaintainer, ViewWithSource viewWithSource, DistinctParam param, User user) throws ServerException;

    ViewWithSourceBaseInfo getView(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    SQLContext getSQLContext(boolean isMaintainer, ViewWithSource viewWithSource, ViewExecuteParam executeParam, User user);

    List getDistinctValueList(List<ViewDistinctParam> params, User user);

    String getDistinctSql(boolean isMaintainer, ViewWithSource viewWithSource, DistinctParam param, User user);

    PaginateWithQueryColumns getCrossResultDataList(boolean isMaintainer, ViewWithSource viewWithSource, ViewExecuteParam executeParam, User user) throws ServerException, SQLException;

    /**
     * 获取透视表行列维度查询sqlcontext
     * @param isMaintainer
     * @param viewWithSource
     * @param executeParam
     * @param user
     * @return left:colSql 透视表列维度查询sql right:rowSqlContext 透视表行维度查询sql
     */
    Pair<String, String> getPivotGroupSqlContext(boolean isMaintainer, ViewWithSource viewWithSource,
                                                         ViewExecuteParam executeParam, User user);

    /**
     * 获取去重查询sql
     * @param isMaintainer
     * @param viewWithSource
     * @param param
     * @param user
     * @return
     */
    String getDistinctQuerySql(boolean isMaintainer, ViewWithSource viewWithSource, DistinctQueryParam param, User user);
}
