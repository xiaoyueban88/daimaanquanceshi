package edp.davinci.service.impl;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import com.iflytek.edu.zx.filecloud.service.FileCloudService;

import edp.core.exception.ServerException;
import edp.davinci.core.utils.FileCloudUtils;
import edp.davinci.dao.UploadIconMapper;
import edp.davinci.model.UploadIcon;
import edp.davinci.model.User;
import edp.davinci.service.UploadIconService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/12/4
 */
@Service("uploadIconService")
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class UploadIconServiceImpl implements UploadIconService {

    @Value("${oss.filecloud.appId}")
    private String appId;

    @Value("${oss.filecloud.category}")
    private String category;

    @Autowired
    private UploadIconMapper uploadIconMapper;

    @Autowired
    private FileCloudUtils fileCloudUtils;

    private final FileCloudService fileCloudService;


    @Override
    public void upload(String name, InputStream inputStream, User user) {
        // 判断图标名是否已经存在
        UploadIcon uploadIconByName = uploadIconMapper.getUploadIconByName(name);
        if (null != uploadIconByName) {
            throw new ServerException("Icon name " + name + " already exists, upload icon err");
        }
        // 上传图标并获取阿里云相对路径
        String path = fileCloudUtils.uploadFile("img", name + ".svg", inputStream);
        UploadIcon uploadIcon = UploadIcon.builder()
                .name(name)
                .path(path)
                .creator(user.getUsername())
                .createTime(new Date())
                .build();
        int insert = uploadIconMapper.insert(uploadIcon);
        if (insert <= 0) {
            throw new ServerException("insert icon info err, icon name:" + name);
        }
    }

    @Override
    public List<UploadIcon> getAllIcons() {
        List<UploadIcon> allUploadIcons = uploadIconMapper.getAllUploadIcons();
        // 相对路径转绝对路径
        allUploadIcons.forEach(uploadIcon -> {
            String fullUrl = fileCloudService.getFullUrl(appId, uploadIcon.getPath());
            uploadIcon.setPath(fullUrl);
        });
        return allUploadIcons;
    }

    @Override
    public void removeIcon(Integer id) {
        int delete = uploadIconMapper.delete(id);
        if (delete <= 0) {
            throw new ServerException("delete icon info err, icon id:" + id);
        }
    }
}
