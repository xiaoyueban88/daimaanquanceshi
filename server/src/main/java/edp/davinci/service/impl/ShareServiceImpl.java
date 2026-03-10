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

package edp.davinci.service.impl;

import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Sets;
import com.iflytek.edu.zx.log.util.LogUtil;

import edp.core.consts.Consts;
import edp.core.enums.HttpCodeEnum;
import edp.core.exception.ForbiddenException;
import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.core.model.Paginate;
import edp.core.model.PaginateWithQueryColumns;
import edp.core.model.QueryColumn;
import edp.core.utils.AESUtils;
import edp.core.utils.CollectionUtils;
import edp.core.utils.FileUtils;
import edp.core.utils.ServerUtils;
import edp.core.utils.TokenUtils;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.core.model.TokenEntity;
import edp.davinci.core.utils.CsvUtils;
import edp.davinci.dao.DashboardPublishMapper;
import edp.davinci.dao.DisplayMapper;
import edp.davinci.dao.MemDashboardWidgetPublishMapper;
import edp.davinci.dao.MemDisplaySlideWidgetMapper;
import edp.davinci.dao.PlatformShareAuthMapper;
import edp.davinci.dao.UserMapper;
import edp.davinci.dao.ViewMapper;
import edp.davinci.dao.WidgetContainerPublishMapper;
import edp.davinci.dao.WidgetMapper;
import edp.davinci.dao.WidgetPublishMapper;
import edp.davinci.dto.displayDto.MemDisplaySlideWidgetWithSlide;
import edp.davinci.dto.projectDto.ProjectDetail;
import edp.davinci.dto.projectDto.ProjectPermission;
import edp.davinci.dto.shareDto.ShareDashboard;
import edp.davinci.dto.shareDto.ShareDisplay;
import edp.davinci.dto.shareDto.ShareDisplaySlide;
import edp.davinci.dto.shareDto.ShareInfo;
import edp.davinci.dto.shareDto.ShareWidget;
import edp.davinci.dto.userDto.UserLogin;
import edp.davinci.dto.viewDto.DistinctParam;
import edp.davinci.dto.viewDto.ViewDistinctParam;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.dto.viewDto.ViewWithProjectAndSource;
import edp.davinci.dto.viewDto.ViewWithSource;
import edp.davinci.dto.widgetDto.WidgetTab;
import edp.davinci.model.Dashboard;
import edp.davinci.model.Display;
import edp.davinci.model.DisplaySlide;
import edp.davinci.model.MemDashboardWidget;
import edp.davinci.model.MemDisplaySlideWidget;
import edp.davinci.model.User;
import edp.davinci.service.ProjectService;
import edp.davinci.service.ShareService;
import edp.davinci.service.UserService;
import edp.davinci.service.ViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static edp.core.consts.Consts.EMPTY;


@Service
@Slf4j
public class ShareServiceImpl implements ShareService {

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WidgetMapper widgetMapper;

    @Autowired
    private WidgetPublishMapper widgetPublishMapper;

    @Autowired
    private DisplayMapper displayMapper;

    @Autowired
    private DashboardPublishMapper dashboardPublishMapper;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ViewMapper viewMapper;

    @Autowired
    private ViewService viewService;

    @Autowired
    private MemDisplaySlideWidgetMapper memDisplaySlideWidgetMapper;

    @Autowired
    private MemDashboardWidgetPublishMapper memDashboardWidgetPublishMapper;

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private ServerUtils serverUtils;

    @Autowired
    private UserService userService;

    @Autowired
    private PlatformShareAuthMapper platformShareAuthMapper;

    @Autowired
    private WidgetContainerPublishMapper widgetContainerPublishMapper;

