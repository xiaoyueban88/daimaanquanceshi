package edp.davinci.service.impl;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.iflytek.edu.zx.filecloud.service.FileCloudService;

import edp.core.enums.HttpCodeEnum;
import edp.core.exception.ForbiddenException;
import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.core.model.Paginate;
import edp.core.utils.AESUtils;
import edp.core.utils.CollectionUtils;
import edp.core.utils.RSAEncrypt;
import edp.core.utils.TokenUtils;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.core.enums.DownloadTaskStatus;
import edp.davinci.core.enums.VizEnum;
import edp.davinci.core.model.TokenEntity;
import edp.davinci.core.utils.FileCloudUtils;
import edp.davinci.dao.DashboardPublishMapper;
import edp.davinci.dao.MemDashboardWidgetPublishMapper;
import edp.davinci.dao.PlatformShareAuthMapper;
import edp.davinci.dao.ShareDownloadRecordMapper;
import edp.davinci.dao.UserMapper;
import edp.davinci.dao.ViewMapper;
import edp.davinci.dao.WidgetContainerPublishMapper;
import edp.davinci.dao.WidgetPublishMapper;
import edp.davinci.dto.dashboardDto.DashboardWithPortal;
import edp.davinci.dto.projectDto.ProjectDetail;
import edp.davinci.dto.projectDto.ProjectPermission;
import edp.davinci.dto.shareDto.PlatformShareInfo;
import edp.davinci.dto.shareDto.ShareDashboard;
import edp.davinci.dto.shareDto.ShareWidget;
import edp.davinci.dto.viewDto.DistinctParam;
import edp.davinci.dto.viewDto.Order;
import edp.davinci.dto.viewDto.ViewDistinctParam;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.dto.viewDto.ViewWithProjectAndSource;
import edp.davinci.dto.widgetDto.WidgetPermission;
import edp.davinci.dto.widgetDto.WidgetTab;
import edp.davinci.model.Dashboard;
import edp.davinci.model.MemDashboardWidget;
import edp.davinci.model.PlatformShareAuth;
import edp.davinci.model.ShareDownloadRecord;
import edp.davinci.model.User;
import edp.davinci.service.PlatformShareService;
import edp.davinci.service.ProjectService;
import edp.davinci.service.ShareService;
import edp.davinci.service.ViewService;
import edp.factory.widgetpower.WidgetPowerFactory;
import edp.factory.widgetpower.WidgetPowerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import static edp.core.consts.Consts.EMPTY;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/2/4
 */
@Slf4j
@Service("platformShareServiceImpl")
@RequiredArgsConstructor(onConstructor_ = {@Lazy})
public class PlatformShareServiceImpl extends VizCommonService implements PlatformShareService {

    @Autowired
    private PlatformShareAuthMapper platformShareAuthMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private WidgetPublishMapper widgetPublishMapper;

    @Autowired
    private DashboardPublishMapper dashboardPublishMapper;

    @Autowired
    private MemDashboardWidgetPublishMapper memDashboardWidgetPublishMapper;

    @Autowired
    private ShareService shareService;

    @Autowired
    private ViewMapper viewMapper;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ViewService viewService;

    @Autowired
    private WidgetPowerFactory widgetPowerFactory;

    @Autowired
    private WidgetContainerPublishMapper widgetContainerPublishMapper;

    @Autowired
    private ShareDownloadRecordMapper shareDownloadRecordMapper;

    private final FileCloudService fileCloudService;

    @Value("${oss.filecloud.appId}")
    private String appId;

    @Override
    public Pair<String, String> getAuthToken(String clientSecret, String shareToken, String extraInfo) {
        String clientId = validatePlatformShareAuth(clientSecret, shareToken);
        // 生成acessToken
        String acessToken = tokenUtils.generatePlatformAcessToken(clientId, extraInfo);
        String refreshToken = tokenUtils.generatePlatformRefreshToken(clientId, extraInfo);
        return Pair.of(acessToken, refreshToken);
    }


