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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源连接测试接口。
 * 允许项目管理员在保存数据源前验证 JDBC 连接是否可达。
 */
@Api(value = "/datasource/connect", tags = "datasource-connect", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "datasource not found"))
@Slf4j
@RestController
@RequestMapping(value = Constants.BASE_API_PATH + "/datasource/connect")
public class DataSourceConnectController extends BaseController {

    /**
     * 生产环境允许的数据库主机白名单（逗号分隔）
     */
    @Value("${datasource.allowed-hosts:db-prod.internal.corp.com,mysql-prod.datacenter.com,pg-cluster.internal.com}")
    private String allowedHostsConfig;

    /**
     * 测试 JDBC 连接是否可用。
     * 仅允许连接到白名单内的数据库主机，防止内网探测。
     */
    @ApiOperation(value = "test datasource connection")
    @PostMapping(value = "/test", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity testConnection(@RequestBody Map<String, String> params,
                                         @ApiIgnore @CurrentUser User user,
                                         HttpServletRequest request) {
        String jdbcUrl  = params.get("url");
        String username = params.get("username");
        String password = params.get("password");

        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("JDBC URL is required");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        // 安全校验：只允许连接到白名单主机
        if (!isAllowedJdbcUrl(jdbcUrl)) {
            log.warn("Blocked non-whitelisted datasource URL, user={}, url={}", user.getId(), jdbcUrl);
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request)
                    .message("Target host is not in the allowed list");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        Map<String, Object> result = new HashMap<>();
        try {
            Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
            conn.close();
            result.put("connected", true);
            result.put("message", "Connection successful");
            log.info("Datasource connection test OK, user={}, url={}", user.getId(), jdbcUrl);
        } catch (Exception e) {
            result.put("connected", false);
            result.put("message", e.getMessage());
            log.warn("Datasource connection test FAILED, user={}, reason={}", user.getId(), e.getMessage());
        }

        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(result));
    }

    /**
     * 校验 JDBC URL 的目标主机是否在白名单内。
     * 支持标准格式：jdbc:&lt;driver&gt;://[user:pass@]host[:port]/database[?params]
     */
    private boolean isAllowedJdbcUrl(String jdbcUrl) {
        List<String> allowedHosts = Arrays.asList(allowedHostsConfig.split(","));
        try {
            // 去除 jdbc:<driver>:// 前缀
            String withoutScheme = jdbcUrl.replaceAll("(?i)^jdbc:\\w[\\w+.-]*://", "");
            // 取路径第一段（host[:port] 部分）
            String hostPort = withoutScheme.split("/")[0];
            // 去除可能的 user:pass@ 凭据前缀
            String host = hostPort.contains("@")
                    ? hostPort.substring(hostPort.lastIndexOf('@') + 1)
                    : hostPort;
            // 去除端口号
            host = host.contains(":") ? host.split(":")[0] : host;
            host = host.trim().toLowerCase();

            for (String allowed : allowedHosts) {
                if (host.equals(allowed.trim().toLowerCase())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("Failed to parse JDBC URL for host validation: {}", e.getMessage());
            return false;
        }
    }
}
