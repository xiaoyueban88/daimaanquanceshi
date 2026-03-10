package edp.davinci.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/4/30
 */
@Data
public class WidgetFolder {

    private Long id;

    /**
     * 文件夹名称
     */
    @NotBlank(message = "widgetfolder name cannot be EMPTY")
    private String name;

    /**
     * 父文件夹id
     */
    @Min(value = 0L, message = "Invalid parent id")
    private Long parentId;

    /**
     * 所在项目id
     */
    @Min(value = 1L, message = "Invalid project id")
    private Long projectId;
}