    @Override
    public ShareDashboard getShareDashboard(String token, String acessToken) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException {
        String clientId = tokenUtils.getPassword(acessToken);
        PlatformShareAuth platformShareAuth = platformShareAuthMapper.selectByClientId(clientId);
        if (platformShareAuth == null) {
            throw new UnAuthorizedException("暂无权限");
        }

        WidgetPowerUtil widgetPowerUtil = widgetPowerFactory.getWidgetPowerUtil(clientId);
        WidgetPermission widgetPower = widgetPowerUtil.getWidgetPermission(acessToken);

        List<Long> hideWidgetContainers = Lists.newArrayList();

        PlatformShareInfo shareInfo = getShareInfo(token);
        Long dashboardId = shareInfo.getShareId();
        Dashboard dashboard = dashboardPublishMapper.getById(dashboardId);
        ShareDashboard shareDashboard = new ShareDashboard();
        BeanUtils.copyProperties(dashboard, shareDashboard);

        //判断Dashboard是否有下载权限
        if (Boolean.FALSE.equals(widgetPower.getHasDownloadPermission())){
            shareDashboard.setHasDownloadPermission(false);
        }else {
            shareDashboard.setHasDownloadPermission(true);
        }

        Set<Long> shareIdSet = Sets.newHashSet();
        Set<ShareWidget> shareWidgetSet = Sets.newHashSet();
        Set<ShareWidget> shareWidgets = widgetPublishMapper.getShareWidgetsByDashboard(dashboardId);
        if (!CollectionUtils.isEmpty(shareWidgets)) {
            shareWidgetSet.addAll(shareWidgets);
            Iterator<ShareWidget> iterator = shareWidgets.iterator();
            while (iterator.hasNext()) {
                ShareWidget shareWidget = iterator.next();
                String dateToken = generateShareToken(shareWidget.getId(), clientId, shareInfo.getShareUser().getId());
                shareWidget.setDataToken(dateToken);
                shareIdSet.add(shareWidget.getId());
            }
        }

        // 添加要分享的容器widget
        Set<ShareWidget> shareContainerWidgets = widgetContainerPublishMapper.getShareWidgetsByDashboard(dashboardId);
        if (!CollectionUtils.isEmpty(shareContainerWidgets)) {
            shareWidgetSet.addAll(shareContainerWidgets);
            // 获取容器内的widgets
            Set<Long> subWidgetIds = Sets.newHashSet();
            shareContainerWidgets.forEach(s -> {
                List<WidgetTab> widgetTabs = JSONArray.parseArray(s.getConfig(), WidgetTab.class);
                List<Long> tabWidgetIds = Lists.newArrayList();
                // 检验容器中包含的widget权限
                Iterator<WidgetTab> widgetTabIterator = widgetTabs.iterator();
                while (widgetTabIterator.hasNext()) {
                    WidgetTab tab = widgetTabIterator.next();
                    if (!shareIdSet.contains(tab.getWidgetId())) {
                        subWidgetIds.add(tab.getWidgetId());
                    }
                    tabWidgetIds.add(tab.getWidgetId());
                    if (!CollectionUtils.isEmpty(widgetPower.getShowWidgetIds())) {
                        if (!widgetPower.getShowWidgetIds().contains(tab.getWidgetId())) {
                            widgetTabIterator.remove();
                            continue;
                        }
                    } else if (!CollectionUtils.isEmpty(widgetPower.getHideWidgetIds())) {
                        if (widgetPower.getHideWidgetIds().contains(tab.getWidgetId())) {
                            widgetTabIterator.remove();
                            continue;
                        }
                    }
                }
                s.setConfig(JSONArray.toJSONString(widgetTabs));

                if (!CollectionUtils.isEmpty(widgetPower.getShowWidgetIds())) {
                    tabWidgetIds.retainAll(widgetPower.getShowWidgetIds());
                    if (CollectionUtils.isEmpty(tabWidgetIds)) {
                        hideWidgetContainers.add(s.getId());
                    }
                } else if (!CollectionUtils.isEmpty(widgetPower.getHideWidgetIds())) {
                    if (widgetPower.getHideWidgetIds().containsAll(tabWidgetIds)) {
                        hideWidgetContainers.add(s.getId());
                    }
                }
            });
            Set<ShareWidget> subShareWidget = widgetPublishMapper.getShareWidgetsByIds(subWidgetIds);
            if (!CollectionUtils.isEmpty(subShareWidget)) {
                subShareWidget.forEach(sub -> {
                    String dateToken = generateShareToken(sub.getId(), clientId, shareInfo.getShareUser().getId());
                    sub.setDataToken(dateToken);
                    shareWidgetSet.add(sub);
                });
            }

        }
        // 包含dashboard与容器widget的关系
        List<MemDashboardWidget> memDashboardWidgets = memDashboardWidgetPublishMapper.getByDashboardId(dashboardId);
        Iterator<MemDashboardWidget> iterator = memDashboardWidgets.iterator();
        while (iterator.hasNext()) {
            MemDashboardWidget next = iterator.next();
            if (Objects.equals(next.getWidgetType(), Constants.NORMAL_WIDGET_TYPE)) {
                if (!CollectionUtils.isEmpty(widgetPower.getShowWidgetIds())) {
                    if (!widgetPower.getShowWidgetIds().contains(next.getWidgetId())) {
                        iterator.remove();
                    }
                } else if (!CollectionUtils.isEmpty(widgetPower.getHideWidgetIds())) {
                    if (widgetPower.getHideWidgetIds().contains(next.getWidgetId())) {
                        iterator.remove();
                    }
                }
            } else if (Objects.equals(next.getWidgetType(), Constants.CONTAINER_WIDGET_TYPE)) {
                if (hideWidgetContainers.contains(next.getWidgetId())) {
                    iterator.remove();
                }
            }
        }
        shareDashboard.setRelations(memDashboardWidgets);
        shareDashboard.setWidgets(shareWidgetSet);
        shareDashboard.setWidgetSkipConfigUrl(platformShareAuth.getWidgetSkipConfigUrl());
        return shareDashboard;
    }

