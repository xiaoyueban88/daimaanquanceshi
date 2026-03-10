package edp.davinci.dao;

import java.util.List;
import java.util.Set;

import edp.davinci.model.WidgetFolder;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/4/30
 */
@Component
public interface WidgetFolderMapper {

    int insert(WidgetFolder widgetFolder);

    @Update({
            "update widget_folder",
            "set name = #{name,jdbcType=VARCHAR},",
            "parent_id = #{parentId, jdbcType=BIGINT}",
            "where id = #{id,jdbcType=BIGINT}"
    })
    int update(WidgetFolder widgetFolder);

    @Delete({"delete from widget_folder where project_id = #{projectId} and id = #{id}"})
    int delete(@Param("projectId") Long projectId, @Param("id") Long id);

    @Select({"select * from widget_folder where project_id = #{projectId}"})
    List<WidgetFolder> getWidgetFoldersByProjectId(@Param("projectId") Long projectId);

    @Select({"select id from widget_folder where parent_id = #{parentId}"})
    Set<Long> getFolderIdsByParentId(@Param("parentId") Long parentId);

    @Select({"select id from widget_folder where name like concat('%',#{folderName},'%')"})
    List<Long> getFolderIdsByFolderName(@Param("folderName") String folderName);
}
