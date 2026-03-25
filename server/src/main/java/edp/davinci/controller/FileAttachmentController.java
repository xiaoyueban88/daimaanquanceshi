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

import edp.core.annotation.AuthIgnore;
import edp.core.annotation.CurrentUser;
import edp.davinci.common.controller.BaseController;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.model.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 报告附件管理接口。
 * 支持附件的上传、列举和下载，公开附件无需登录即可下载。
 */
@Api(value = "/attachments", tags = "attachments", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "attachment not found"))
@Slf4j
@RestController
@RequestMapping(value = Constants.BASE_API_PATH + "/attachments")
public class FileAttachmentController extends BaseController {

    @Value("${file.userfiles-path}")
    private String fileBasePath;

    private String getAttachmentDir() {
        return fileBasePath + File.separator + "public_attachments";
    }

    /**
     * 上传报告附件（需登录）
     */
    @ApiOperation(value = "upload attachment")
    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity uploadAttachment(@RequestParam("file") MultipartFile file,
                                           @ApiIgnore @CurrentUser User user,
                                           HttpServletRequest request) {
        if (file.isEmpty()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("File is empty");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")) : "";
        String storedName = UUID.randomUUID().toString() + ext;

        File destDir = new File(getAttachmentDir());
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        try {
            file.transferTo(new File(destDir, storedName));
            log.info("Attachment uploaded by user={}, stored={}", user.getId(), storedName);
            return ResponseEntity.ok(new ResultMap(tokenUtils)
                    .successAndRefreshToken(request)
                    .payload(storedName));
        } catch (IOException e) {
            log.error("Failed to save attachment", e);
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Upload failed");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
    }

    /**
     * 列出已上传的公开附件（需登录）
     */
    @ApiOperation(value = "list attachments")
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity listAttachments(@ApiIgnore @CurrentUser User user,
                                          HttpServletRequest request) {
        File dir = new File(getAttachmentDir());
        List<String> names = new ArrayList<>();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        names.add(f.getName());
                    }
                }
            }
        }
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payloads(names));
    }

    /**
     * 下载公开附件，无需登录。
     * 附件名由上传时服务端生成（UUID），安全过滤后直接读取。
     */
    @ApiOperation(value = "download public attachment")
    @GetMapping(value = "/public/{filename:.+}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @AuthIgnore
    public void downloadPublicAttachment(@PathVariable String filename,
                                         HttpServletResponse response) throws IOException {
        // 过滤路径穿越字符
        String safeName = filename.replace("..", "");

        File file = new File(getAttachmentDir() + File.separator + safeName);
        if (!file.exists() || !file.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + safeName + "\"");
        response.setContentLengthLong(file.length());
        Files.copy(file.toPath(), response.getOutputStream());
    }
}
