package edp.davinci.model;

import edp.core.model.RecordInfo;
import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/2/26
 */
@Data
public class WidgetContainer extends RecordInfo<WidgetContainer> {
    private Long id;

    /**
     * name
     */
    private String name;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 子tab和widget的关联信息
     * [
     * {
     * index: 0,
     * name: "tab1",
     * widgetId: 11
     * }
     * ]
     */
    private String config;

    /**
     * 所属项目id
     */
    private Long projectId;

    /**
     * 挂载的目录id
     */
    private Long folderId;

    private String folderName;

    /**
     * widget tabs前端配置
     */
    private String dispose;
}
