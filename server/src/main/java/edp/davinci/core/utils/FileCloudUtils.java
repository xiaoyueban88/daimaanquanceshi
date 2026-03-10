package edp.davinci.core.utils;

import java.io.InputStream;

import javax.annotation.Resource;

import com.aliyun.oss.OSSClient;
import com.iflytek.edu.zx.bizservice.system.oss.model.STSToken;
import com.iflytek.edu.zx.bizservice.system.oss.service.OSSFileService;

import com.iflytek.edu.zx.filecloud.service.FileCloudService;
import edp.core.exception.ServerException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/11/10
 */
@Component
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class FileCloudUtils {

    private static Logger logger = LoggerFactory.getLogger(FileCloudUtils.class);

    private final OSSFileService ossFileService;

    @Value("${oss.filecloud.appId}")
    private String appId;

    @Value("${oss.filecloud.category}")
    private String category;

    @Value("${oss.filecloud.appSecret}")
    private String appSecret;

    private final FileCloudService fileCloudService;


    public String uploadFile(String subPath,
                             String fileName,
                             InputStream inputStream) throws ServerException {
        STSToken token = ossFileService.getUploadToken(appId, appSecret, subPath);
        if (null == token) {
            throw new ServerException("获取上传token异常,appSecret=" + appSecret);
        }
        logger.info("oss-appSecret---" + appSecret);
        //上传
        String endpoint = token.getEndPoint();
        String accessKeyId = token.getAccessKeyId();
        String accessKeySecret = token.getAccessKeySecret();
        String bucketName = token.getBucket();
        // 上传文件后的相对路径，如果文件需要下载则objectName需要持久化
        String objectName = token.getPath() + '/' + fileName;
        String securityToken = token.getSecurityToken();
        OSSClient ossClient = null;
        try {
            ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret,
                    securityToken);
            ossClient.putObject(bucketName, objectName, inputStream);
        } finally {
            if (null != ossClient) {
                // new OSSClient和ossClient.shutdown()必须要成对出现
                ossClient.shutdown();
            }
        }
        return objectName;
    }

    public String uploadFileReturnFullUrl(String subPath,
                             String fileName,
                             InputStream inputStream) throws ServerException {
        STSToken token = ossFileService.getUploadToken(appId, appSecret, subPath);
        if (null == token) {
            throw new ServerException("获取上传token异常,appSecret=" + appSecret);
        }
        logger.info("oss-appSecret---" + appSecret);
        //上传
        String endpoint = token.getEndPoint();
        String accessKeyId = token.getAccessKeyId();
        String accessKeySecret = token.getAccessKeySecret();
        String bucketName = token.getBucket();
        // 上传文件后的相对路径，如果文件需要下载则objectName需要持久化
        String objectName = token.getPath() + '/' + fileName;
        String securityToken = token.getSecurityToken();
        OSSClient ossClient = null;
        try {
            ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret,
                    securityToken);
            ossClient.putObject(bucketName, objectName, inputStream);
        } finally {
            if (null != ossClient) {
                // new OSSClient和ossClient.shutdown()必须要成对出现
                ossClient.shutdown();
            }
        }
        return fileCloudService.getFullUrl(appId, objectName);
    }
}