    @Override
    public ShareWidget getShareWidget(String token, String clientId) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException {
        PlatformShareInfo shareInfo = getShareInfo(token);

        ShareWidget shareWidget = widgetPublishMapper.getShareWidgetById(shareInfo.getShareId());

        if (null == shareWidget) {
            throw new NotFoundException("widget not found");
        }

        String dateToken = generateShareToken(shareWidget.getId(), clientId, shareInfo.getShareUser().getId());
        shareWidget.setDataToken(dateToken);
        return shareWidget;
    }



    @Override
    public PlatformShareInfo getShareInfo(String shareToken) {
        if (StringUtils.isEmpty(shareToken)) {
            throw new ServerException("Invalid share token");
        }

        //AES解密
        String decrypt = AESUtils.decrypt(shareToken, null);

        //获取分享信息
        String tokenUserName = tokenUtils.getUsername(decrypt);
        String tokenPassword = tokenUtils.getPassword(decrypt);

        if (tokenUserName == null || tokenPassword == null) {
            throw new ServerException("Invalid share token");
        }

        String[] tokenInfos = tokenUserName.split(Constants.SPLIT_CHAR_STRING);
        String[] tokenCrypts = tokenPassword.split(Constants.SPLIT_CHAR_STRING);

        // 验证shareToken 有效性
        if (tokenInfos.length != 3 || tokenCrypts.length != 2) {
            throw new ServerException("Invalid share token");
        }

        // 分享实体id
        Long shareId1 = Long.parseLong(tokenInfos[0]);
        Long shareId2 = Long.parseLong(tokenCrypts[0]);

        // 分享人id
        Long shareUserId = Long.parseLong(tokenInfos[1]);

        // 被分享的第三方clientId
        String shareClientId = tokenInfos[2];

        // 被分享的第三方id
        Long platformShareAuthId = Long.parseLong(tokenCrypts[1]);

        if (shareId1.longValue() < 1L || shareId2.longValue() < 1L || !shareId1.equals(shareId2)) {
            throw new ServerException("Invalid share token");
        }

        if (shareUserId.longValue() < 1L) {
            throw new ServerException("Invalid share token");
        }

        User shareUser = userMapper.getById(shareUserId);
        if (null == shareUser) {
            throw new ServerException("Invalid share token");
        }

        PlatformShareInfo shareInfo = new PlatformShareInfo();
        shareInfo.setShareId(shareId1);
        shareInfo.setShareUser(shareUser);
        shareInfo.setClientId(shareClientId);
        shareInfo.setPlatformShareAuthId(platformShareAuthId);
        return shareInfo;
    }

