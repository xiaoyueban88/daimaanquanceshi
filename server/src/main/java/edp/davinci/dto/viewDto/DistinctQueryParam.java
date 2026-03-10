package edp.davinci.dto.viewDto;

import java.util.List;

/**
 * @Description 去重查询参数
 * @author zswu3
 * @date 2021/8/26
 */
public class DistinctQueryParam {
    /**
     * 去重查询列
     */
    private List<String> columns;

    /**
     * 过滤条件
     */
    private List<String> filters;

    /**
     * 查询变量
     */
    private List<Param> params;

    /**
     * 排序列
     */
    private List<Order> orders;

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    public List<Param> getParams() {
        return params;
    }

    public void setParams(List<Param> params) {
        this.params = params;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }
}
