package edp.davinci.dto.WidgetFolder;

import java.util.Set;

import edp.davinci.model.WidgetFolder;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/4/30
 */
public class WidgetFolderDto extends WidgetFolder {

    public WidgetFolderDto() {
    }

    public WidgetFolderDto(WidgetFolder widgetFolder) {
        this.setId(widgetFolder.getId());
        this.setName(widgetFolder.getName());
        this.setParentId(widgetFolder.getParentId());
        this.setProjectId(widgetFolder.getProjectId());
    }

    /**
     * 子文件夹
     */
    private Set<WidgetFolderDto> children;

    public Set<WidgetFolderDto> getChildren() {
        return children;
    }

    public void setChildren(Set<WidgetFolderDto> children) {
        this.children = children;
    }

    private WidgetFolderDto(Builder builder) {
        this.setId(builder.id);
        this.setName(builder.name);
        this.setParentId(builder.parentId);
        this.setProjectId(builder.projectId);
        this.setChildren(builder.children);
    }

    public static class Builder {
        private Long id;
        private String name;
        private Long parentId;
        private Long projectId;
        private Set<WidgetFolderDto> children;

        public Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder parentId(Long parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder children(Set<WidgetFolderDto> children) {
            this.children = children;
            return this;
        }

        public WidgetFolderDto build() {
            return new WidgetFolderDto(this);
        }

    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.getId().equals(getId());
    }
}
