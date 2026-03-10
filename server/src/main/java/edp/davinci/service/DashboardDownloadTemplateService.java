package edp.davinci.service;

import java.io.InputStream;
import java.rmi.ServerException;

import edp.davinci.model.User;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/11/10
 */
public interface DashboardDownloadTemplateService {
    /**
     * 上传excel下载模板文件
     *
     * @param dashboardId
     * @param inputStream
     * @param originalName
     * @param user
     * @throws ServerException
     */
    void upload(Long dashboardId, InputStream inputStream, String originalName, User user) throws ServerException;

    /**
     * 获取dashboard下载模板文件流
     *
     * @param dashboardId
     * @return
     */
    InputStream getDownloadTemplate(Long dashboardId);


}
