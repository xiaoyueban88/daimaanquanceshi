package edp.davinci.dto.WidgetContainer;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/2/28
 */
@NotNull(message = "widgetcontainer cannot be null")
@Data
public class WidgetContainerUpdate {
    @Min(value = 1L, message = "Invalid id")
    private Long id;

//    @NotBlank(message = "widgetcontainer name cannot be EMPTY")
    private String name;

    private String description;

//    @NotBlank(message = "widgetcontainer config cannot be EMPTY")
    private String config;

    private String dispose;

    private Long folderId;
}
