package edp.davinci.service;

import java.util.List;

import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.davinci.model.CustomizeMetricsFav;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/4/28
 */
public interface CustomizeMetricsFavService {
    /**
     * 收藏自定义指标
     *
     * @param customizeMetricsFav
     * @return
     */
    CustomizeMetricsFav favCustomizeMetrics(CustomizeMetricsFav customizeMetricsFav) throws UnAuthorizedException, ServerException;

    /**
     * 更新已收藏的自定义指标
     *
     * @param customizeMetricsFav
     * @return
     */
    void updateFavMetrics(CustomizeMetricsFav customizeMetricsFav) throws UnAuthorizedException, ServerException;

    /**
     * 移出收藏
     *
     * @param id
     * @return
     */
    void removeFavMetrics(Long id) throws UnAuthorizedException, ServerException;

    /**
     * 根据viewId获取收藏的自定义指标列表
     *
     * @param viewId
     * @return
     */
    List<CustomizeMetricsFav> getMyFavMetricsByViewId(Long viewId) throws UnAuthorizedException, ServerException;
}
