package edp.davinci.service.excel;

import edp.davinci.dto.viewDto.Aggregator;
import edp.davinci.dto.viewDto.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @ClassName GroupsContext
 * @Author rqliu3
 * @CreateDate 2021/08/11 11:31
 * @Description :
 **/
public class GroupsContext {
    private List<String> colGroups;
    private List<String> rowGroups;
    private List<Order> colOrders;
    private List<Order> rowOrders;
    //指标
    private Aggregator metric;

    public GroupsContext() {
    }

    public List<String> getColGroups() {
        return colGroups;
    }

    public void setColGroups(List<String> colGroups) {
        this.colGroups = colGroups;
    }

    public List<String> getRowGroups() {
        return rowGroups;
    }

    public void setRowGroups(List<String> rowGroups) {
        this.rowGroups = rowGroups;
    }

    public List<Order> getColOrders() {
        return colOrders;
    }

    public void setColOrders(List<Order> colOrders) {
        this.colOrders = colOrders;
    }

    public List<Order> getRowOrders() {
        return rowOrders;
    }

    public void setRowOrders(List<Order> rowOrders) {
        this.rowOrders = rowOrders;
    }


    public Aggregator getMetric() {
        return metric;
    }

    public void setMetric(Aggregator metric) {
        this.metric = metric;
    }

    public GroupsContext(List<String> colGroups, List<String> rowGroups, List<Order> colOrders, List<Order> rowOrders, Aggregator metric) {
        this.colGroups = colGroups;
        this.rowGroups = rowGroups;
        this.colOrders = colOrders;
        this.rowOrders = rowOrders;
        this.metric = metric;
    }
}
