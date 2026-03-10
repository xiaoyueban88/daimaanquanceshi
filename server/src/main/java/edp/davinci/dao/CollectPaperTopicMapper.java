package edp.davinci.dao;

import java.util.List;

import edp.davinci.model.CollectPaperTopic;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/9
 */
@Component
public interface CollectPaperTopicMapper {

    int insertBatch(@Param("list") List<CollectPaperTopic> topics);

    @Delete({"update collect_paper_topic set is_delete=1 where paper_id = #{paperId} and is_delete=0"})
    int delete(@Param("paperId") Long paperId);

    @Select({"select * from collect_paper_topic where paper_id=#{paperId} and is_delete=0 order by `order`"})
    List<CollectPaperTopic> getTopicsByPaper(@Param("paperId") Long paperId);
}
