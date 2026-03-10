package edp.davinci.service;

import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.davinci.model.User;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/1/18
 */
public interface DeployService {

    /**
     * widget 发布
     *
     * @param id
     * @param user
     * @return
     * @throws NotFoundException
     * @throws ServerException
     * @throws UnAuthorizedException
     */
    boolean deployWidget(Long id, User user) throws NotFoundException, ServerException, UnAuthorizedException;

    /**
     * 发布widgetcontainer
     *
     * @param id
     * @param user
     * @return
     * @throws NotFoundException
     * @throws ServerException
     * @throws UnAuthorizedException
     */
    boolean deployWidgetContainer(Long id, User user) throws NotFoundException, ServerException, UnAuthorizedException;

    /**
     * dashboard 发布
     *
     * @param dashboardId
     * @param user
     * @return
     * @throws NotFoundException
     * @throws ServerException
     * @throws UnAuthorizedException
     */
    boolean deployDashboard(Long dashboardId, User user) throws NotFoundException, ServerException, UnAuthorizedException;
}
