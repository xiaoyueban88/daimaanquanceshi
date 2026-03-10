package edp.davinci.service.impl;

import java.util.List;

import com.zhixue.auth.model.User;

import edp.core.exception.ServerException;
import edp.core.exception.UnAuthorizedException;
import edp.davinci.dao.CustomizeMetricsFavMapper;
import edp.davinci.model.CustomizeMetricsFav;
import edp.davinci.service.CustomizeMetricsFavService;
import edp.system.util.SessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/4/28
 */
@Service("customizeMetricsFavService")
public class CustomizeMetricsFavServiceImpl implements CustomizeMetricsFavService {

    @Autowired
    private CustomizeMetricsFavMapper customizeMetricsFavMapper;

    @Override
    public CustomizeMetricsFav favCustomizeMetrics(CustomizeMetricsFav customizeMetricsFav) throws UnAuthorizedException, ServerException {
        User currentUser = new SessionUtil().getCurrentUser();
        if (currentUser == null) {
            throw new UnAuthorizedException("收藏失败, 未检测到登录用户");
        }
        customizeMetricsFav.setCreator(currentUser.getId());
        int insert = customizeMetricsFavMapper.insert(customizeMetricsFav);
        if (insert > 0) {
            return customizeMetricsFav;
        } else {
            throw new ServerException("create dashboard fail");
        }
    }

    @Override
    public void updateFavMetrics(CustomizeMetricsFav customizeMetricsFav) throws UnAuthorizedException, ServerException {
        User currentUser = new SessionUtil().getCurrentUser();
        if (currentUser == null) {
            throw new UnAuthorizedException("更新失败, 未检测到登录用户");
        }
        int update = customizeMetricsFavMapper.update(customizeMetricsFav);
        if (update <= 0) {
            throw new ServerException("更新失败");
        }
    }

    @Override
    public void removeFavMetrics(Long id) throws UnAuthorizedException, ServerException {
        User currentUser = new SessionUtil().getCurrentUser();
        if (currentUser == null) {
            throw new UnAuthorizedException("移除失败, 未检测到登录用户");
        }
        int delete = customizeMetricsFavMapper.delete(id);
        if (delete <= 0) {
            throw new ServerException("删除失败");
        }
    }

    @Override
    public List<CustomizeMetricsFav> getMyFavMetricsByViewId(Long viewId) throws UnAuthorizedException, ServerException {
        User currentUser = new SessionUtil().getCurrentUser();
        if (currentUser == null) {
            throw new UnAuthorizedException("获取收藏列表失败, 未检测到登录用户");
        }
        List<CustomizeMetricsFav> list = customizeMetricsFavMapper.getListByViewId(viewId);
        return list;
    }
}
