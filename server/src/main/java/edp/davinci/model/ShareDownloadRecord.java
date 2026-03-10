package edp.davinci.model;

import lombok.Data;

@Data
public class ShareDownloadRecord extends DownloadRecordBaseInfo {

    private Long id;

    private String uuid;

    /**
     * 下载类型：1 单个下载, 2 批量下载
     */
    private Integer downloadType = 1;
}