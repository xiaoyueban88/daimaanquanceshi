package edp.davinci.service.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Maps;

import edp.core.consts.Consts;
import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.core.utils.CollectionUtils;
import edp.davinci.core.enums.LogNameEnum;
import edp.davinci.core.enums.UserPermissionEnum;
import edp.davinci.dao.MemDashboardWidgetMapper;
import edp.davinci.dao.MemDashboardWidgetPublishMapper;
import edp.davinci.dao.WidgetContainerMapper;
import edp.davinci.dao.WidgetContainerPublishMapper;
import edp.davinci.dao.WidgetFolderMapper;
import edp.davinci.dao.WidgetMapper;
import edp.davinci.dto.WidgetContainer.WidgetContainerCreate;
import edp.davinci.dto.WidgetContainer.WidgetContainerUpdate;
import edp.davinci.dto.projectDto.ProjectDetail;
import edp.davinci.dto.projectDto.ProjectPermission;
import edp.davinci.dto.widgetDto.WidgetTab;
import edp.davinci.model.User;
import edp.davinci.model.Widget;
import edp.davinci.model.WidgetContainer;
import edp.davinci.model.WidgetFolder;
import edp.davinci.service.ProjectService;
import edp.davinci.service.WidgetContainerService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/2/28
 */
@Service("widgetContainerService")
@Slf4j
public class WidgetContainerServiceImpl implements WidgetContainerService {
    private static final Logger optLogger = LoggerFactory.getLogger(LogNameEnum.BUSINESS_OPERATION.getName());

    @Autowired
    private ProjectService projectService;

    @Autowired
    private WidgetMapper widgetMapper;

    @Autowired
    private WidgetContainerMapper widgetContainerMapper;

    @Autowired
    private WidgetContainerPublishMapper widgetContainerPublishMapper;

    @Autowired
    private MemDashboardWidgetMapper memDashboardWidgetMapper;

    @Autowired
    private MemDashboardWidgetPublishMapper memDashboardWidgetPublishMapper;

    @Autowired
    private WidgetFolderMapper widgetFolderMapper;

