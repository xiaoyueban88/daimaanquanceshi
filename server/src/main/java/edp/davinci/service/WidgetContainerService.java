package edp.davinci.service;


import java.util.List;

import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.davinci.core.service.CheckEntityService;
import edp.davinci.dto.WidgetContainer.WidgetContainerCreate;
import edp.davinci.dto.WidgetContainer.WidgetContainerUpdate;
import edp.davinci.model.User;
import edp.davinci.model.WidgetContainer;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/2/28
 */
public interface WidgetContainerService extends CheckEntityService {
    WidgetContainer createWidgetContainer(WidgetContainerCreate widgetContainerCreate, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean updateWidgetContainer(WidgetContainerUpdate widgetContainerUpdate, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean batchUpdateWidgetContainerDispose(List<WidgetContainerUpdate> widgetContainerUpdates, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean deleteWidgetContainer(Long id, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    List<WidgetContainer> getWidgetContainers(Long projectId, Long folderId, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    void changeFolder(Long id, Long folderId, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    boolean isExistNameByFolder(String name, Long id, Long folderId, Long projectId);
}
