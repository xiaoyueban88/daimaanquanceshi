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

import java.io.File;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edp.core.consts.Consts;
import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.core.model.Paginate;
import edp.core.model.PaginateWithQueryColumns;
import edp.core.model.QueryColumn;
import edp.core.utils.CollectionUtils;
import edp.core.utils.FileUtils;
import edp.core.utils.ServerUtils;
import edp.davinci.common.utils.StringUtil;
import edp.davinci.core.enums.FileTypeEnum;
import edp.davinci.core.enums.LogNameEnum;
import edp.davinci.core.enums.UserPermissionEnum;
import edp.davinci.core.utils.CsvUtils;
import edp.davinci.core.utils.ExcelUtils;
import edp.davinci.dao.MemDashboardWidgetMapper;
import edp.davinci.dao.MemDashboardWidgetPublishMapper;
import edp.davinci.dao.MemDisplaySlideWidgetMapper;
import edp.davinci.dao.ViewMapper;
import edp.davinci.dao.WidgetFolderMapper;
import edp.davinci.dao.WidgetMapper;
import edp.davinci.dao.WidgetPublishMapper;
import edp.davinci.dto.projectDto.ProjectDetail;
import edp.davinci.dto.projectDto.ProjectPermission;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.dto.viewDto.ViewWithProjectAndSource;
import edp.davinci.dto.viewDto.ViewWithSource;
import edp.davinci.dto.widgetDto.WidgetCreate;
import edp.davinci.dto.widgetDto.WidgetQueryDto;
import edp.davinci.dto.widgetDto.WidgetTab;
import edp.davinci.dto.widgetDto.WidgetUpdate;
import edp.davinci.model.Dashboard;
import edp.davinci.model.MemDashboardWidget;
import edp.davinci.model.User;
import edp.davinci.model.View;
import edp.davinci.model.Widget;
import edp.davinci.model.WidgetFolder;
import edp.davinci.service.DashboardService;
import edp.davinci.service.ProjectService;
import edp.davinci.service.ShareService;
import edp.davinci.service.ViewService;
import edp.davinci.service.WidgetService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static edp.core.consts.Consts.CONTAINER_WIDGET_TYPE;
import static edp.core.consts.Consts.EMPTY;
import static edp.core.consts.Consts.NORMAL_WIDGET_TYPE;
import static edp.davinci.common.utils.ScriptUtiils.getExecuptParamScriptEngine;
import static edp.davinci.common.utils.ScriptUtiils.getViewExecuteParam;


@Service("widgetService")
@Slf4j
public class WidgetServiceImpl implements WidgetService {
    private static final Logger optLogger = LoggerFactory.getLogger(LogNameEnum.BUSINESS_OPERATION.getName());

    private static ExecutorService executorService = new ThreadPoolExecutor(8, 8,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    @Autowired
    private WidgetMapper widgetMapper;

    @Autowired
    private WidgetPublishMapper widgetPublishMapper;

    @Autowired
    private ViewMapper viewMapper;

    @Autowired
    private MemDashboardWidgetMapper memDashboardWidgetMapper;

    @Autowired
    private MemDashboardWidgetPublishMapper memDashboardWidgetPublishMapper;

    @Autowired
    private MemDisplaySlideWidgetMapper memDisplaySlideWidgetMapper;

    @Autowired
    private ShareService shareService;

    @Autowired
    private ViewService viewService;

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private ServerUtils serverUtils;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private WidgetFolderMapper widgetFolderMapper;

    @Autowired
    private DashboardService dashboardService;

    @Override
    public synchronized boolean isExist(String name, Long id, Long projectId) {
        Long widgetId = widgetMapper.getByNameWithProjectId(name, projectId);
        if (null != id && null != widgetId) {
            return !id.equals(widgetId);
        }
        return null != widgetId && widgetId.longValue() > 0L;
    }

    /**
     * 获取widgets列表
     *
     * @param projectId
     * @param user
     * @return
     */
    @Override
    public List<Widget> getWidgets(Long projectId, Long folderId, User user) throws NotFoundException, UnAuthorizedException, ServerException {

        ProjectDetail projectDetail = null;
        try {
            projectDetail = projectService.getProjectDetail(projectId, user, false);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException e) {
            return null;
        }

        List<Widget> widgets;
        if (folderId == null || folderId < 0) {
            widgets = widgetMapper.getByProject(projectId);
        } else {
            widgets = widgetMapper.getByFolderId(projectId, folderId);
        }


        if (null != widgets) {
            ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);
            if (projectPermission.getVizPermission() == UserPermissionEnum.HIDDEN.getPermission() &&
                    projectPermission.getWidgetPermission() == UserPermissionEnum.HIDDEN.getPermission()) {
                return null;
            }
        }

        List<WidgetFolder> folders = widgetFolderMapper.getWidgetFoldersByProjectId(projectId);
        // folderId -> folderName
        Map<Long, String> folderNameMap = Maps.newHashMap();
        folders.forEach(folder -> {
            folderNameMap.put(folder.getId(), folder.getName());
        });
        // 添加根目录
        folderNameMap.put(0L, "根目录");
        widgets.forEach(widget -> {
            widget.setFolderName(folderNameMap.get(widget.getFolderId()));
        });

        return widgets;
    }