    @Override
    public User shareLogin(String token, UserLogin userLogin) throws NotFoundException, ServerException, UnAuthorizedException {
        //AES解密
        String decrypt = AESUtils.decrypt(token, null);
        //获取分享信息
        String tokenUserName = tokenUtils.getUsername(decrypt);
        String tokenPassword = tokenUtils.getPassword(decrypt);

        String[] tokenInfos = tokenUserName.split(Constants.SPLIT_CHAR_STRING);
        String[] tokenCrypts = tokenPassword.split(Constants.SPLIT_CHAR_STRING);

        if (tokenInfos.length < 2) {
            throw new ServerException("Invalid share token");
        }

        User loginUser = userService.userLogin(userLogin);
        if (null == loginUser) {
            throw new NotFoundException("user is not found");
        }

        Long shareUserId = Long.parseLong(tokenInfos[1]);
        if (shareUserId.longValue() < 1L) {
            throw new ServerException("Invalid share token");
        }

        User shareUser = userMapper.getById(shareUserId);
        if (null == shareUser) {
            throw new ServerException("Invalid share token");
        }

        if (tokenInfos.length == 3) {
            if (tokenCrypts.length < 2) {
                throw new ServerException("Invalid share token");
            }
            try {
                String sharedUserName = tokenInfos[2];
                Long sharedUserId = Long.parseLong(tokenCrypts[1]);
                if (!(loginUser.getUsername().equals(sharedUserName) && loginUser.getId().equals(sharedUserId)) && !loginUser.getId().equals(shareUserId)) {
                    throw new ForbiddenException("The resource requires authentication, which was not supplied with the request");
                }
            } catch (NumberFormatException e) {
                throw new ForbiddenException("The resource requires authentication, which was not supplied with the request");
            }
        }

        //是否激活
        if (!loginUser.getActive()) {
            throw new ServerException("this user is not active");
        }

        return loginUser;
    }

    /**
     * 获取分享widget
     *
     * @param token
     * @param user
     * @return
     */
    @Override
    public ShareWidget getShareWidget(String token, User user) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException {

        ShareInfo shareInfo = getShareInfo(token, user);
        verifyShareUser(user, shareInfo);


        ShareWidget shareWidget = widgetPublishMapper.getShareWidgetById(shareInfo.getShareId());

        if (null == shareWidget) {
            throw new NotFoundException("widget not found");
        }

        String dateToken = generateShareToken(shareWidget.getId(), shareInfo.getSharedUserName(), shareInfo.getShareUser().getId());
        shareWidget.setDataToken(dateToken);
        return shareWidget;
    }

    @Override
    public ShareWidget getShareWidgetInfo(String dashboardToken, Long id, User user) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException {
        ShareInfo shareInfo = getShareInfo(dashboardToken, user);

        ShareWidget shareWidget = widgetPublishMapper.getShareWidgetById(id);

        if (null == shareWidget) {
            throw new NotFoundException("widget not found");
        }

        String dateToken = generateShareToken(shareWidget.getId(), shareInfo.getSharedUserName(), shareInfo.getShareUser().getId());
        shareWidget.setDataToken(dateToken);
        return shareWidget;
    }


    /**
     * 获取分享Display
     *
     * @param token
     * @param user
     * @return
     */
    @Override
    public ShareDisplay getShareDisplay(String token, User user) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException {
        ShareInfo shareInfo = getShareInfo(token, user);
        verifyShareUser(user, shareInfo);

        Long displayId = shareInfo.getShareId();
        Display display = displayMapper.getById(displayId);
        if (null == display) {
            throw new ServerException("display is not found");
        }

        ShareDisplay shareDisplay = new ShareDisplay();

        BeanUtils.copyProperties(display, shareDisplay);

        List<MemDisplaySlideWidgetWithSlide> memWithSlides = memDisplaySlideWidgetMapper.getMemWithSlideByDisplayId(displayId);

        if (!CollectionUtils.isEmpty(memWithSlides)) {
            Set<DisplaySlide> displaySlideSet = new HashSet<>();
            Set<MemDisplaySlideWidget> memDisplaySlideWidgetSet = new HashSet<>();
            for (MemDisplaySlideWidgetWithSlide memWithSlide : memWithSlides) {
                displaySlideSet.add(memWithSlide.getDisplaySlide());
                MemDisplaySlideWidget memDisplaySlideWidget = new MemDisplaySlideWidget();
                BeanUtils.copyProperties(memWithSlide, memDisplaySlideWidget);
                memDisplaySlideWidgetSet.add(memDisplaySlideWidget);
            }

            if (!CollectionUtils.isEmpty(displaySlideSet)) {
                Set<ShareDisplaySlide> shareDisplaySlideSet = new HashSet<>();
                Iterator<DisplaySlide> slideIterator = displaySlideSet.iterator();
                while (slideIterator.hasNext()) {
                    DisplaySlide displaySlide = slideIterator.next();
                    ShareDisplaySlide shareDisplaySlide = new ShareDisplaySlide();
                    BeanUtils.copyProperties(displaySlide, shareDisplaySlide);

                    Iterator<MemDisplaySlideWidget> memIterator = memDisplaySlideWidgetSet.iterator();
                    Set<MemDisplaySlideWidget> relations = new HashSet<>();
                    while (memIterator.hasNext()) {
                        MemDisplaySlideWidget memDisplaySlideWidget = memIterator.next();
                        if (memDisplaySlideWidget.getDisplaySlideId().equals(displaySlide.getId())) {
                            relations.add(memDisplaySlideWidget);
                        }
                    }
                    shareDisplaySlide.setRelations(relations);
                    shareDisplaySlideSet.add(shareDisplaySlide);
                }
                shareDisplay.setSlides(shareDisplaySlideSet);
            }
        }

        Set<ShareWidget> shareWidgets = widgetMapper.getShareWidgetsByDisplayId(displayId);
        if (!CollectionUtils.isEmpty(shareWidgets)) {
            Iterator<ShareWidget> widgetIterator = shareWidgets.iterator();
            while (widgetIterator.hasNext()) {
                ShareWidget shareWidget = widgetIterator.next();
                String dateToken = generateShareToken(shareWidget.getId(), shareInfo.getSharedUserName(), shareInfo.getShareUser().getId());
                shareWidget.setDataToken(dateToken);
            }
            shareDisplay.setWidgets(shareWidgets);
        }

        return shareDisplay;
    }

