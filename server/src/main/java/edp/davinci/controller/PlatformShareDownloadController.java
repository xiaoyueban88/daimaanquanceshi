package edp.davinci.controller;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.alibaba.druid.util.StringUtils;
import com.google.common.collect.Maps;
import com.iflytek.edu.zx.filecloud.service.FileCloudService;

import edp.core.annotation.CurrentUser;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.core.utils.CollectionUtils;
import edp.core.utils.FileUtils;
import edp.davinci.common.controller.BaseController;
import edp.davinci.common.utils.DownloadUtils;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.core.enums.DownloadType;
import edp.davinci.core.enums.FileTypeEnum;
import edp.davinci.dao.PlatformShareAuthMapper;
import edp.davinci.dto.viewDto.DownloadParam;
import edp.davinci.dto.viewDto.DownloadViewExecuteParam;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.dto.widgetDto.WidgetPermission;
import edp.davinci.model.PlatformShareAuth;
import edp.davinci.model.ShareDownloadRecord;
import edp.davinci.model.User;
import edp.davinci.service.DashboardDownloadTemplateService;
import edp.davinci.service.ShareDownloadService;
import edp.factory.widgetpower.WidgetPowerFactory;
import edp.factory.widgetpower.WidgetPowerUtil;
import edp.system.util.SimpleDownloadConLimiter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/3/25
 */
@Api(value = "/platformDownload", tags = "download", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "download not found"))
@Slf4j
@RestController
@RequestMapping(value = Constants.PLATFORM_API_PATH + "/download")
public class PlatformShareDownloadController extends BaseController {

    @Autowired
    private ShareDownloadService shareDownloadService;

    @Autowired
    private FileCloudService fileCloudService;

    @Autowired
    private PlatformShareAuthMapper platformShareAuthMapper;

    @Autowired
    private WidgetPowerFactory widgetPowerFactory;

    @Value("${oss.filecloud.appId}")
    private String appId;

