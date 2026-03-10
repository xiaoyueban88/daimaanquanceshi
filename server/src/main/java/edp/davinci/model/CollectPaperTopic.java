package edp.davinci.model;

import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/8
 */
@Data
public class CollectPaperTopic {

    private Long id;

    /**
     * 所在的问卷id
     */
    private Long paperId;

    /**
     * 问卷题目标识
     */
    private String key;

    /**
     * 问卷题目对应的字段
     */
    private String fieldName;

    /**
     * 问卷题目类型
     */
    private String type;

    /**
     * 配置信息
     */
    private String config;

    /**
     * 排序
     */
    private Integer order;

    /**
     * 删除标记
     */
    private Short isDelete = 0;
}
