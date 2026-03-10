package edp.davinci.controller;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;

import edp.core.annotation.AuthIgnore;
import edp.core.annotation.AuthShare;
import edp.core.annotation.CurrentUser;
import edp.core.annotation.VpnCheck;
import edp.core.enums.HttpCodeEnum;
import edp.core.exception.ServerException;
import edp.core.model.Paginate;
import edp.davinci.common.controller.BaseController;
import edp.davinci.common.utils.EncryptUtil;
import edp.davinci.common.utils.StringUtil;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.dto.shareDto.ShareDashboard;
import edp.davinci.dto.shareDto.ShareWidget;
import edp.davinci.dto.viewDto.DistinctParam;
import edp.davinci.dto.viewDto.ViewDistinctParam;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.model.ShareDownloadRecord;
import edp.davinci.model.User;
import edp.davinci.service.PlatformShareService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
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
 * @Description 第三方授权分享
 * @date 2020/2/4
 */

@Api(value = "/platformAuthShare", tags = "platformAuthShare", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "platform not found"))
@Slf4j
@RestController
@RequestMapping(value = Constants.PLATFORM_API_PATH + "/share", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class PlatformShareController extends BaseController {

    @Autowired
    private PlatformShareService platformShareService;


    /**
     * 获取acess token 和 refresh token
     *
     * @param clientSecret
     * @param shareToken
     * @return
     */
    @ApiOperation(value = "get authToken")
    @PostMapping("/getAuthToken")
    @AuthIgnore
    @VpnCheck
    public ResponseEntity getAuthToken(@RequestParam String clientSecret,
                                       @RequestParam String shareToken,
                                       @RequestParam String extraInfo) {
        Pair<String, String> pair = platformShareService.getAuthToken(clientSecret, shareToken, extraInfo);
        Map<String, String> result = Maps.newHashMap();
        result.put("accessToken", pair.getLeft());
//        result.put("refreshToken", pair.getRight());
        return ResponseEntity.ok(new ResultMap().success().payload(result));
    }

    /**
     * 获取acess token 和 refresh token
     *
     * @param clientSecret
     * @param shareToken
     * @return
     */
    @ApiOperation(value = "get getDownloadRecord")
    @PostMapping("/getDownloadRecord")
    @AuthIgnore
    @VpnCheck
    public ResponseEntity getDownloadRecord(@RequestParam String clientSecret,
                                       @RequestParam String shareToken,
                                       @RequestParam Integer recordId) {
        ShareDownloadRecord downloadRecord = platformShareService.getDownloadRecordById(clientSecret, shareToken, recordId);
        return ResponseEntity.ok(new ResultMap().success().payload(downloadRecord));
    }

    @ApiOperation(value = "refresh acessAcessToken")
    @PostMapping("/refreshToken")
    @VpnCheck
    public ResponseEntity refreshAcessToken(HttpServletRequest request) {
        String refreshToken = request.getHeader(Constants.TOKEN_HEADER_SHARE_STRING);
        return ResponseEntity.ok(new ResultMap().success().payload(tokenUtils.refreshAcessTokenByRefreshToken(refreshToken)));
    }

    /**
     * 获取分享dashboard
     *
     * @param token
     * @param request
     * @return
     */
    @ApiOperation(value = "get share dashboard")
    @GetMapping("/dashboard/{token}")
    @VpnCheck
    public ResponseEntity getShareDashboard(@PathVariable String token,
                                            HttpServletRequest request) {
        if (StringUtils.isEmpty(token)) {
            ResultMap resultMap = new ResultMap().fail().message("Invalid share token");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        String acessToken = request.getHeader(Constants.TOKEN_HEADER_SHARE_STRING);
        ShareDashboard shareDashboard = platformShareService.getShareDashboard(token, acessToken);

        return ResponseEntity.ok(new ResultMap().successForShare(tokenUtils.refreshToken(acessToken)).payload(shareDashboard));
    }

    @ApiOperation(value = "get share dashboard")
    @GetMapping("/widget/{token}")
    @VpnCheck
    public ResponseEntity getShareWidget(@PathVariable String token,
                                         HttpServletRequest request) {

        if (StringUtils.isEmpty(token)) {
            ResultMap resultMap = new ResultMap().fail().message("Invalid share token");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        String acessToken = request.getHeader(Constants.TOKEN_HEADER_SHARE_STRING);

        String clientId = tokenUtils.getPassword(acessToken);

        ShareWidget shareWidget = platformShareService.getShareWidget(token, clientId);

        return ResponseEntity.ok(new ResultMap().successForShare(tokenUtils.refreshToken(acessToken)).payload(shareWidget));
    }

    @ApiOperation(value = "get share widget info")
    @AuthShare
    @GetMapping("/widgetInfo/{token}/{widgetId}")
    public  ResponseEntity getWidgetInfo(@PathVariable String token,
                                         @PathVariable Long widgetId) {
        if (StringUtils.isEmpty(token)) {
            ResultMap resultMap = new ResultMap().fail().message("Invalid share token");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        ShareWidget shareWidgetInfo = platformShareService.getShareWidgetInfo(token, widgetId);
        return ResponseEntity.ok(new ResultMap().success().payload(shareWidgetInfo));
    }

    /**
     * share页获取源数据
     *
     * @param token
     * @param executeParam
     * @return
     */
    @ApiOperation(value = "get share data")
    @PostMapping(value = "/data/{token}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @VpnCheck
    public ResponseEntity getShareData(@PathVariable String token,
                                       @RequestBody(required = false) ViewExecuteParam executeParam,
                                       HttpServletRequest request) {
        if (StringUtils.isEmpty(token)) {
            ResultMap resultMap = new ResultMap().fail().message("Invalid share token");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if(!executeParam.checkSign()) {
            ResultMap resultMap = new ResultMap().fail().message("Illegal executeParam");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        String acessToken = request.getHeader(Constants.TOKEN_HEADER_SHARE_STRING);
        Paginate<Map<String, Object>> shareData = null;
        try {
            shareData = platformShareService.getShareData(token, executeParam, acessToken);
        } catch (SQLException e) {
            throw new ServerException("query data is error");
        }
        return ResponseEntity.ok(new ResultMap().successForShare(tokenUtils.refreshToken(acessToken)).payload(shareData));
    }

    @ApiOperation(value = "get share data")
    @AuthShare
    @PostMapping(value = "/data/{token}/distinctvalue/{viewId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @VpnCheck
    public ResponseEntity getDistinctValue(@PathVariable("token") String token,
                                           @PathVariable("viewId") Long viewId,
                                           @Valid @RequestBody DistinctParam param,
                                           @ApiIgnore BindingResult bindingResult,
                                           HttpServletRequest request) {

        if (StringUtils.isEmpty(token)) {
            ResultMap resultMap = new ResultMap().fail().message("Invalid share token");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if (invalidId(viewId)) {
            ResultMap resultMap = new ResultMap().fail().message("Invalid view id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap().fail().message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if(!param.checkSign()) {
            ResultMap resultMap = new ResultMap().fail().message("Illegal param");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        try {
            ResultMap resultMap = platformShareService.getDistinctValue(token, viewId, param);
            String acessToken = request.getHeader(Constants.TOKEN_HEADER_SHARE_STRING);
            resultMap.put("accessToken", tokenUtils.refreshToken(acessToken));
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpCodeEnum.SERVER_ERROR.getCode()).body(HttpCodeEnum.SERVER_ERROR.getMessage());
        }
    }

    @ApiOperation(value = "get union share distinct")
    @AuthShare
    @PostMapping(value = "/data/{token}/unionDistinctvalue", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getUnionDistinctValue(@Valid @RequestBody List<ViewDistinctParam> params,
                                                @ApiIgnore BindingResult bindingResult,
                                                @PathVariable("token") String token,
                                                HttpServletRequest request) {
        if (StringUtils.isEmpty(token)) {
            ResultMap resultMap = new ResultMap().fail().message("Invalid share token");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap().fail().message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        for (ViewDistinctParam param : params) {
            if(!param.checkSign()) {
                ResultMap resultMap = new ResultMap().fail().message("Illegal params");
                return ResponseEntity.status(resultMap.getCode()).body(resultMap);
            }
        }

        try {
            ResultMap unionDistinctValue = platformShareService.getUnionDistinctValue(token, params, request);
            return ResponseEntity.status(unionDistinctValue.getCode()).body(unionDistinctValue);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpCodeEnum.SERVER_ERROR.getCode()).body(HttpCodeEnum.SERVER_ERROR.getMessage());
        }
    }
}
