package edp.davinci.service.excel;

import lombok.Data;

import java.util.List;

/**
 * 下载配置
 * @author wghu
 */
@Data
public class DownloadConfig {
    /**
     * 是否开启批量下载
     */
    private Boolean onOff;

    /**
     * 过滤器必选项
     */
    private List<String> requiredFilters;

    /**
     * 过滤器组合项
     */
    private List<String> groupFilters;

    /**
     * 命名模板
     */
    private String nameTemplate;

    /**
     * 单词明名模板
     */
    private String onceNameTemplate;

}