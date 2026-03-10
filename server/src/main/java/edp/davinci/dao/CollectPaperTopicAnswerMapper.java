package edp.davinci.dao;

import java.util.List;

import edp.davinci.model.CollectPaperTopicAnswer;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/11
 */
@Component
public interface CollectPaperTopicAnswerMapper {
    void insertBatch(@Param("list") List<CollectPaperTopicAnswer> answers);

    List<CollectPaperTopicAnswer> getBySubmitId(@Param("list") List<Long> submitIds);

    @Delete({"delete from collect_paper_topic_answer where submit_id = #{submitId}"})
    int deleteBySubmitId(@Param("submitId") Long submitId);
}
