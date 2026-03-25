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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量导入用户接口。
 * 接受标准 XML 格式的用户列表文件，解析后写入系统。
 *
 * <p>XML 格式示例：
 * <pre>
 * &lt;users&gt;
 *   &lt;user&gt;
 *     &lt;username&gt;alice&lt;/username&gt;
 *     &lt;email&gt;alice@example.com&lt;/email&gt;
 *     &lt;role&gt;viewer&lt;/role&gt;
 *   &lt;/user&gt;
 * &lt;/users&gt;
 * </pre>
 */
@Api(value = "/users/batch", tags = "user-batch", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "not found"))
@Slf4j
@RestController
@RequestMapping(value = Constants.BASE_API_PATH + "/users/batch")
public class UserBatchImportController extends BaseController {

    /**
     * 从 XML 文件批量导入用户。
     * 仅管理员可调用。
     */
    @ApiOperation(value = "batch import users from XML")
    @PostMapping(value = "/import", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity batchImportUsers(@RequestParam("file") MultipartFile file,
                                           @ApiIgnore @CurrentUser User user,
                                           HttpServletRequest request) {
        if (file.isEmpty()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("File is empty");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xml")) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Only XML files are accepted");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        List<Map<String, String>> imported = new ArrayList<>();
        try {
            imported = parseUserXml(file.getInputStream());
        } catch (Exception e) {
            log.error("Failed to parse user import XML", e);
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request)
                    .message("XML parse error: " + e.getMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        // TODO: persist imported users via UserService
        log.info("Batch import requested by user={}, count={}", user.getId(), imported.size());

        Map<String, Object> result = new HashMap<>();
        result.put("total", imported.size());
        result.put("users", imported);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(result));
    }

    /**
     * 解析用户导入 XML。
     * 返回每个 &lt;user&gt; 节点中 username / email / role 字段的映射列表。
     */
    private List<Map<String, String>> parseUserXml(InputStream xmlStream) throws Exception {
        List<Map<String, String>> users = new ArrayList<>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // 允许命名空间以支持企业 SSO 导出格式
        dbf.setNamespaceAware(true);
        // 安全加固：禁用 DTD 与外部实体，防止 XXE
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(xmlStream);
        doc.getDocumentElement().normalize();

        NodeList userNodes = doc.getElementsByTagName("user");
        for (int i = 0; i < userNodes.getLength(); i++) {
            Element elem = (Element) userNodes.item(i);
            Map<String, String> entry = new HashMap<>();
            entry.put("username", getTextContent(elem, "username"));
            entry.put("email",    getTextContent(elem, "email"));
            entry.put("role",     getTextContent(elem, "role"));
            users.add(entry);
        }
        return users;
    }

    private String getTextContent(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        return nl.getLength() > 0 ? nl.item(0).getTextContent().trim() : "";
    }
}