    /**
     * 获取分享dashboard
     *
     * @param token
     * @param user
     * @return
     */
    @Override
    public ShareDashboard getShareDashboard(String token, User user) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException {
        ShareInfo shareInfo = getShareInfo(token, user);

        verifyShareUser(user, shareInfo);

        Long dashboardId = shareInfo.getShareId();
        Dashboard dashboard = dashboardPublishMapper.getById(dashboardId);

        if (null == dashboard) {
            throw new NotFoundException("dashboard is not found, please check publish status");
        }

        ShareDashboard shareDashboard = new ShareDashboard();
        BeanUtils.copyProperties(dashboard, shareDashboard);

        // 包含dashboard与容器widget的关系
        List<MemDashboardWidget> memDashboardWidgets = memDashboardWidgetPublishMapper.getByDashboardId(dashboardId);
        shareDashboard.setRelations(memDashboardWidgets);
        Set<Long> shareIdSet = Sets.newHashSet();
        Set<ShareWidget> shareWidgetSet = Sets.newHashSet();
        Set<ShareWidget> shareWidgets = widgetPublishMapper.getShareWidgetsByDashboard(dashboardId);
        if (!CollectionUtils.isEmpty(shareWidgets)) {
            shareWidgetSet.addAll(shareWidgets);
            Iterator<ShareWidget> iterator = shareWidgets.iterator();
            while (iterator.hasNext()) {
                ShareWidget shareWidget = iterator.next();
                String dateToken = generateShareToken(shareWidget.getId(), shareInfo.getSharedUserName(), shareInfo.getShareUser().getId());
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
                widgetTabs.forEach(tab -> {
                    if (!shareIdSet.contains(tab.getWidgetId())) {
                        subWidgetIds.add(tab.getWidgetId());
                    }
                });
            });
            Set<ShareWidget> subShareWidget = widgetPublishMapper.getShareWidgetsByIds(subWidgetIds);
            if (!CollectionUtils.isEmpty(subShareWidget)) {
                subShareWidget.forEach(sub -> {
                    String dateToken = generateShareToken(sub.getId(), shareInfo.getSharedUserName(), shareInfo.getShareUser().getId());
                    sub.setDataToken(dateToken);
                    shareWidgetSet.add(sub);
                });
            }

        }

