package edp.davinci.service;

import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.davinci.dto.WidgetFolder.WidgetFolderDto;
import edp.davinci.model.WidgetFolder;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/4/30
 */
public interface WidgetFolderService {
    /**
     * 新建widget文件夹
     *
     * @param widgetFolder
     * @return
     */
    WidgetFolder create(WidgetFolder widgetFolder) throws UnAuthorizedException, ServerException;

    /**
     * 修改文件夹信息
     *
     * @param widgetFolder
     */
    void update(WidgetFolder widgetFolder) throws UnAuthorizedException, ServerException;

    /**
     * 删除文件夹
     *
     * @param projectId
     * @param id
     * @throws UnAuthorizedException
     * @throws ServerException
     */
    void loopDeleteFolder(Long projectId, Long id) throws UnAuthorizedException, ServerException;

    /**
     * 获取项目下的所有文件夹
     *
     * @param projectId
     * @return
     */
    WidgetFolderDto getWidgetFolderDto(Long projectId) throws UnAuthorizedException, ServerException;
}
