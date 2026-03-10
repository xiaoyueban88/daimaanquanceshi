package edp.davinci.model;

import lombok.Data;

import java.util.List;

@Data
public class DashBoardConfig {

    private List<DashBoardConfigFilter> filters;

    private String queryMode;

    private String gridItemMargin;
}
