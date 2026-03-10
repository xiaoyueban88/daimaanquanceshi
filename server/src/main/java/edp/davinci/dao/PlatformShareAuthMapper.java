package edp.davinci.dao;

import edp.davinci.model.PlatformShareAuth;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/2/4
 */

@Component
public interface PlatformShareAuthMapper {
    @Select({"select * from `platform_share_auth` where `client_id` = #{clientId}"})
    PlatformShareAuth selectByClientId(@Param("clientId") String clientId);

}
