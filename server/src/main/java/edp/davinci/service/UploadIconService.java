package edp.davinci.service;

import java.io.InputStream;
import java.util.List;

import edp.davinci.model.UploadIcon;
import edp.davinci.model.User;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/12/4
 */
public interface UploadIconService {

    /**
     * 上传图标
     *
     * @param name
     * @param inputStream
     * @param user
     */
    void upload(String name, InputStream inputStream, User user);

    /**
     * 获取所有图标
     *
     * @return
     */
    List<UploadIcon> getAllIcons();

    /**
     * 删除自定义图标
     *
     * @param id
     */
    void removeIcon(Integer id);
}