    @Override
    public Paginate<Map<String, Object>> getShareData(String token, ViewExecuteParam executeParam, String accessToken) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException, SQLException {
        if (null == executeParam || (CollectionUtils.isEmpty(executeParam.getGroups()) && CollectionUtils.isEmpty(executeParam.getAggregators()))) {
            return null;
        }
//        List<Order> orders = executeParam.getOrders();
//        if (!CollectionUtils.isEmpty(orders)) {
//            executeParam.setOrders(Lists.newArrayList(orders.get(orders.size() - 1)));
//        }

        ViewExecuteParam.decryptParam(executeParam);

        PlatformShareInfo shareInfo = getShareInfo(token);
        String clientId = tokenUtils.getPassword(accessToken);
        WidgetPowerUtil widgetPowerUtil = widgetPowerFactory.getWidgetPowerUtil(clientId);
        WidgetPermission widgetPermission = widgetPowerUtil.getWidgetPermission(accessToken);

        if (!CollectionUtils.isEmpty(widgetPermission.getShieldWidgetIds())) {
            if (widgetPermission.getShieldWidgetIds().contains(shareInfo.getShareId())) {
                Paginate pa = new Paginate();
                pa.setDownloadPermission(false);
                pa.setTip("暂无权限");
                return pa;
            }
        }
        ViewWithProjectAndSource viewWithProjectAndSource = viewMapper.getViewWithProjectAndSourceByWidgetId(shareInfo.getShareId());

        ProjectDetail projectDetail = projectService.getProjectDetail(viewWithProjectAndSource.getProjectId(), shareInfo.getShareUser(), false);
        boolean maintainer = projectService.isMaintainer(projectDetail, shareInfo.getShareUser());

        Paginate paginate = null;
        if (CollectionUtils.isEmpty(executeParam.getRowGroups()) && CollectionUtils.isEmpty(executeParam.getColGroups())) {
            paginate = viewService.getResultDataList(maintainer, viewWithProjectAndSource, executeParam, shareInfo.getShareUser());
        } else {
            paginate = viewService.getCrossResultDataList(maintainer, viewWithProjectAndSource, executeParam, shareInfo.getShareUser());
        }

        // 下载权限校验
        List<Long> downloadableIds = widgetPermission.getDownloadableIds();
        List<Long> undownloadableId = widgetPermission.getUndownloadableId();
        if (!CollectionUtils.isEmpty(downloadableIds)) {
            if (!downloadableIds.contains(shareInfo.getShareId())) {
                paginate.setDownloadPermission(false);
            }
        }
        if (!CollectionUtils.isEmpty(undownloadableId)) {
            if (undownloadableId.contains(shareInfo.getShareId())) {
                paginate.setDownloadPermission(false);
            }
        }
        return paginate;
    }

