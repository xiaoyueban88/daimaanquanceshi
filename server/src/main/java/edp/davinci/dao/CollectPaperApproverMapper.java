package edp.davinci.dao;

import java.util.List;

import edp.davinci.model.CollectPaperApprover;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/10
 */
@Component
public interface CollectPaperApproverMapper {
    @Insert({"insert into collect_paper_approver(`user`, `paper_id`, `is_delete`) values (#{user}, #{paperId}, #{isDelete})"})
    int insert(CollectPaperApprover collectPaperApprover);

    @Update({"update collect_paper_approver set is_delete=1 where paper_id = #{paperId} and is_delete=0"})
    int delete(@Param("paperId") Long paperId);

    List<Long> getPaperIdByUser(@Param("projectId") Long projectId, @Param("username") String userName, @Param("keyword") String keyword,
                                @Param("start") Integer start, @Param("end") Integer end);

    @Select({"select user from collect_paper_approver where paper_id = #{paperId} and is_delete=0"})
    String getApproverByPaperId(@Param("paperId") Long paperId);

    int getCount(@Param("projectId") Long projectId, @Param("username") String userName, @Param("keyword") String keyword);
}
