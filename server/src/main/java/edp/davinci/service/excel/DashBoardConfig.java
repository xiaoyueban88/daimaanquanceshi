package edp.davinci.service.excel;

import lombok.Data;

import java.util.List;

@Data
public class DashBoardConfig {

    private List<String> filters;

    private Long queryMode;

    private Long gridItemMargin;

    private DownloadConfig downloadConfig;

}
