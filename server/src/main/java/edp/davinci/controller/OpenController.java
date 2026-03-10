package edp.davinci.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edp.core.utils.CollectionUtils;
import edp.core.utils.TokenUtils;
import edp.davinci.common.controller.BaseController;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.dao.DashboardMapper;
import edp.davinci.dao.MemDashboardWidgetPublishMapper;
import edp.davinci.dao.WidgetContainerPublishMapper;
import edp.davinci.dao.WidgetMapper;
import edp.davinci.dao.WidgetPublishMapper;
import edp.davinci.dto.projectDto.ProjectInfo;
import edp.davinci.dto.shareDto.PlatformShareInfo;
import edp.davinci.dto.shareDto.ShareWidget;
import edp.davinci.dto.widgetDto.WidgetPermission;
import edp.davinci.dto.widgetDto.WidgetTab;
import edp.davinci.dto.widgetDto.WidgetWithRelationDashboardId;
import edp.davinci.model.Dashboard;
import edp.davinci.model.DashboardPortal;
import edp.davinci.model.User;
import edp.davinci.model.Widget;
import edp.davinci.service.DashboardPortalService;
import edp.davinci.service.DashboardService;
import edp.davinci.service.PlatformShareService;
import edp.davinci.service.ProjectService;
import edp.davinci.service.UserService;
import edp.system.CasProperties;
import edp.system.util.SessionUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Api(value = "/openController", tags = "openapi", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "project not found"))
@Slf4j
@RestController
@RequestMapping(value = Constants.BASE_API_PATH + "/openapi", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)

public class OpenController extends BaseController {
    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private DashboardPortalService dashboardPortalService;

    @Autowired
    private DashboardService dashboardService;


    @Autowired
    private CasProperties casProperties;

    @Autowired
    private PlatformShareService platformShareService;

    @Autowired
    private WidgetPublishMapper widgetPublishMapper;

    @Autowired
    private MemDashboardWidgetPublishMapper memDashboardWidgetPublishMapper;

    @Autowired
    private DashboardMapper dashboardMapper;

    @Autowired
    private WidgetMapper widgetMapper;

    @Autowired
    private WidgetContainerPublishMapper widgetContainerPublishMapper;


    //    公共账号
    private static String PUBLIC_ACCOUNT = "admin";


    //   返回admin角色下的所有项目
//    @ApiOperation(value = "get admin projects")
//    @GetMapping("/getAdminProjects")
//    public ResponseEntity getRolesOfProject(HttpServletRequest request) {
//        User user = userService.getByUsername(PUBLIC_ACCOUNT);
//        List<ProjectInfo> projects = projectService.getProjects(user);
//        return ResponseEntity.ok(projects);
//    }

    //   返回角色下的所有项目
    @ApiOperation(value = "get admin projects")
    @GetMapping("/getUserProjects")
    public ResponseEntity getUserProjects(@RequestParam String userId,
                                          HttpServletRequest request) {
        User user = userService.getByUsername(userId);
        List<ProjectInfo> projects = projectService.getProjects(user);
        return ResponseEntity.ok(projects);
    }

    //    返回项目下所有dashboard
    @ApiOperation(value = "get dashboardPortals")
    @GetMapping("/getProjDashboards")
    public ResponseEntity getDashboards(@RequestParam Long projectId,
                                        @RequestParam String userId,
                                        HttpServletRequest request) {
        User user = userService.getByUsername(userId);
        List<DashboardPortal> dashboardPortals = dashboardPortalService.getDashboardPortals(projectId, user);
        List<Dashboard> dashboardList = new ArrayList<>();
        if (dashboardPortals != null && dashboardPortals.size() > 0) {
            for (DashboardPortal dashboardPortal : dashboardPortals) {
                List<Dashboard> dashboards = dashboardService.getDashboards(dashboardPortal.getId(), user, Boolean.TRUE);
                dashboardList.addAll(dashboards);
            }
        }
        return ResponseEntity.ok(dashboardList);
    }


    /**
     * 根据token获取dashboard授权分析数据
     *
     * @param token
     * @return
     */
    @ApiOperation(value = "get dashboardInfo")
    @GetMapping("/getDashboardInfo")
    public ResponseEntity getDashboardInfo(@RequestParam String token, @RequestParam String clientSecret, @RequestParam String extraInfo) {
        PlatformShareInfo shareInfo = platformShareService.getShareInfo(token);
        Dashboard dashboard = dashboardService.getDashboardById(shareInfo.getShareId());
        Pair<String, String> pair = platformShareService.getAuthToken(clientSecret, token, extraInfo);
        Map<String, Object> result = Maps.newHashMap();
        result.put("dashboardId", dashboard.getId());
        result.put("dashboardName", dashboard.getName());
        result.put("acessToken", pair.getLeft());
        return ResponseEntity.ok(result);
    }

    //  返回分享链接
    @ApiOperation(value = "share dashboard")
    @GetMapping("/shareToken")
    public ResponseEntity shareDashboard(@RequestParam Long dashboardId,
                                         @RequestParam String userId,
                                         HttpServletRequest request) {

        User user = userService.getByUsername(userId);
        String shareToken = dashboardService.shareDashboard(dashboardId, "", user);
        return ResponseEntity.ok(shareToken);
    }

