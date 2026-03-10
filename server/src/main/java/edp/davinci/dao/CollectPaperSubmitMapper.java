package edp.davinci.dao;

import java.util.Date;
import java.util.List;

import edp.davinci.model.CollectPaperSubmit;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/14
 */
@Component
public interface CollectPaperSubmitMapper {
    int insert(CollectPaperSubmit submit);

    int getCount(@Param("paperId") Long paperId, @Param("status") Short status);

    List<CollectPaperSubmit> getListByPage(@Param("paperId") Long paperId, @Param("status") Short status,
                                           @Param("start") Integer start, @Param("end") Integer end);


    int approveSubmits(@Param("approver") String approver, @Param("paperId") Long paperId,
                       @Param("list") List<Long> submitIds, @Param("approveTime") Date approveTime);

    @Update({"update collect_paper_submit set is_delete=1 where paper_id = #{paperId}"})
    int deleteByPaperId(@Param("paperId") Long paperId);

    @Select({"select id, create_time from collect_paper_submit where is_delete=0"})
    List<CollectPaperSubmit> getSimpleSubmit();

    int removeBatch(@Param("list") List<Long> submitIds);

    @Select("select * from collect_paper_submit where id = #{id} and is_delete=0")
    CollectPaperSubmit getSubmitById(@Param("id") Long id);
}