        shareDashboard.setWidgets(shareWidgetSet);
        return shareDashboard;
    }

    private void verifyShareUser(User user, ShareInfo shareInfo) {
        if (null == shareInfo || shareInfo.getShareId().longValue() < 1L) {
            throw new ServerException("Invalid share token");
        }

        if (!StringUtils.isEmpty(shareInfo.getSharedUserName())) {
            User tokenUser = userMapper.selectByUsername(shareInfo.getSharedUserName());
            if (tokenUser == null || !tokenUser.getId().equals(user.getId())) {
                throw new UnAuthorizedException("ERROR Permission denied");
            }
        }
    }

    /**
     * 获取分享数据
     *
     * @param token
     * @param executeParam
     * @param user
     * @return
     */
    @Override
    public Paginate<Map<String, Object>> getShareData(String token, ViewExecuteParam executeParam, User user)
            throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException, SQLException {

        if (null == executeParam || (CollectionUtils.isEmpty(executeParam.getGroups()) && CollectionUtils.isEmpty(executeParam.getAggregators()))) {
            return null;
        }

        ViewExecuteParam.decryptParam(executeParam);

        ShareInfo shareInfo = getShareInfo(token, user);
        verifyShareUser(user, shareInfo);

        ViewWithProjectAndSource viewWithProjectAndSource = viewMapper.getViewWithProjectAndSourceByWidgetId(shareInfo.getShareId());

        ProjectDetail projectDetail = projectService.getProjectDetail(viewWithProjectAndSource.getProjectId(), shareInfo.getShareUser(), false);
        boolean maintainer = projectService.isMaintainer(projectDetail, shareInfo.getShareUser());
        if (CollectionUtils.isEmpty(executeParam.getRowGroups()) && CollectionUtils.isEmpty(executeParam.getColGroups())) {
            return viewService.getResultDataList(maintainer, viewWithProjectAndSource, executeParam, shareInfo.getShareUser());
        } else {
            return viewService.getCrossResultDataList(maintainer, viewWithProjectAndSource, executeParam, shareInfo.getShareUser());
        }
    }


    /**
     * 分享数据生成csv文件并下载
     *
     * @param executeParam
     * @param user
     * @param token
     * @return
     */
    @Override
    public String generationShareDataCsv(ViewExecuteParam executeParam, User user, String token) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException {
        String filePath = null;
        ShareInfo shareInfo = getShareInfo(token, user);
        verifyShareUser(user, shareInfo);

        ViewWithSource viewWithSource = viewMapper.getViewWithProjectAndSourceByWidgetId(shareInfo.getShareId());
        ProjectDetail projectDetail = projectService.getProjectDetail(viewWithSource.getProjectId(), shareInfo.getShareUser(), false);
        ProjectPermission projectPermission = projectService.getProjectPermission(projectDetail, shareInfo.getShareUser());

        if (!projectPermission.getDownloadPermission()) {
            throw new ForbiddenException("ERROR Permission denied");
        }

        executeParam.setLimit(-1);
        executeParam.setPageSize(-1);
        executeParam.setPageNo(-1);

        PaginateWithQueryColumns paginate = null;
        try {
            boolean maintainer = projectService.isMaintainer(projectDetail, shareInfo.getShareUser());
            paginate = viewService.getResultDataList(maintainer, viewWithSource, executeParam, shareInfo.getShareUser());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ServerException(HttpCodeEnum.SERVER_ERROR.getMessage());
        }
        List<QueryColumn> columns = paginate.getColumns();

        if (!CollectionUtils.isEmpty(columns)) {
            String csvPath = fileUtils.fileBasePath + File.separator + "csv";
            File file = new File(csvPath);
            if (!file.exists()) {
                file.mkdirs();
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String csvName = viewWithSource.getName() + "_" + sdf.format(new Date());
            String fileFullPath = CsvUtils.formatCsvWithFirstAsHeader(csvPath, csvName, columns, paginate.getResultList());
            filePath = fileFullPath.replace(fileUtils.fileBasePath, EMPTY);
        }

        return serverUtils.getHost() + filePath;
    }

    /**
     * 获取分享distinct value
     *
     * @param token
     * @param viewId
     * @param param
     * @param user
     * @param request
     * @return
     */
    @Override
    public ResultMap getDistinctValue(String token, Long viewId, DistinctParam param, User user, HttpServletRequest request) {
        List<Map<String, Object>> list = null;
        try {

            ShareInfo shareInfo = getShareInfo(token, user);
            verifyShareUser(user, shareInfo);

            ViewWithProjectAndSource viewWithProjectAndSource = viewMapper.getViewWithProjectAndSourceById(viewId);
            if (null == viewWithProjectAndSource) {
                log.info("view (:{}) not found", viewId);
                return resultFail(user, request, null).message("view not found");
            }

            ProjectDetail projectDetail = projectService.getProjectDetail(viewWithProjectAndSource.getProjectId(), shareInfo.getShareUser(), false);

            if (!projectService.allowGetData(projectDetail, shareInfo.getShareUser())) {
                return resultFail(user, request, HttpCodeEnum.UNAUTHORIZED).message("ERROR Permission denied");
            }

            try {
                boolean maintainer = projectService.isMaintainer(projectDetail, shareInfo.getShareUser());

                // decry param
                DistinctParam.decryParams(param);

                list = viewService.getDistinctValueData(maintainer, viewWithProjectAndSource, param, shareInfo.getShareUser());
            } catch (ServerException e) {
                return resultFail(user, request, HttpCodeEnum.UNAUTHORIZED).message(e.getMessage());
            }
        } catch (NotFoundException e) {
            return resultFail(user, request, null).message(e.getMessage());
        } catch (ServerException e) {
            return resultFail(user, request, null).message(e.getMessage());
        } catch (UnAuthorizedException e) {
            return resultFail(user, request, HttpCodeEnum.FORBIDDEN).message(e.getMessage());
        }

        return resultSuccess(user, request).payloads(list);
    }

    @Override
    public ResultMap getUnionDistinctValue(String token, List<ViewDistinctParam> params, User user, HttpServletRequest request) {
        List<Map<String, Object>> list = null;
        try {

            ShareInfo shareInfo = getShareInfo(token, user);
            verifyShareUser(user, shareInfo);
            try {
                // decry param
                params.forEach(p -> DistinctParam.decryParams(p));
                list = viewService.getDistinctValueList(params, shareInfo.getShareUser());
            } catch (ServerException e) {
                return resultFail(user, request, HttpCodeEnum.UNAUTHORIZED).message(e.getMessage());
            }
        } catch (NotFoundException e) {
            return resultFail(user, request, null).message(e.getMessage());
        } catch (ServerException e) {
            return resultFail(user, request, null).message(e.getMessage());
        } catch (UnAuthorizedException e) {
            return resultFail(user, request, HttpCodeEnum.FORBIDDEN).message(e.getMessage());
        }

        return resultSuccess(user, request).payloads(list);
    }

    /**
     * 生成分享token
     *
     * @param shareEntityId
     * @param username
     * @return
     * @throws ServerException
     */
    @Override
    public String generateShareToken(Long shareEntityId, String username, Long userId) throws ServerException {
        /**
         * username: share实体Id:-:分享人id[:-:被分享人用户名]
         * password: share实体Id[:-:被分享人Id]
         */
        TokenEntity shareToken = new TokenEntity();
        String tokenUserName = shareEntityId + Constants.SPLIT_CHAR_STRING + userId;
        String tokenPassword = shareEntityId + EMPTY;
        if (!StringUtils.isEmpty(username)) {
//            PlatformShareAuth platformShareAuth = platformShareAuthMapper.selectByClientId(clientId);
            User user = userMapper.selectByUsername(username);
            if (null == user) {
                throw new ServerException("user : \"" + user + "\" not found");
            }
            tokenUserName += Constants.SPLIT_CHAR_STRING + username;
            tokenPassword += (Constants.SPLIT_CHAR_STRING + user.getId());
        }
        shareToken.setUsername(tokenUserName);
        shareToken.setPassword(tokenPassword);

        //生成token 并 aes加密
        return AESUtils.encrypt(tokenUtils.generateContinuousToken(shareToken), null);
    }

    @Override
    public String generateShareTokenForCustomExpire(Long contentId, Long userId, Long expire) throws ServerException {
        TokenEntity shareToken = new TokenEntity();
        String tokenUserName = contentId + Constants.SPLIT_CHAR_STRING + userId;
        String tokenPassword = contentId + EMPTY;
        shareToken.setUsername(tokenUserName);
        shareToken.setPassword(tokenPassword);

        //生成token 并 aes加密
        return AESUtils.encrypt(tokenUtils.generateToken(shareToken, expire), null);
    }

    public static void main(String[] args) {
        String token = "DA1C3E7DAF7EC46FDC39861F361C78B7517FC6CEF4F1ED425FFF75A2CB7527D3ABD4F0725AF12462EE9485E57D5234010260641CDAAEC4E9D462FB2898624C3B792270116A5B8C0526F4CC0327F84E9F066179FDE04FD90E959C92242768332512602E993DC452DFD3206FCE8D555B6AE318AB72C523CDE0C950C019EBEE101586999E8145B46CBA82D6A1608622C3165A26BAAFA0F0421235E814D33428BD29244106481614276C52793A32B600C0701C5F028D7F9E4167E1069111C503C6E4834C8E9BD3B3A9CC5E223CB055754B8C00306DD2B50DBB86459A04DCFE71D263D30C06927836039293A8EE3CFDC1F0E0776DD416F7E56A9650A804DFE1C68BBAFD857D64393B18ED579568243B197FBF";
        //AES解密
        String decrypt = AESUtils.decrypt(token, null);
        //获取分享信息
        String tokenUserName = new TokenUtils().getUsername(decrypt);
        String tokenPassword = new TokenUtils().getPassword(decrypt);
        System.out.println(tokenUserName);
        System.out.println(tokenPassword);
    }

    /**
     * 获取分享实体id
     *
     * @param token
     * @param user
     * @return
     * @throws ServerException
     * @throws UnAuthorizedException
     */
    @Override
    public ShareInfo getShareInfo(String token, User user) throws ServerException, ForbiddenException {

        if (StringUtils.isEmpty(token)) {
            throw new ServerException("Invalid share token");
        }

        //AES解密
        String decrypt = AESUtils.decrypt(token, null);
        //获取分享信息
        String tokenUserName = tokenUtils.getUsername(decrypt);
        String tokenPassword = tokenUtils.getPassword(decrypt);

        String[] tokenInfos = tokenUserName.split(Constants.SPLIT_CHAR_STRING);
        String[] tokenCrypts = tokenPassword.split(Constants.SPLIT_CHAR_STRING);

        if (tokenInfos.length < 2) {
            throw new ServerException("Invalid share token");
        }

        Long shareUserId = Long.parseLong(tokenInfos[1]);
        if (shareUserId.longValue() < 1L) {
            throw new ServerException("Invalid share token");
        }

        User shareUser = userMapper.getById(shareUserId);
        if (null == shareUser) {
            throw new ServerException("Invalid share token");
        }

        String sharedUserName = null;
        if (tokenInfos.length == 3) {
            if (tokenCrypts.length < 2) {
                throw new ServerException("Invalid share token");
            }
            String username = tokenInfos[2];
            Long sharedUserId = Long.parseLong(tokenCrypts[1]);
            User sharedUser = userMapper.selectByUsername(username);
            if (null == sharedUser || !sharedUser.getId().equals(sharedUserId)) {
                throw new ForbiddenException("The resource requires authentication, which was not supplied with the request");
            }

            if (null == user || (!user.getId().equals(sharedUserId) && !user.getId().equals(shareUserId))) {
                throw new ForbiddenException("The resource requires authentication, which was not supplied with the request");
            }

            sharedUserName = username;
        }

        Long shareId1 = Long.parseLong(tokenInfos[0]);
        Long shareId2 = Long.parseLong(tokenCrypts[0]);

        if (shareId1.longValue() < 1L || shareId2.longValue() < 1L || !shareId1.equals(shareId2)) {
            throw new ServerException("Invalid share token");
        }

        return new ShareInfo(shareId1, shareUser, sharedUserName);
    }

    @Override
    public void generateTracing(String opCode, String acessToken, Map<String, String> otherInfos, boolean isPlatform, HttpServletRequest request) {
        if (isPlatform) {
            String username = tokenUtils.getUsername(acessToken);
            String[] userNameSplit = username.split(Constants.SPLIT_CHAR_STRING);
            String password = tokenUtils.getPassword(acessToken);
            if (userNameSplit.length >= 2) {
                String extraInfo = username.split(Constants.SPLIT_CHAR_STRING)[1];
                String[] split = extraInfo.split(",");
                otherInfos.put("appCode", password);
                // 决策平台支持提供的埋点信息
                if (split.length >= 3 && "datav".equals(password)) {
                    otherInfos.put("userCode", split[2]);
                    otherInfos.put("reportId", split[0]);
                }
            }
        }
        LogUtil.userAction(request, Consts.LOG_SDK_OPCODE, opCode, otherInfos);
    }


    private ResultMap resultSuccess(User user, HttpServletRequest request) {
        if (null == user) {
            return new ResultMap().success();
        } else {
            return new ResultMap(tokenUtils).successAndRefreshToken(request);
        }
    }


    private ResultMap resultFail(User user, HttpServletRequest request, HttpCodeEnum httpCodeEnum) {
        if (null == user) {
            if (null != httpCodeEnum) {
                return new ResultMap().fail(httpCodeEnum.getCode());
            } else {
                return new ResultMap().fail();
            }
        } else {
            if (null != httpCodeEnum) {
                return new ResultMap(tokenUtils).failAndRefreshToken(request, httpCodeEnum);
            } else {
                return new ResultMap(tokenUtils).failAndRefreshToken(request);
            }
        }
    }
}