    @ApiOperation(value = "submit share download")
    @PostMapping(value = "/share/submit/{type}/{uuid}/{dataToken:.*}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity submitDownloadTask(@PathVariable(name = "type") String type,
                                             @PathVariable(name = "uuid") String uuid,
                                             @PathVariable(name = "dataToken") String dataToken,
                                             @Valid @RequestBody(required = false) DownloadViewExecuteParam[] params,
                                             @ApiIgnore @CurrentUser User user,
                                             HttpServletRequest request) {
        String acessToken = request.getHeader(Constants.TOKEN_HEADER_SHARE_STRING);
        if (!SimpleDownloadConLimiter.INSTANCE.obtain()) {
            return ResponseEntity.ok(new ResultMap(tokenUtils).failAndRefreshToken(request).payload(null).message("当前下载excel的用户请求频繁，请稍后再试"));
        }
        if (StringUtils.isEmpty(dataToken)) {
            ResultMap resultMap = new ResultMap().fail().message("Invalid share token");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        for (DownloadViewExecuteParam param : params) {
            if(null != param.getParam() && !param.getParam().checkSign()) {
                ResultMap resultMap = new ResultMap().fail().message("Illegal params");
                return ResponseEntity.status(resultMap.getCode()).body(resultMap);
            }
        }

        List<DownloadViewExecuteParam> downloadViewExecuteParams = Arrays.asList(params);
        // 对加密参数进行处理
        downloadViewExecuteParams.forEach(download -> {
            ViewExecuteParam.decryptParam(download.getParam());
        });

        //根据acessToken来判断该用户是否有下载看板权限
        if (acessToken != null) {
            String clientId = tokenUtils.getPassword(acessToken);
            PlatformShareAuth platformShareAuth = platformShareAuthMapper.selectByClientId(clientId);
            if (platformShareAuth == null) {
                throw new UnAuthorizedException("暂无权限");
            }

            WidgetPowerUtil widgetPowerUtil = widgetPowerFactory.getWidgetPowerUtil(clientId);
            WidgetPermission widgetPower = widgetPowerUtil.getWidgetPermission(acessToken);
            //判断Dashboard是否有下载权限
            if (Boolean.FALSE.equals(widgetPower.getHasDownloadPermission())){
                return ResponseEntity.ok(new ResultMap().fail().message("暂无下载权限"));
            }
        }

        Pair<Boolean, Long> pair = shareDownloadService.submit(DownloadType.getDownloadType(type), uuid, dataToken, user, downloadViewExecuteParams, true);
        Boolean rst = pair.getLeft();
        Map<String, Object> resultMap = Maps.newHashMap();
        resultMap.put("shareClientId", uuid);
        resultMap.put("id", pair.getRight());
        resultMap.put("token", dataToken);
        return ResponseEntity.ok(rst ? new ResultMap().success().payload(resultMap) : new ResultMap().fail());
    }

    @ApiOperation(value = "submit share download")
    @PostMapping(value = "/sharev1/submit/{type}/{uuid}/{dataToken:.*}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity submitv1DownloadTask(@PathVariable(name = "type") String type,
                                             @PathVariable(name = "uuid") String uuid,
                                             @PathVariable(name = "dataToken") String dataToken,
                                             @Valid @RequestBody(required = false) DownloadParam downloadParam,
                                             @ApiIgnore @CurrentUser User user,
                                             HttpServletRequest request) {
        if (!SimpleDownloadConLimiter.INSTANCE.obtain()) {
            return ResponseEntity.ok(new ResultMap(tokenUtils).failAndRefreshToken(request).payload(null).message("当前下载excel的用户请求频繁，请稍后再试"));
        }
        if (StringUtils.isEmpty(dataToken)) {
            ResultMap resultMap = new ResultMap().fail().message("Invalid share token");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if(downloadParam != null && !CollectionUtils.isEmpty(downloadParam.getParams())) {
            List<DownloadViewExecuteParam> params = downloadParam.getParams();
            for (DownloadViewExecuteParam param : params) {
                if(null != param.getParam() && !param.getParam().checkSign()) {
                    ResultMap resultMap = new ResultMap().fail().message("Illegal downloadParam");
                    return ResponseEntity.status(resultMap.getCode()).body(resultMap);
                }
            }

        }

        List<DownloadViewExecuteParam> downloadViewExecuteParams = downloadParam.getParams();
        // 对加密参数进行处理
        downloadViewExecuteParams.forEach(download -> {
            ViewExecuteParam.decryptParam(download.getParam());
        });
        Pair<Boolean, Long> pair = shareDownloadService.submitBatchTask(DownloadType.getDownloadType(type), uuid, dataToken,
                user, downloadParam.getParams(), true);
        Boolean rst = pair.getLeft();
        Map<String, Object> resultMap = Maps.newHashMap();
        resultMap.put("shareClientId", uuid);
        resultMap.put("id", pair.getRight());
        resultMap.put("token", dataToken);
        return ResponseEntity.ok(rst ? new ResultMap().success().payload(resultMap) : new ResultMap().fail());
    }

    @ApiOperation(value = "get share download record page")
    @GetMapping(value = "/share/page/{uuid}/{dataToken:.*}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity getShareDownloadRecordPage(@PathVariable(name = "uuid") String uuid,
                                                     @PathVariable(name = "dataToken") String dataToken,
                                                     @ApiIgnore @CurrentUser User user,
                                                     HttpServletRequest request) {
        if (StringUtils.isEmpty(dataToken)) {
            ResultMap resultMap = new ResultMap().fail().message("Invalid share token");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        List<ShareDownloadRecord> records = shareDownloadService.queryDownloadRecordPage(uuid, dataToken, user, 2);

        if (null == user) {
            return ResponseEntity.ok(new ResultMap(tokenUtils).success().payloads(records));
        } else {
            return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payloads(records));
        }
    }


    @ApiOperation(value = "get download record file")
    @GetMapping(value = "/share/record/file/{type}/{id}/{uuid}/{token:.*}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity getShareDownloadRecordFile(@PathVariable(name = "type") String type,
                                                     @PathVariable(name = "id") String id,
                                                     @PathVariable(name = "uuid") String uuid,
                                                     @PathVariable(name = "token") String token,
                                                     @RequestParam(required = false) String waterMarkText,
                                                     @ApiIgnore @CurrentUser User user,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        if (StringUtils.isEmpty(token)) {
            ResultMap resultMap = new ResultMap().fail().message("Invalid share token");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        ShareDownloadRecord record = shareDownloadService.downloadById(id, uuid, token, user, true, waterMarkText, DownloadType.getDownloadType(type));
        if (record == null || StringUtils.isEmpty(record.getPath())) {
            throw new ServerException("未找到下载记录或下载文件路径为空,下载异常。id=" + id);
        }
        InputStream is = null;
        try {
            // 下载文件默认为xlsx
            FileTypeEnum fileTypeEnum = record.getPath().contains(FileTypeEnum.XLSM.getFormat())
                    ? FileTypeEnum.XLSM : FileTypeEnum.XLSX;
            DownloadUtils.encodeFileName(request, response, record.getName() + fileTypeEnum.getFormat());
            is = fileCloudService.download(appId, record.getPath());
            Streams.copy(is, response.getOutputStream(), true);
        } catch (Exception e) {
            log.error("getDownloadRecordFile error,id=" + id + ",e=", e);
        } finally {
            FileUtils.closeCloseable(is);
        }
        return null;
    }

    @ApiOperation(value = "get download record")
    @PostMapping(value = "/share/submitAndReturnRecord/{type}/{uuid}/{dataToken:.*}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity submitAndReturnRecord(@PathVariable(name = "type") String type,
                                              @PathVariable(name = "uuid") String uuid,
                                              @PathVariable(name = "dataToken") String dataToken,
                                              @Valid @RequestBody(required = false) DownloadViewExecuteParam[] params,
                                              @ApiIgnore @CurrentUser User user,
                                              HttpServletRequest request) {
        String acessToken = request.getHeader(Constants.TOKEN_HEADER_SHARE_STRING);
        if (StringUtils.isEmpty(dataToken)) {
            ResultMap resultMap = new ResultMap().fail().message("Invalid share token");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        for (DownloadViewExecuteParam param : params) {
            if(null != param.getParam() && !param.getParam().checkSign()) {
                ResultMap resultMap = new ResultMap().fail().message("Illegal params");
                return ResponseEntity.status(resultMap.getCode()).body(resultMap);
            }
        }

        List<DownloadViewExecuteParam> downloadViewExecuteParams = Arrays.asList(params);
        // 对加密参数进行处理
        downloadViewExecuteParams.forEach(download -> {
            ViewExecuteParam.decryptParam(download.getParam());
        });
        Pair<Boolean, Long> pair = shareDownloadService.submit(DownloadType.getDownloadType(type), uuid, dataToken, user, downloadViewExecuteParams, true);
        if(pair.getRight() == null) {
            ResultMap resultMap = new ResultMap().fail().message("任务异常");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        ShareDownloadRecord record = shareDownloadService.downloadById(pair.getRight().toString(), uuid, dataToken, user,
                true, null, DownloadType.getDownloadType(type));
        return ResponseEntity.ok(new ResultMap().success().payload(record));
    }
}
