package edp.davinci.dao;

import java.util.List;

import edp.davinci.model.CustomizeMetricsFav;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/4/28
 */

@Component
public interface CustomizeMetricsFavMapper {

    int insert(CustomizeMetricsFav customizeMetricsFav);

    @Delete({"delete from customize_metrics_fav where id = #{id}"})
    int delete(@Param("id") Long id);

    @Select({"select * from customize_metrics_fav where view_id = #{viewId}"})
    List<CustomizeMetricsFav> getListByViewId(@Param("viewId") Long viewId);

    @Update({
            "update customize_metrics_fav",
            "set calculation_rules = #{calculationRules,jdbcType=VARCHAR},",
            "alias = #{alias,jdbcType=VARCHAR},",
            "customize_metrics_fav.desc = #{desc,jdbcType=VARCHAR}",
            "where id = #{id,jdbcType=BIGINT}"
    })
    int update(CustomizeMetricsFav customizeMetricsFav);
}