    @Override
    public WidgetContainer createWidgetContainer(WidgetContainerCreate widgetContainerCreate, User user) {
        ProjectDetail projectDetail = projectService.getProjectDetail(widgetContainerCreate.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        if (projectPermission.getWidgetPermission() < UserPermissionEnum.WRITE.getPermission()) {
            log.info("user {} have not permisson to create widget", user.getUsername());
            throw new UnAuthorizedException("you have not permission to create widget");
        }

        if (isExistNameByFolder(widgetContainerCreate.getName(), null, widgetContainerCreate.getFolderId(), widgetContainerCreate.getProjectId())) {
            log.info("the widget {} name is already taken", widgetContainerCreate.getName());
            throw new ServerException("the widget name is already taken");
        }

        // 判断config是否有效
        boolean validate = validateConfig(widgetContainerCreate.getConfig());
        if (!validate) {
            throw new ServerException("the widgetcontainer config is not valid");
        }

        WidgetContainer widgetContainer = new WidgetContainer().createdBy(user.getId());
        BeanUtils.copyProperties(widgetContainerCreate, widgetContainer);
        int insert = widgetContainerMapper.insert(widgetContainer);
        if (insert > 0) {
            optLogger.info("widgetcontainer ({}) create by user(:{})", widgetContainer.toString());
            return widgetContainer;
        } else {
            throw new ServerException("create widgetcontainer fail");
        }

    }

    @Override
    public boolean updateWidgetContainer(WidgetContainerUpdate widgetContainerUpdate, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        WidgetContainer widgetContainer = widgetContainerMapper.getById(widgetContainerUpdate.getId());
        if (null == widgetContainer) {
            log.info("widgetContainer (:{}) is not found", widgetContainer.getId());
            throw new NotFoundException("widgetContainer is not found");
        }
        ProjectDetail projectDetail = projectService.getProjectDetail(widgetContainer.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        //校验权限
        if (projectPermission.getWidgetPermission() < UserPermissionEnum.WRITE.getPermission()) {
            log.info("user {} have not permisson to update widget", user.getUsername());
            throw new UnAuthorizedException("you have not permission to update widget");
        }

        if (isExistNameByFolder(widgetContainerUpdate.getName(), widgetContainerUpdate.getId(), widgetContainerUpdate.getFolderId(), projectDetail.getId())) {
            log.info("the widget {} name is already taken", widgetContainerUpdate.getName());
            throw new ServerException("the widget name is already taken");
        }

        // 判断config是否有效
        boolean validate = validateConfig(widgetContainerUpdate.getConfig());
        if (!validate) {
            throw new ServerException("the widgetcontainer config is not valid");
        }

        BeanUtils.copyProperties(widgetContainerUpdate, widgetContainer);
        widgetContainer.updatedBy(user.getId());
        int update = widgetContainerMapper.update(widgetContainer);

        if (update > 0) {
            optLogger.info("widgetconfainer ({}) is updated by user(:{})", widgetContainer.toString(), user.getId());
            return true;
        } else {
            throw new ServerException("update widgetcontainer fail");
        }
    }

    @Override
    public boolean batchUpdateWidgetContainerDispose(List<WidgetContainerUpdate> widgetContainerUpdates, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        // @TODO 此处暂时不限制权限，待权限改版后调整

        List<WidgetContainer> collect = widgetContainerUpdates.stream().map(w -> {
            WidgetContainer newWidget = new WidgetContainer();
            BeanUtils.copyProperties(w, newWidget);
            newWidget.updatedBy(user.getId());
            return newWidget;
        }).collect(Collectors.toList());

        if(!CollectionUtils.isEmpty(collect)) {
            int update = widgetContainerMapper.updateDisposeBatch(collect);
            if(update <= 0) {
                throw new ServerException("update widgetContainers fail");
            }
        }
        return true;
    }

    @Override
    public boolean deleteWidgetContainer(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        WidgetContainer widgetContainer = widgetContainerMapper.getById(id);
        if (null == widgetContainer) {
            log.info("widgetContainer (:{}) is not found", id);
            throw new NotFoundException("widgetContainer is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(widgetContainer.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        //校验权限
        if (projectPermission.getWidgetPermission() < UserPermissionEnum.DELETE.getPermission()) {
            log.info("user {} have not permisson to delete widget", user.getUsername());
            throw new UnAuthorizedException("you have not permission to delete widget");
        }

        // TODO 删除已发布出去的dashboard和widgetcontainer的引用关系
        memDashboardWidgetPublishMapper.deleteByWidget(id, Consts.CONTAINER_WIDGET_TYPE);

        // TODO 删除已发布出去的widgetcontainer
        widgetContainerPublishMapper.deleteById(id);

        // TODO 删除dashoard和widgetContainer的引用关系
        memDashboardWidgetMapper.deleteByWidget(id, Consts.CONTAINER_WIDGET_TYPE);

        // 删除widgetcontainer
        widgetContainerMapper.deleteById(id);
        return false;
    }

    @Override
    public List<WidgetContainer> getWidgetContainers(Long projectId, Long folderId, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        ProjectDetail projectDetail = null;
        try {
            projectDetail = projectService.getProjectDetail(projectId, user, false);
        } catch (NotFoundException e) {
            throw e;
        } catch (UnAuthorizedException e) {
            return null;
        }
        List<WidgetContainer> widgetContainers;
        if (folderId == null || folderId < 0) {
            widgetContainers = widgetContainerMapper.getByProject(projectId);
        } else {
            widgetContainers = widgetContainerMapper.getByFolderId(projectId, folderId);
        }


        if (null != widgetContainers) {
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
        widgetContainers.forEach(widgetContainer -> {
            String folderName;
            if (widgetContainer.getFolderId() == 0) {
                folderName = "根目录";
            } else {
                folderName = folderNameMap.get(widgetContainer.getFolderId());
            }
            widgetContainer.setFolderName(folderName);
        });
        return widgetContainers;
    }

    @Override
    public void changeFolder(Long id, Long folderId, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        WidgetContainer widgetContainer = widgetContainerMapper.getById(id);
        if (null == widgetContainer) {
            log.info("widgetContainer (:{}) is not found", id);
            throw new NotFoundException("widget is not found");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(widgetContainer.getProjectId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        //校验权限
        if (projectPermission.getWidgetPermission() < UserPermissionEnum.WRITE.getPermission()) {
            log.info("user {} have not permisson to update widget", user.getUsername());
            throw new UnAuthorizedException("you have not permission to update widget");
        }

        if (isExistNameByFolder(widgetContainer.getName(), widgetContainer.getId(), folderId, projectDetail.getId())) {
            log.info("the widget {} name is already taken", widgetContainer.getName());
            throw new ServerException("the widget name is already taken");
        }

        // 更新所属文件夹
        widgetContainerMapper.updateFolder(id, folderId);
    }

    @Override
    public boolean isExist(String name, Long id, Long projectId) {
        Long widgetId = widgetContainerMapper.getByNameWithProjectId(name, projectId);
        if (null != id && null != widgetId) {
            return !id.equals(widgetId);
        }
        return null != widgetId && widgetId.longValue() > 0L;
    }

    @Override
    public synchronized boolean isExistNameByFolder(String name, Long id, Long folderId, Long projectId) {
        Long widgetId = widgetContainerMapper.getByNameWithFolderId(name, folderId, projectId);
        if (null != id && null != widgetId) {
            return !id.equals(widgetId);
        }
        return null != widgetId && widgetId.longValue() > 0L;
    }

    private boolean validateConfig(String config) {
        List<WidgetTab> widgetTabs = JSONArray.parseArray(config, WidgetTab.class);
        Iterator<WidgetTab> iterator = widgetTabs.iterator();
        while (iterator.hasNext()) {
            WidgetTab widgetTab = iterator.next();
            Widget widget = widgetMapper.getById(widgetTab.getWidgetId());
            if (widget == null) {
                log.info("widget {} not exist", widget.getId());
                iterator.remove();
            }
        }
        if (CollectionUtils.isEmpty(widgetTabs)) {
            log.info("widgetcontainer config cannot be empty");
            return false;
        }
        return true;
    }
}