    @Override
    public ResultMap getDistinctValue(String token, Long viewId, DistinctParam param) {
        List<Map<String, Object>> list = null;
        try {

            PlatformShareInfo shareInfo = getShareInfo(token);

            ViewWithProjectAndSource viewWithProjectAndSource = viewMapper.getViewWithProjectAndSourceById(viewId);
            if (null == viewWithProjectAndSource) {
                log.info("view (:{}) not found", viewId);
                return new ResultMap().fail().message("view not found");
            }

            ProjectDetail projectDetail = projectService.getProjectDetail(viewWithProjectAndSource.getProjectId(), shareInfo.getShareUser(), false);

            if (!projectService.allowGetData(projectDetail, shareInfo.getShareUser())) {
                return new ResultMap().fail(HttpCodeEnum.UNAUTHORIZED.getCode()).message("ERROR Permission denied");
            }

            try {
                boolean maintainer = projectService.isMaintainer(projectDetail, shareInfo.getShareUser());

                // decry param
                DistinctParam.decryParams(param);

                list = viewService.getDistinctValueData(maintainer, viewWithProjectAndSource, param, shareInfo.getShareUser());
            } catch (ServerException e) {
                return new ResultMap().fail(HttpCodeEnum.UNAUTHORIZED.getCode()).message(e.getMessage());
            }
        } catch (NotFoundException e) {
            return new ResultMap().fail().message(e.getMessage());
        } catch (ServerException e) {
            return new ResultMap().fail().message(e.getMessage());
        } catch (UnAuthorizedException e) {
            return new ResultMap().fail(HttpCodeEnum.FORBIDDEN.getCode()).message(e.getMessage());
        }
        return new ResultMap().success().payloads(list);
    }

    /**
     * 生成分享token
     *
     * @param shareEntityId
     * @param clientId
     * @return
     * @throws ServerException
     */
    @Override
    public String generateShareToken(Long shareEntityId, String clientId, Long userId) throws ServerException {
        /**
         * username: share实体Id:-:分享人id[:-:被分享人用户名]
         * password: share实体Id[:-:被分享人Id]
         */
        TokenEntity shareToken = new TokenEntity();
        String tokenUserName = shareEntityId + Constants.SPLIT_CHAR_STRING + userId;
        String tokenPassword = shareEntityId + EMPTY;
        if (!StringUtils.isEmpty(clientId)) {
            PlatformShareAuth platformShareAuth = platformShareAuthMapper.selectByClientId(clientId);
            if (null == platformShareAuth) {
                throw new ServerException("clientId : \"" + clientId + "\" not found");
            }
            tokenUserName += Constants.SPLIT_CHAR_STRING + clientId;
            tokenPassword += (Constants.SPLIT_CHAR_STRING + platformShareAuth.getId());
        }
        shareToken.setUsername(tokenUserName);
        shareToken.setPassword(tokenPassword);

        //生成token 并 aes加密
        return AESUtils.encrypt(tokenUtils.generateContinuousToken(shareToken), null);
    }

    @Override
    public String shareDashboard(Long dashboardId, String clientId, User user) throws NotFoundException, UnAuthorizedException, ServerException {
        DashboardWithPortal dashboardWithPortalAndProject = dashboardPublishMapper.getDashboardWithPortalAndProject(dashboardId);

        if (null == dashboardWithPortalAndProject) {
            log.info("dashboard (:{}) not found", dashboardId);
            throw new NotFoundException("dashboard is not found, please check publish status");
        }

        ProjectDetail projectDetail = projectService.getProjectDetail(dashboardWithPortalAndProject.getProject().getId(), user, false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, user);

        List<Long> disablePortals = getDisableVizs(user.getId(), projectDetail.getId(), null, VizEnum.PORTAL);
        boolean isDisable = disablePortals.contains(dashboardWithPortalAndProject.getDashboardPortalId());

        List<Long> disableDashboards = getDisableVizs(user.getId(), dashboardWithPortalAndProject.getDashboardPortalId(), null, VizEnum.DASHBOARD);

        //校验权限
        if (!projectPermission.getSharePermission() ||
                (!projectPermission.isProjectMaintainer() && (isDisable || disableDashboards.contains(dashboardWithPortalAndProject.getId())))) {
            log.info("user {} have not permisson to share the dashboard {}", user.getUsername(), user.getId());
            throw new UnAuthorizedException("you have not permission to share the dashboard");
        }


        if (dashboardWithPortalAndProject.getType() == 0) {
            throw new ServerException("dashboard folder cannot be shared");
        }

        return generateShareToken(dashboardId, clientId, user.getId());
    }

