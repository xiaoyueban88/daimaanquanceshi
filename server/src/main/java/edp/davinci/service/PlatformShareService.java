package edp.davinci.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import edp.core.exception.ForbiddenException;
import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.core.model.Paginate;
import edp.davinci.core.common.ResultMap;
import edp.davinci.dto.shareDto.PlatformShareInfo;
import edp.davinci.dto.shareDto.ShareDashboard;
import edp.davinci.dto.shareDto.ShareWidget;
import edp.davinci.dto.viewDto.DistinctParam;
import edp.davinci.dto.viewDto.ViewDistinctParam;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.model.ShareDownloadRecord;
import edp.davinci.model.User;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/2/4
 */
public interface PlatformShareService {
    Pair<String, String> getAuthToken(String clientSecret, String shareToken, String extraInfo);

    ShareDashboard getShareDashboard(String token, String acessToken) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException;

    ShareWidget getShareWidget(String token, String clientId) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException;

    PlatformShareInfo getShareInfo(String shareToken) throws ServerException, ForbiddenException;

    Paginate<Map<String, Object>> getShareData(String token, ViewExecuteParam executeParam, String accessToken) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException, SQLException;

    ResultMap getDistinctValue(String token, Long viewId, DistinctParam param);

    String generateShareToken(Long shareEntityId, String clientId, Long userId) throws ServerException;

    String shareDashboard(Long dashboardId, String clientId, User user) throws NotFoundException, UnAuthorizedException, ServerException;

    /**
     * 获取distinct集合
     *
     * @param token
     * @param params
     * @param request
     * @return
     */
    ResultMap getUnionDistinctValue(String token, List<ViewDistinctParam> params, HttpServletRequest request);

    /**
     * 根据widgetId获取分享widget信息
     * @param token
     * @param widgetId
     * @return
     * @throws NotFoundException
     * @throws ServerException
     * @throws ForbiddenException
     * @throws UnAuthorizedException
     */
    ShareWidget getShareWidgetInfo(String token, Long widgetId) throws NotFoundException, ServerException, ForbiddenException, UnAuthorizedException;

    /**
     * 第三方获取下载信息
     * @param clientSecret
     * @param shareToken
     * @param recordId
     * @return
     */
    ShareDownloadRecord getDownloadRecordById(String clientSecret, String shareToken, Integer recordId);
}
