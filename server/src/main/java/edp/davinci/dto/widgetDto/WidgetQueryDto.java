package edp.davinci.dto.widgetDto;
/**
 * @Description ${description}
 * @author zswu3
 * @date 2021/7/22
 */
public class WidgetQueryDto {
    /**
     * widget 名称, 模糊匹配
     */
    private String widgetName;

    /**
     * widget所在文件夹名称, 支持模糊匹配
     */
    private String folderName;

    private Integer pageIndex;

    private Integer pageSize;

    public String getWidgetName() {
        return widgetName;
    }

    public void setWidgetName(String widgetName) {
        this.widgetName = widgetName;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public Integer getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(Integer pageIndex) {
        this.pageIndex = pageIndex;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