    /**
     * 获取单个widget信息
     *
     * @param id
     * @param user
     * @return
     */
    @Override
    public Widget getWidget(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException {

        Widget widget = widgetMapper.getById(id);

        if (null == widget) {
            log.info("widget {} not found", id);
            throw new NotFoundException("widget is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(widget.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);
        if (projectPermission.getWidgetPermission() < UserPermissionEnum.READ.getPermission()) {
            throw new UnAuthorizedException();
        }

        return widget;
    }

    /**
     * 创建widget
     *
     * @param widgetCreate
     * @param user
     * @return
     */
    @Override
    @Transactional
    public Widget createWidget(WidgetCreate widgetCreate, User user) throws NotFoundException, UnAuthorizedException, ServerException {

        ProjectDetail projectDetail = projectService.getProjectDetail(widgetCreate.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        if (projectPermission.getWidgetPermission() < UserPermissionEnum.WRITE.getPermission()) {
            log.info("user {} have not permisson to create widget", user.getUsername());
            throw new UnAuthorizedException("you have not permission to create widget");
        }

        if (isExistNameByFolder(widgetCreate.getName(), null, widgetCreate.getFolderId(), widgetCreate.getProjectId())) {
            log.info("the widget {} name is already taken", widgetCreate.getName());
            throw new ServerException("the widget name is already taken");
        }

        View view = viewMapper.getById(widgetCreate.getViewId());
        if (null == view) {
            log.info("view (:{}) is not found", widgetCreate.getViewId());
            throw new NotFoundException("view not found");
        }

        Widget widget = new Widget().createdBy(user.getId());
        BeanUtils.copyProperties(widgetCreate, widget);
        int insert = widgetMapper.insert(widget);
        if (insert > 0) {
            optLogger.info("widget ({}) create by user(:{})", widget.toString());
            return widget;
        } else {
            throw new ServerException("create widget fail");
        }
    }

    /**
     * 修改widget
     *
     * @param widgetUpdate
     * @param user
     * @return
     */
    @Override
    @Transactional
    public boolean updateWidget(WidgetUpdate widgetUpdate, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        Widget widget = widgetMapper.getById(widgetUpdate.getId());
        if (null == widget) {
            log.info("widget (:{}) is not found", widgetUpdate.getId());
            throw new NotFoundException("widget is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(widget.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        //校验权限
        if (projectPermission.getWidgetPermission() < UserPermissionEnum.WRITE.getPermission()) {
            log.info("user {} have not permisson to update widget", user.getUsername());
            throw new UnAuthorizedException("you have not permission to update widget");
        }

        if (isExistNameByFolder(widgetUpdate.getName(), widgetUpdate.getId(), widgetUpdate.getFolderId(), projectDetail.getId())) {
            log.info("the widget {} name is already taken", widgetUpdate.getName());
            throw new ServerException("the widget name is already taken");
        }

        View view = viewMapper.getById(widgetUpdate.getViewId());
        if (null == view) {
            log.info("view (:{}) not found", widgetUpdate.getViewId());
            throw new NotFoundException("view not found");
        }

        String originStr = widget.toString();

        BeanUtils.copyProperties(widgetUpdate, widget);
        widget.updatedBy(user.getId());
        int update = widgetMapper.update(widget);
        if (update > 0) {
            optLogger.info("widget ({}) is updated by user(:{}), origin: ({})", widget.toString(), user.getId(), originStr);
            return true;
        } else {
            throw new ServerException("update widget fail");
        }
    }

    @Override
    public boolean batchUpdateConfig(List<WidgetUpdate> widgetUpdates, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        // @TODO 此处暂不考虑权限问题,待权限重构后调整
        List<Widget> widgets = widgetUpdates.stream().map(w -> {
            Widget newWidget = new Widget();
            BeanUtils.copyProperties(w, newWidget);
            newWidget.updatedBy(user.getId());
            return newWidget;
        }).collect(Collectors.toList());

        // 批量更新
        if(!CollectionUtils.isEmpty(widgets)) {
            int update = widgetMapper.updateConfigBatch(widgets);
            if(update <= 0) {
                throw new ServerException("update widgets fail");
            }
        }
        return true;
    }

    /**
     * 删除widget
     *
     * @param id
     * @param user
     * @return
     */
    @Override
    @Transactional
    public boolean deleteWidget(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException {

        Widget widget = widgetMapper.getById(id);
        if (null == widget) {
            log.info("widget (:{}) is not found", id);
            throw new NotFoundException("widget is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(widget.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        //校验权限
        if (projectPermission.getWidgetPermission() < UserPermissionEnum.DELETE.getPermission()) {
            log.info("user {} have not permisson to delete widget", user.getUsername());
            throw new UnAuthorizedException("you have not permission to delete widget");
        }

        //删除发布出去的dashboard对widget的引用关系
        memDashboardWidgetPublishMapper.deleteByWidget(id, Consts.NORMAL_WIDGET_TYPE);

        //删除已发布出去的widget
        widgetPublishMapper.deleteById(id);

        //删除引用widget的dashboard
        memDashboardWidgetMapper.deleteByWidget(id, Consts.NORMAL_WIDGET_TYPE);

        //删除引用widget的displayslide
        memDisplaySlideWidgetMapper.deleteByWidget(id);

        widgetMapper.deleteById(id);
        optLogger.info("widget ( {} ) delete by user( :{} )", widget.toString(), user.getId());

        return true;
    }


    /**
     * 共享widget
     *
     * @param id
     * @param user
     * @param clientId
     * @return
     */
    @Override
    public String shareWidget(Long id, User user, String clientId) throws NotFoundException, UnAuthorizedException, ServerException {

        Widget widget = widgetPublishMapper.getById(id);
        if (null == widget) {
            log.info("widget (:{}) is not found", id);
            throw new NotFoundException("widget is not found");
        }

        ProjectPermission projectPermission = projectService.getProjectPermission(projectService.getProjectDetail(widget.getProjectId(), user, false), user);

        //校验权限
        if (!projectPermission.getSharePermission()) {
            log.info("user {} have not permisson to share the widget {}", user.getUsername(), id);
            throw new UnAuthorizedException("you have not permission to share the widget");
        }

        return shareService.generateShareToken(id, clientId, user.getId());
    }


    @Override
    public String generationFile(Long id, ViewExecuteParam executeParam, User user, String type) throws NotFoundException, ServerException, UnAuthorizedException {
        String filePath = null;
        Widget widget = widgetMapper.getById(id);

        if (null == widget) {
            log.info("widget (:{}) not found", id);
            throw new NotFoundException("widget is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(widget.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);
        //校验权限
        if (!projectPermission.getDownloadPermission()) {
            log.info("user {} have not permisson to download the widget {}", user.getUsername(), id);
            throw new UnAuthorizedException("you have not permission to download the widget");
        }

        executeParam.setPageNo(-1);
        executeParam.setPageSize(-1);
        executeParam.setLimit(-1);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        String rootPath = fileUtils.fileBasePath +
                File.separator +
                "download" +
                File.separator +
                sdf.format(new Date()) +
                File.separator +
                type +
                File.separator;

        try {
            if (type.equals(FileTypeEnum.CSV.getType())) {
                ViewWithSource viewWithSource = viewMapper.getViewWithSource(widget.getViewId());

                boolean maintainer = projectService.isMaintainer(projectDetail, user);

                PaginateWithQueryColumns paginate = viewService.getResultDataList(maintainer, viewWithSource, executeParam, user);
                List<QueryColumn> columns = paginate.getColumns();
                if (!CollectionUtils.isEmpty(columns)) {
                    File file = new File(rootPath);
                    if (!file.exists()) {
                        file.mkdirs();
                    }

                    String csvName = widget.getName() + "_" +
                            System.currentTimeMillis() +
                            UUID.randomUUID().toString().replace("-", EMPTY) +
                            FileTypeEnum.CSV.getFormat();

                    filePath = CsvUtils.formatCsvWithFirstAsHeader(rootPath, csvName, columns, paginate.getResultList());
                }
            } else if (type.equals(FileTypeEnum.XLSX.getType())) {

                String excelName = widget.getName() + "_" +
                        System.currentTimeMillis() +
                        UUID.randomUUID().toString().replace("-", EMPTY) +
                        FileTypeEnum.XLSX.getFormat();


                HashSet<Widget> widgets = new HashSet<>();
                widgets.add(widget);
                Map<Long, ViewExecuteParam> executeParamMap = new HashMap<>();
                executeParamMap.put(widget.getId(), executeParam);

                filePath = rootPath + excelName;
                writeExcel(widgets, projectDetail, executeParamMap, filePath, user, false);
            } else {
                throw new ServerException("unknow file type");
            }
        } catch (Exception e) {
            throw new ServerException("generation " + type + " error!");
        }

        return serverUtils.getHost() + fileUtils.formatFilePath(filePath);
    }


    /**
     * widget列表数据写入指定excle文件
     *
     * @param widgets
     * @param projectDetail
     * @param executeParamMap
     * @param filePath
     * @param user
     * @param containType
     * @return
     * @throws Exception
     */
    @Override
    public File writeExcel(Set<Widget> widgets,
                           ProjectDetail projectDetail, Map<Long, ViewExecuteParam> executeParamMap,
                           String filePath, User user, boolean containType) throws Exception {

        if (StringUtils.isEmpty(filePath)) {
            throw new ServerException("excel file path is EMPTY");
        }

        if (!filePath.trim().toLowerCase().endsWith(FileTypeEnum.XLSX.getFormat())) {
            throw new ServerException("unknow file format");
        }

        SXSSFWorkbook wb = new SXSSFWorkbook(1000);

        CountDownLatch countDownLatch = new CountDownLatch(widgets.size());

        Iterator<Widget> iterator = widgets.iterator();
        int i = 1;

        ScriptEngine engine = getExecuptParamScriptEngine();

        boolean maintainer = projectService.isMaintainer(projectDetail, user);

        while (iterator.hasNext()) {
            Widget widget = iterator.next();
            final String sheetName = widgets.size() == 1 ? "Sheet" : "Sheet" + (widgets.size() - (i - 1));
            executorService.execute(() -> {
                Sheet sheet = null;
                try {
                    ViewWithProjectAndSource viewWithProjectAndSource = viewMapper
                            .getViewWithProjectAndSourceById(widget.getViewId());

                    ViewExecuteParam executeParam = null;
                    if (null != executeParamMap && executeParamMap.containsKey(widget.getId())) {
                        executeParam = executeParamMap.get(widget.getId());
                    } else {
                        executeParam = getViewExecuteParam((engine), null, widget.getConfig(), null);
                    }

                    PaginateWithQueryColumns paginate = viewService.getResultDataList(maintainer,
                            viewWithProjectAndSource, executeParam, user);

                    sheet = wb.createSheet(sheetName);
                    ExcelUtils.writeSheet(sheet, paginate.getColumns(), paginate.getResultList(), wb, containType,
                            widget.getConfig(), executeParam.getParams());
                } catch (ServerException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    sheet = null;
                    countDownLatch.countDown();
                }
            });

            i++;
        }

        countDownLatch.await();
        //TODO performance problem need to fix 
        executorService.shutdown();

        File file = new File(filePath);
        File dir = new File(file.getParent());
        if (!dir.exists()) {
            dir.mkdirs();
        }

        FileOutputStream out = new FileOutputStream(filePath);
        try {
            wb.write(out);
            out.flush();
        } catch (Exception e) {
            // ignore
        } finally {
            FileUtils.closeCloseable(out);
        }
        return file;
    }

    @Override
    public void changeFolder(Long id, Long folderId, User user) {
        Widget widget = widgetMapper.getById(id);
        if (null == widget) {
            log.info("widget (:{}) is not found", id);
            throw new NotFoundException("widget is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(widget.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        //校验权限
        if (projectPermission.getWidgetPermission() < UserPermissionEnum.WRITE.getPermission()) {
            log.info("user {} have not permisson to update widget", user.getUsername());
            throw new UnAuthorizedException("you have not permission to update widget");
        }

        if (isExistNameByFolder(widget.getName(), widget.getId(), folderId, widget.getProjectId())) {
            log.info("the widget {} name is already taken", widget.getName());
            throw new ServerException("the widget name is already taken");
        }

        // 更新所属文件夹
        widgetMapper.updateFolder(id, folderId);
    }

    @Override
    public synchronized boolean isExistNameByFolder(String name, Long id, Long folderId, Long projectId) {
        Long widgetId = widgetMapper.getByNameWithFolderId(name, folderId, projectId);
        if (null != id && null != widgetId) {
            return !id.equals(widgetId);
        }
        return null != widgetId && widgetId.longValue() > 0L;
    }

    @Override
    public List<Widget> getWidgetList(Long projectId, Long folderId, User user) {
        List<Widget> widgetList = widgetMapper.getWidgetListByFolder(projectId, folderId);

        // dashboard中使用到的widget容器id
        Set<Long> usedWidgetContainerIds = Sets.newHashSet();
        // 被使用的widget id(包含dashboard中使用到的和widget容器中使用到的)
        Set<Long> usedWidgetIds = getWidgetIdsFromWidgetContainer(widgetList);

        // 找出dashboard中使用到的widget容器id和widget id
        List<Dashboard> dashboards = dashboardService.getDashboardByProjectId(projectId);
        List<Long> dashboardIds = Lists.newArrayList();
        dashboards.forEach(d -> {
            dashboardIds.add(d.getId());
        });
        List<MemDashboardWidget> mems = memDashboardWidgetMapper.getSimpleInfoByDashboardIds(dashboardIds);
        mems.forEach(m -> {
            if (NORMAL_WIDGET_TYPE.equals(m.getWidgetType())) {
                usedWidgetIds.add(m.getWidgetId());
            } else if (CONTAINER_WIDGET_TYPE.equals(m.getWidgetType())) {
                usedWidgetContainerIds.add(m.getWidgetId());
            }
        });

        List<WidgetFolder> folders = widgetFolderMapper.getWidgetFoldersByProjectId(projectId);
        // folderId -> folderName
        Map<Long, String> folderNameMap = Maps.newHashMap();
        // 添加根目录映射
        folderNameMap.put(0L, "根目录");
        folders.forEach(folder -> {
            folderNameMap.put(folder.getId(), folder.getName());
        });

        // 给widgets设置"被引用标记"和目录名称
        for (Widget widget : widgetList) {
            widget.setFolderName(folderNameMap.get(widget.getFolderId()));
            if (NORMAL_WIDGET_TYPE.equals(widget.getWidgetType())) {
                if (usedWidgetIds.contains(widget.getId())) {
                    widget.setIsUsed(true);
                }
            } else if (CONTAINER_WIDGET_TYPE.equals(widget.getWidgetType())) {
                if (usedWidgetContainerIds.contains(widget.getId())) {
                    widget.setIsUsed(true);
                }
            }
        }
        return widgetList;
    }

    @Override
    public List<Widget> getDashboardWidgets(Long dashboardId, User user) {
        Dashboard dashboard = dashboardService.getDashboardById(dashboardId);
        if(dashboard == null) {
            return Lists.newArrayList();
        }
        // 获取dashboard直接引用的widget及widgetContainers信息
        List<Widget> widgetsAndWidgetContainers = widgetMapper.getWidgetsAndWidgetContainers(dashboardId);
        if(CollectionUtils.isEmpty(widgetsAndWidgetContainers)) {
            return Lists.newArrayList();
        }
        // 找出已经获取到的widgetIds, 避免从widgetContainer中提取widgetId出现重复
        Set<Long> widgetIdSet = widgetsAndWidgetContainers.stream()
                .filter(w -> ObjectUtils.equals(w.getWidgetType(), NORMAL_WIDGET_TYPE))
                .map(Widget::getId).collect(Collectors.toSet());
        // 获取widget容器中配置的widget id
        Set<Long> widgetIds = getWidgetIdsFromWidgetContainer(widgetsAndWidgetContainers);
        widgetIds.removeAll(widgetIdSet);
        widgetsAndWidgetContainers.addAll(widgetMapper.getByIds(widgetIds));
        return widgetsAndWidgetContainers;
    }

    @Override
    public PageInfo<Widget> getPagerWidgets(Long projectId, WidgetQueryDto queryParam, User user) {
        // 校验项目权限
        ProjectDetail projectDetail = projectService.getProjectDetail(projectId, user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);
        if (projectPermission.getVizPermission() == UserPermissionEnum.HIDDEN.getPermission() &&
                projectPermission.getWidgetPermission() == UserPermissionEnum.HIDDEN.getPermission()) {
            return new PageInfo<>();
        }
        List<Long> folderIds = null;
        if(!StringUtils.isEmpty(queryParam.getFolderName())) {
            folderIds = widgetFolderMapper.getFolderIdsByFolderName(queryParam.getFolderName());
            // 根目录默认为0
            if("根目录".contains(queryParam.getFolderName())) {
                folderIds.add(0L);
            }
            if(CollectionUtils.isEmpty(folderIds)) {
                return new PageInfo<>(Lists.newArrayList());
            }
        }

        PageHelper.startPage(queryParam.getPageIndex(), queryParam.getPageSize());
        List<Widget> widgets = widgetMapper.batchQueryWidgetsAndContainersOrderByCreateTime(projectId,folderIds, queryParam.getWidgetName());
        PageInfo<Widget> result = new PageInfo<>(widgets);
        List<WidgetFolder> widgetFolders = widgetFolderMapper.getWidgetFoldersByProjectId(projectId);
        // folderId -> folderName
        Map<Long, String> folderMap = widgetFolders.stream()
                .collect(Collectors.toMap(WidgetFolder::getId, WidgetFolder::getName, (key1, key2) -> key1));
        folderMap.put(0L, "根目录");
        // 填充widgetName属性
        widgets.forEach(w -> w.setFolderName(folderMap.get(w.getFolderId())));
        return result;
    }

    private Set<Long> getWidgetIdsFromWidgetContainer(List<Widget> widgets) {
        Set<Long> result = Sets.newLinkedHashSet();
        if(CollectionUtils.isEmpty(widgets)) {
            return Sets.newHashSet();
        }
        widgets.forEach(w -> {
            if(ObjectUtils.equals(w.getWidgetType(), CONTAINER_WIDGET_TYPE)) {
                List<WidgetTab> widgetTabs = JSONArray.parseArray(w.getConfig(), WidgetTab.class);
                widgetTabs.forEach(tab -> {
                    result.add(tab.getWidgetId());
                });
            }
        });
        return result;
    }
}
