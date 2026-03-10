package edp.davinci.dto.DataTransDto;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.iflytek.edu.zx.etl.model.ColumnBasicInfo;

import lombok.Data;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/4/21
 */
@Data
@NotNull(message = "view cannot be null")
public class DataTransParamDto {
    @NotBlank(message = "tableName cannot be EMPTY")
    String tableName;

    @NotNull(message = "chColumnInfocannot be EMPTY")
    List<ColumnBasicInfo> chColumnInfo;

    String partition;

    /**
     * 对应clickhouse的数据库名
     */
    @NotBlank(message = "tableName cannot be EMPTY")
    String dbName;

    String orderColumns;

    /**
     * 是否是增量
     */
    Boolean isIncr;
}
