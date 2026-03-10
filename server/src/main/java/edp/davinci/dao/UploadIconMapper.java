package edp.davinci.dao;

import java.util.List;

import edp.davinci.model.UploadIcon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/12/3
 */
@Component
public interface UploadIconMapper {
    /**
     * 插入图标上传信息
     *
     * @param uploadIcon
     * @return
     */
    int insert(UploadIcon uploadIcon);

    /**
     * 根据图标名称删除图标信息
     *
     * @param id
     * @return
     */
    @Update({
            "update upload_icon set is_delete = 1 where is_delete=0 and id=#{id}"
    })
    int delete(@Param("id") Integer id);

    /**
     * 根据名称获取图标信息
     *
     * @param name
     * @return
     */
    @Select({
            "select * from upload_icon where is_delete=0 and name=#{name}"
    })
    UploadIcon getUploadIconByName(@Param("name") String name);

    /**
     * 获取图标列表信息
     *
     * @return
     */
    @Select({
            "select * from upload_icon where is_delete=0"
    })
    List<UploadIcon> getAllUploadIcons();
}