    /**
     * 返回第三方授权分享链接
     *
     * @param dashboardId
     * @param userId      分享人id
     * @param clientId    第三发应用id
     * @return
     */
    @ApiOperation(value = "share dashboard")
    @GetMapping("/sharePlatformToken")
    public ResponseEntity shareDashboard(@RequestParam Long dashboardId,
                                         @RequestParam String userId,
                                         @RequestParam String clientId) {
        User user = userService.getByUsername(userId);
        String shareToken = platformShareService.shareDashboard(dashboardId, clientId, user);
        return ResponseEntity.ok(shareToken);
    }

    @CrossOrigin("*")
    @ApiOperation(value = "get userInfo")
    @GetMapping("/getUserInfo")
    public ResponseEntity getToken() {
        com.zhixue.auth.model.User currentUser = new SessionUtil().getCurrentUser();
        User user = userService.regist(currentUser);
        Map<String, Object> result = Maps.newHashMap();
        result.put("userInfo", user);
        String logoutUrl = casProperties.getDemoServerUrl() + casProperties.getDemoLogoutUrl();
        result.put("logoutUrl", logoutUrl);
        return ResponseEntity.ok(new ResultMap().success(tokenUtils.generateToken(currentUser)).payload(result));
    }

    @ApiOperation(value = "set cachetime")
    @PostMapping("/setCacheTime")
    public ResponseEntity setCacheTime(@RequestParam Long dashboardId,
                                       @RequestParam Long expired,
                                       @RequestParam Boolean cache) {
        Set<WidgetWithRelationDashboardId> widgets = widgetMapper.getByDashboard(dashboardId);
        widgets.forEach(w -> {
            String config = w.getConfig();
            JSONObject parse = JSONObject.parseObject(config);
            parse.put("cache", cache);
            parse.put("expired", expired);
            w.setConfig(parse.toJSONString());
        });
        widgetMapper.updateConfigBatch(Lists.newArrayList(widgets));
        return ResponseEntity.ok("update success");
    }

    @RequestMapping("/getSimpleDashboardInfo")
    public ResponseEntity getSimpleDashboardInfo(@RequestParam String dashboardIds) {
        String[] split = dashboardIds.split(",");
        if (split.length <= 0) {
            return null;
        }
        List<Map<String, Object>> dashboardInfos = Lists.newArrayList();
        for (String s : split) {
            long id = Long.parseLong(s);
            Dashboard dashboard = dashboardMapper.getById(id);
            if (dashboard == null) {
                continue;
            }
            Map<String, Object> map = Maps.newHashMap();
            map.put("dashboardId", dashboard.getId());
            map.put("dashboardName", dashboard.getName());
            dashboardInfos.add(map);
        }
        return ResponseEntity.ok(dashboardInfos);
    }


    @GetMapping("/getSimpleWidgetInfo")
    public ResponseEntity getSimpleWidget(@RequestParam String dashboardIds) {
        String[] split = dashboardIds.split(",");
        Set<Long> widgetIds = Sets.newHashSet();
        Arrays.stream(split).forEach(s -> {
            try {
                Long dashboardId = Long.parseLong(s);
                Set<Long> widgetIdSet = memDashboardWidgetPublishMapper.getWidgetIdByDashboardId(dashboardId);
                if (!CollectionUtils.isEmpty(widgetIdSet)) {
                    widgetIds.addAll(widgetIdSet);
                }
                Set<ShareWidget> shareContainerWidgets = widgetContainerPublishMapper.getShareWidgetsByDashboard(dashboardId);
                if (!CollectionUtils.isEmpty(shareContainerWidgets)) {
                    shareContainerWidgets.forEach(e -> {
                        List<WidgetTab> widgetTabs = JSONArray.parseArray(e.getConfig(), WidgetTab.class);
                        widgetTabs.forEach(tab -> {
                            widgetIds.add(tab.getWidgetId());
                        });
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Set<Widget> simpleWidgetInfo = widgetPublishMapper.getSimpleWidgetInfo(Lists.newArrayList(widgetIds));
        return ResponseEntity.ok(simpleWidgetInfo);
    }

    @PostMapping("/getWidgetNamesMap")
    public ResponseEntity getWidgetNamesMap(Set<Long> widgetIds) {
        if (CollectionUtils.isEmpty(widgetIds)) {
            return null;
        }

        Set<Widget> widgetNamesByIds = widgetPublishMapper.getWidgetNamesByIds(widgetIds);
        return ResponseEntity.ok(widgetNamesByIds);
    }

    @GetMapping("/getPaperToken")
    public String getPaperToken(Long paperId, String info) {
        long currentTime = System.currentTimeMillis();
        currentTime += 30 * 60 * 1000;
        Date deadline = new Date(currentTime);
        return tokenUtils.generatePaperToken(paperId, info, deadline);
    }

    @ApiOperation(value = "get platformInfo")
    @GetMapping("/getPlatformInfo")
    public ResponseEntity getPlatformInfo(HttpServletRequest request) {
        String token = request.getHeader(Constants.TOKEN_HEADER_STRING);
        if(StringUtils.isEmpty(token)) {
            token = request.getHeader(Constants.TOKEN_HEADER_SHARE_STRING);
        }
        JSONObject result = new JSONObject();
        if(StringUtils.isEmpty(token)) {
            return ResponseEntity.ok(result);
        }
        String username = tokenUtils.getUsername(token);
        String[] userNameSplit = username.split(Constants.SPLIT_CHAR_STRING);
        if (userNameSplit.length < 2) {
            return ResponseEntity.ok(result);
        }
        String extraInfo = username.split(Constants.SPLIT_CHAR_STRING)[1];
        try {
            result = JSONObject.parseObject(extraInfo);
        } catch (Exception e) {

        }
        return ResponseEntity.ok(result);
    }

}

