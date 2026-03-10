/*
 * <<
 *  Davinci
 *  ==
 *  Copyright (C) 2016 - 2019 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package edp.davinci.controller;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.alibaba.druid.util.StringUtils;
import com.google.common.collect.Maps;
import com.iflytek.edu.zx.filecloud.service.FileCloudService;

import edp.core.annotation.AuthIgnore;
import edp.core.annotation.AuthShare;
import edp.core.annotation.CurrentUser;
import edp.core.utils.FileUtils;
import edp.davinci.common.controller.BaseController;
import edp.davinci.common.utils.DownloadUtils;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.core.enums.DownloadType;
import edp.davinci.core.enums.FileTypeEnum;
import edp.davinci.core.utils.ExcelUtils;
import edp.davinci.dao.ShareDownloadRecordMapper;
import edp.davinci.dto.viewDto.DownloadParam;
import edp.davinci.dto.viewDto.DownloadViewExecuteParam;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.model.DownloadRecord;
import edp.davinci.model.ShareDownloadRecord;
import edp.davinci.model.User;
import edp.davinci.service.DownloadService;
import edp.davinci.service.ShareDownloadService;
import edp.davinci.service.BatchDownloadService;
import edp.system.util.SimpleDownloadConLimiter;
import edp.system.util.SessionUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author daemon
 * @Date 19/5/27 20:30
 * To change this template use File | Settings | File Templates.
 */
@Api(value = "/download", tags = "download", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "download not found"))
@Slf4j
@RestController
@RequestMapping(value = Constants.BASE_API_PATH + "/download")
public class DownloadController extends BaseController {

    @Autowired
    private DownloadService downloadService;

    @Autowired
    private BatchDownloadService batchDownloadService;

    @Autowired
    private ShareDownloadService shareDownloadService;

    @Autowired
    private ShareDownloadRecordMapper shareDownloadRecordMapper;

    @Resource
    private FileCloudService fileCloudService;

    @Value("${file.userfiles-path}")
    public String fileBasePath;

    @Value("${oss.filecloud.appId}")
    private String appId;


    @ApiOperation(value = "get download record page")
    @GetMapping(value = "/page", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity getDownloadRecordPage(@ApiIgnore @CurrentUser User user,
                                                HttpServletRequest request) {
        List<DownloadRecord> records = downloadService.queryDownloadRecordPage(user.getId());
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(records));
    }


