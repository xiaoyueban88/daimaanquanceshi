package edp.davinci.model;

import java.util.Date;

import lombok.Builder;
import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/11/10
 */
@Data
@Builder
public class DashboardDownloadTemplate {
    private Long id;

    /**
     * 看板id
     */
    private Long dashboardId;

    /**
     * 下载模板相对路径
     */
    private String path;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private String createUser;

    /**
     * 删除标记 1-已删除
     */
    private Short isDelete;
}
