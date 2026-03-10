package edp.davinci.dto.DataTransDto;

import java.util.List;

/**
 * @Description ${description}
 * @author zswu3
 * @date 2021/5/24
 */
public class ProcessDto {
    // 显示名称
    private String displayName;
    // 名称
    private String name;
    // 实例生成url
    private String instanceUrl;

    private String creator;

    private List<ProcessStateDto> processStateDtos;
    private List<ProcessPathDto> processPathDtos;

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public List<ProcessStateDto> getProcessStateDtos() {
        return processStateDtos;
    }

    public void setProcessStateDtos(List<ProcessStateDto> processStateDtos) {
        this.processStateDtos = processStateDtos;
    }

    public List<ProcessPathDto> getProcessPathDtos() {
        return processPathDtos;
    }

    public void setProcessPathDtos(List<ProcessPathDto> processPathDtos) {
        this.processPathDtos = processPathDtos;
    }
}
