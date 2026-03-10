package edp.davinci.dto.CollectDto;

import java.util.List;
import java.util.Map;

import edp.davinci.model.CollectPaperTopic;
import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/13
 */
@Data
public class PaperAnswerInfoDto {
    /**
     * 问卷题目信息
     */
    List<CollectPaperTopic> topic;

    /**
     * 问卷答题情况
     */
    List<Map<String, Object>> answerMap;

    /**
     * 答题总数
     */
    Integer count;

    /**
     * 问卷类型
     */
    Short type;
}
