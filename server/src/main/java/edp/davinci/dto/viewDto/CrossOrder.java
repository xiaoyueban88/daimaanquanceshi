package edp.davinci.dto.viewDto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

import edp.core.enums.DataTypeEnum;
import edp.core.utils.CollectionUtils;
import edp.core.utils.SqlUtils;
import lombok.Data;

import static edp.core.consts.Consts.PATTERN_SQL_AGGREGATE;

/**
 * @Description 交叉排序
 * @author zswu3
 * @date 2021/3/12
 */
@Data
public class CrossOrder {
    /**
     * 排序条件
     */
    private List<Param> conditions;

    /**
     * 指标排序
     */
    private Order order;

    public Order getOrder(DataTypeEnum dataTypeEnum) {
        String keywordPrefix = dataTypeEnum.getKeywordPrefix();
        String keywordSuffix = dataTypeEnum.getKeywordSuffix();
        if(!order.getColumn().startsWith(keywordPrefix)) {
            order.setColumn(keywordPrefix + order.getColumn() + keywordSuffix);
        }
        return order;
    }
}
