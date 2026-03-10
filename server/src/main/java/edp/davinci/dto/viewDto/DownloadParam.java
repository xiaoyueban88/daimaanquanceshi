package edp.davinci.dto.viewDto;

import lombok.Data;

import java.util.List;

@Data
public class DownloadParam {

    /**
     * 下载类型：1 普通下载, 2 批量下载
     */
    private Integer downloadType;

    private List<DownloadViewExecuteParam> params;
}
