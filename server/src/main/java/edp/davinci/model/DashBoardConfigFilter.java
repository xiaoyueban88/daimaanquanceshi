package edp.davinci.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashBoardConfigFilter {

    private String key;

    private String name;

    private String type;

    private String interactionType;

    private String operator;

    private Boolean cache;

    private Long expired;

    private Double width;

    private Map<String, RelatedItem> relatedItems;

    private Map<String, RelatedView> relatedViews;

    private String titlePosition;

    private Boolean customOptions;

    private List<String> options;

    private String parent;
}
