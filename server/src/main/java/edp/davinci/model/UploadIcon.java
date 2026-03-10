package edp.davinci.model;

import java.util.Date;

import lombok.Builder;
import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/12/3
 */
@Data
@Builder
public class UploadIcon {
    private Integer id;

    /**
     * icon名称
     */
    private String name;

    /**
     * 图标路径
     */
    private String path;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人域账号
     */
    private String creator;
}
