package edp.davinci.dao;

import java.util.Date;
import java.util.List;
import java.util.Set;

import edp.davinci.model.SqlQueryInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.jdbc.object.SqlQuery;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/3/31
 */
@Component
public interface SqlQueryInfoMapper {
    int insert(SqlQueryInfo queryInfo);

    /**
     * 更新查询次数
     * @param id 主键
     * @param number 数量
     * @return
     */
    int updateQueryNumber(Integer id, Integer number);

    /**
     *
     * @param part
     * @return
     */
    List<SqlQueryInfo> listInfo(String part);
}
