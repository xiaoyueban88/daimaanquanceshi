package edp.davinci.dao;

import java.util.List;

import edp.davinci.dto.CollectDto.PaperApproveDto;
import edp.davinci.dto.CollectDto.PaperConfigDto;
import edp.davinci.model.CollectPaper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/9
 */
@Component
public interface CollectPaperMapper {

    int insert(CollectPaper collectPaper);

    @Update({
            "update collect_paper",
            "set `status` = #{status,jdbcType=SMALLINT},",
            "`type` = #{type,jdbcType=SMALLINT},",
            "`deadline` = #{deadline,jdbcType=TIMESTAMP},",
            "`expires` = #{expires,jdbcType=INTEGER},",
            "`update_time` = #{updateTime,jdbcType=TIMESTAMP}",
            "where id = #{paperId,jdbcType=BIGINT} and is_delete=0"
    })
    int updatePaperConfig(PaperConfigDto paperConfigDto);

    @Update({"update collect_paper set is_delete=1 where id=#{id} and is_delete=0"})
    int delete(@Param("id") Long id);


    List<CollectPaper> getMyPaperByPager(@Param("projectId") Long projectId,
                                         @Param("username") String username,
                                         @Param("keyword") String keyword,
                                         @Param("start") Integer startIndex,
                                         @Param("end") Integer endIndex);

    Integer getMyPaperCount(@Param("projectId") Long projectId,
                            @Param("username") String username,
                            @Param("keyword") String keyword);

    @Select({"select paper.* from collect_paper paper left join collect_paper_approver app on" +
            " (app.paper_id=paper.id and app.is_delete=0 and paper.is_delete=0)  where paper.id = #{id} and paper.is_delete=0"})
    CollectPaper getById(@Param("id") Long id);

    @Select({"select id, status, deadline, expires from collect_paper where is_delete=0"})
    List<CollectPaper> getSimplesPapers();

    List<PaperApproveDto> getPaperApproveInfo(@Param("list") List<Long> paperIds);

    @Update({"update collect_paper set title=#{title} where id =#{paperId} and is_delete=0"})
    int updateTitle(@Param("paperId") Long paperId, @Param("title") String title);

    int updateStatus(@Param("list") List<Long> paperIds, @Param("status") Short status);
}
