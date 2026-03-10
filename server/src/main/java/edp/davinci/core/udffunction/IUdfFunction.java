package edp.davinci.core.udffunction;

import edp.davinci.dto.viewDto.ExtraAggregator;
import edp.davinci.dto.viewDto.UdfFunctionDto;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author zswu3
 * @Description 自定义函数类接口
 * @date 2020/9/7
 */
public interface IUdfFunction {
    /**
     * 函数解析成sql模板stg参数
     */
    Pair<ExtraAggregator, ExtraAggregator> prepare(UdfFunctionDto udfFunctionDto);

    /**
     * widgetsql执行结果处理
     */
    void post();
}
