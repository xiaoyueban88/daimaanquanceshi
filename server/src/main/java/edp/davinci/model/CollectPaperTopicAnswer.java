package edp.davinci.model;

import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/8
 */
@Data
public class CollectPaperTopicAnswer {

    private Long id;

    /**
     * 提交记录id
     */
    private Long submitId;

    /**
     * fieldName
     */
    private String fieldName;


    /**
     * 作答内容
     */
    private String answer;
}