    @ApiOperation(value = "get download record file")
    @GetMapping(value = "/record/file/{id}/{token:.*}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @AuthIgnore
    public ResponseEntity getDownloadRecordFile(@PathVariable Long id,
                                                @PathVariable String token,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        DownloadRecord record = downloadService.downloadById(id, token);
        InputStream is = null;
        try {
            com.zhixue.auth.model.User currentUser = new SessionUtil().getCurrentUser();
            String format = FileTypeEnum.XLSX.getFormat();
            boolean showWaterMark = true;
            // 进行批量下载或者文件格式为xlsm时,不设置水印
            if (record.getPath().contains(FileTypeEnum.XLSM.getFormat())) {
                format = FileTypeEnum.XLSM.getFormat();
                showWaterMark = false;
            } else if (record.getPath().contains(FileTypeEnum.ZIP.getFormat())) {
                format = FileTypeEnum.ZIP.getFormat();
                showWaterMark = false;
            }
            DownloadUtils.encodeFileName(request, response, record.getName() + format);
            is = fileCloudService.download(appId, record.getPath());
//            if (showWaterMark) {
//                SXSSFWorkbook wb = new SXSSFWorkbook(new XSSFWorkbook(is), 1000);
//                String path = fileBasePath + "/download/" + currentUser.getId() + ".png";
//                ExcelUtils.setExcelBackground(wb.getXSSFWorkbook(), currentUser.getId(), path, response);
//            } else {
//                Streams.copy(is, response.getOutputStream(), true);
//            }
            Streams.copy(is, response.getOutputStream(), true);
        } catch (Exception e) {
            log.error("getDownloadRecordFile error,id=" + id + ",e=", e);
        } finally {
            FileUtils.closeCloseable(is);
        }
        return null;
    }


    @ApiOperation(value = "get download record file")
    @PostMapping(value = "/submit/{type}/{id}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity submitDownloadTask(@PathVariable String type,
                                             @PathVariable Long id,
                                             @ApiIgnore @CurrentUser User user,
                                             @Valid @RequestBody(required = false) DownloadParam downloadParam,
                                             HttpServletRequest request) {
        if (! SimpleDownloadConLimiter.INSTANCE.obtain()) {
            return ResponseEntity.ok(new ResultMap(tokenUtils).failAndRefreshToken(request).payload(null).message("当前下载excel的用户请求频繁，请稍后再试"));
        }
        boolean rst = false;
        if (1 == downloadParam.getDownloadType()) {
            rst = downloadService.submit(DownloadType.getDownloadType(type), id, user, downloadParam.getParams());
        } else if (2 == downloadParam.getDownloadType()) {
            rst = batchDownloadService.submit(DownloadType.getDownloadType(type), id, user, downloadParam.getParams());
        }
        return ResponseEntity.ok(rst ? new ResultMap(tokenUtils).successAndRefreshToken(request).payload(null) :
                new ResultMap(tokenUtils).failAndRefreshToken(request).payload(null));
    }

    @ApiOperation(value = "submit share download")
    @PostMapping(value = "/share/submit/{type}/{uuid}/{dataToken:.*}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @AuthShare
    public ResponseEntity submitShareDownloadTask(@PathVariable(name = "type") String type,
                                                  @PathVariable(name = "uuid") String uuid,
                                                  @PathVariable(name = "dataToken") String dataToken,
                                                  @Valid @RequestBody(required = false) DownloadViewExecuteParam[] params,
                                                  @ApiIgnore @CurrentUser User user,
                                                  HttpServletRequest request) {
        if (! SimpleDownloadConLimiter.INSTANCE.obtain()) {
            return ResponseEntity.ok(new ResultMap(tokenUtils).failAndRefreshToken(request).payload(null).message("当前下载excel的用户请求频繁，请稍后再试"));
        }
        if (StringUtils.isEmpty(dataToken)) {
            ResultMap resultMap = new ResultMap().fail().message("Invalid share token");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        List<DownloadViewExecuteParam> downloadViewExecuteParams = Arrays.asList(params);
        // 对加密参数进行处理
        downloadViewExecuteParams.forEach(download -> {
            ViewExecuteParam.decryptParam(download.getParam());
        });
        Pair<Boolean, Long> pair = shareDownloadService.submit(DownloadType.getDownloadType(type), uuid, dataToken,
                user, downloadViewExecuteParams, false);
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
        Pair<Boolean, Long> pair = shareDownloadService.submitBatchTask(DownloadType.getDownloadType(type), uuid, dataToken,
                user, downloadParam.getParams(), false);
        Boolean rst = pair.getLeft();
        Map<String, Object> resultMap = Maps.newHashMap();
        resultMap.put("shareClientId", uuid);
        resultMap.put("id", pair.getRight());
        resultMap.put("token", dataToken);
        return ResponseEntity.ok(rst ? new ResultMap().success().payload(resultMap) : new ResultMap().fail());
    }


    @ApiOperation(value = "get share download record page")
    @GetMapping(value = "/share/page/{uuid}/{token:.*}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @AuthShare
    public ResponseEntity getShareDownloadRecordPage(@PathVariable(name = "uuid") String uuid,
                                                     @PathVariable(name = "token") String token,
                                                     @ApiIgnore @CurrentUser User user,
                                                     HttpServletRequest request) {
        if (StringUtils.isEmpty(token)) {
            ResultMap resultMap = new ResultMap().fail().message("Invalid share token");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        List<ShareDownloadRecord> records = shareDownloadService.queryDownloadRecordPage(uuid, token, user, 2);

        if (null == user) {
            return ResponseEntity.ok(new ResultMap(tokenUtils).success().payloads(records));
        } else {
            return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payloads(records));
        }
    }


    @ApiOperation(value = "get download record file")
    @GetMapping(value = "/share/record/file/{type}/{id}/{uuid}/{token:.*}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @AuthShare
    public void getShareDownloadRecordFile(@PathVariable(name = "type") String type,
                                                     @PathVariable(name = "id") String id,
                                                     @PathVariable(name = "uuid") String uuid,
                                                     @PathVariable(name = "token") String token,
                                                     @ApiIgnore @CurrentUser User user,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        ShareDownloadRecord record = shareDownloadService.downloadById(id, uuid, token, user, false,
                null, DownloadType.getDownloadType(type));
        InputStream is = null;
        try {
            // 默认为xlsx文件格式
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
    }
}
