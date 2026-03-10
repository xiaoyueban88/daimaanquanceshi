package edp.davinci.service.impl;

import java.io.InputStream;
import java.util.UUID;

import javax.annotation.Resource;

import com.alibaba.druid.util.StringUtils;
import com.iflytek.edu.zx.filecloud.service.FileCloudService;

import edp.core.exception.ServerException;
import edp.davinci.core.utils.FileCloudUtils;
import edp.davinci.dao.DashboardDownloadTemplateMapper;
import edp.davinci.model.DashboardDownloadTemplate;
import edp.davinci.model.User;
import edp.davinci.service.DashboardDownloadTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/11/10
 */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class DashboardDownloadTemplateServiceImpl implements DashboardDownloadTemplateService {

    @Autowired
    private DashboardDownloadTemplateMapper dashboardDownloadTemplateMapper;

    @Autowired
    private FileCloudUtils fileCloudUtils;

    private final FileCloudService fileCloudService;

    @Value("${oss.filecloud.appId}")
    private String appId;

    @Override
    @Transactional
    public void upload(Long dashboardId, InputStream inputStream, String originalName, User user) throws ServerException {
        // 校验文件后缀
        if (StringUtils.isEmpty(originalName)) {
            throw new ServerException("template filename is empty; dashboardId=" + dashboardId);
        }
        String[] split = originalName.split("\\.");
        String suffix = split[split.length - 1];
        if (!"xlsm".equals(suffix)) {
            throw new ServerException("you can only upload template file with .xlsm suffix; dashboardId=" + dashboardId);
        }

        // uuid生成文件名
        UUID uuid = UUID.randomUUID();
        String fileName = uuid + ".xlsm";
        String filePath = fileCloudUtils.uploadFile("file", fileName, inputStream);
        DashboardDownloadTemplate template = DashboardDownloadTemplate.builder()
                .dashboardId(dashboardId).path(filePath).createUser(user.getUsername()).build();
        // 逻辑删除dashboard之前的模板
        int delete = dashboardDownloadTemplateMapper.delete(dashboardId);
        if (delete < 0) {
            throw new ServerException("download template record delete error; dashboardId=" + dashboardId);
        }
        int insert = dashboardDownloadTemplateMapper.insert(template);
        if (insert < 0) {
            throw new ServerException("insert download template file error; dashboardId=" + dashboardId);
        }
    }

    @Override
    public InputStream getDownloadTemplate(Long dashboardId) {
        DashboardDownloadTemplate query = dashboardDownloadTemplateMapper.query(dashboardId);
        if (null == query || StringUtils.isEmpty(query.getPath())) {
            return null;
        }
        String path = query.getPath();
        return fileCloudService.download(appId, path);
    }
}