    @Override
    public ResultMap getUnionDistinctValue(String token, List<ViewDistinctParam> params, HttpServletRequest request) {
        List<Map<String, Object>> list = null;
        try {

            PlatformShareInfo shareInfo = getShareInfo(token);
            try {
                // decry param
                params.forEach(p -> DistinctParam.decryParams(p));

                list = viewService.getDistinctValueList(params, shareInfo.getShareUser());
            } catch (ServerException e) {
                return new ResultMap().fail(HttpCodeEnum.UNAUTHORIZED.getCode()).message(e.getMessage());
            }
        } catch (NotFoundException e) {
            return new ResultMap().fail().message(e.getMessage());
        } catch (ServerException e) {
            return new ResultMap().fail().message(e.getMessage());
        } catch (UnAuthorizedException e) {
            return new ResultMap().fail(HttpCodeEnum.FORBIDDEN.getCode()).message(e.getMessage());
        }
        return new ResultMap().success().payloads(list);
    }

    @Override
    public ShareWidget getShareWidgetInfo(String token, Long widgetId) throws NotFoundException, ServerException {
        PlatformShareInfo shareInfo = getShareInfo(token);

        ShareWidget shareWidget = widgetPublishMapper.getShareWidgetById(widgetId);

        if (null == shareWidget) {
            throw new NotFoundException("widget not found");
        }

        String dateToken = generateShareToken(shareWidget.getId(), shareInfo.getClientId(), shareInfo.getShareUser().getId());
        shareWidget.setDataToken(dateToken);
        return shareWidget;
    }

    @Override
    public ShareDownloadRecord getDownloadRecordById(String clientSecret, String shareToken, Integer recordId) {
        validatePlatformShareAuth(clientSecret, shareToken);
        ShareDownloadRecord record = shareDownloadRecordMapper.getById(recordId);
        if(Objects.equals(DownloadTaskStatus.DOWNLOADED.getStatus(), record.getStatus())) {
            String fullUrl = fileCloudService.getFullUrl(appId, record.getPath());
            record.setPath(fullUrl);
        }
        return record;
    }

    public String validatePlatformShareAuth(String clientSecret, String shareToken) {
        PlatformShareInfo shareInfo = getShareInfo(shareToken);

        String clientId = shareInfo.getClientId();
        PlatformShareAuth platformShareAuth = platformShareAuthMapper.selectByClientId(clientId);
        if (null == platformShareAuth || !shareInfo.getPlatformShareAuthId().equals(platformShareAuth.getId())) {
            throw new ForbiddenException("The resource requires authentication, which was not supplied with the request");
        }

        // 验证第三方身份
        String secret = null;
        try {
            secret = RSAEncrypt.publicDecrypt(clientSecret, platformShareAuth.getPublicKey());
        } catch (Exception e) {
            throw new ForbiddenException("The resource requires authentication, which was not supplied with the request");
        }
        if (secret == null) {
            throw new ForbiddenException("The resource requires authentication which was not supplied with the request");
        }

        boolean checkpw = false;
        try {
            checkpw = BCrypt.checkpw(secret, platformShareAuth.getClientSecret());
        } catch (Exception e) {
        }
        if (!checkpw) {
            throw new ForbiddenException("The resource requires authentication, which was not supplied with the request");
        }
        return clientId;
    }
}
