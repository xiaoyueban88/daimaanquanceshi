package edp.davinci.model;

import java.util.Date;

import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/3/31
 */
@Data
public class SqlQueryInfo {
    private Integer id;

    /**
     * detail 加密后的信息
     */
    private String md5Key;

    /**
     * 查询sql
     */
    private String sql;

    private Integer pageNo;

    private Integer pageSize;

    /**
     * sql查询语句查询的表
     */
    private String tables;

    /**
     * 数据源id
     */
    private Long sourceId;

    /**
     * distinct: 全局过滤器下拉框
     * data: widget数据
     */
    private String type;

    /**
     * 查询次数
     */
    private Integer queryNumber;

    /**
     * 查询时间ms
     */
    private Integer queryMs;

    /**
     * 日期字符串
     */
    private String part;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
