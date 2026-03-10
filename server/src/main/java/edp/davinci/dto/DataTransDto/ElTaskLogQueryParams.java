package edp.davinci.dto.DataTransDto;

import javax.validation.constraints.NotNull;

import com.iflytek.edu.zx.etl.constant.ElLogQuery;

/**
 * @Description ${description}
 * @author zswu3
 * @date 2021/5/10
 */
@NotNull(message = "ElTaskLogQueryParams cannot be null")
public class ElTaskLogQueryParams {
    /**
     * 流程id
     */
    @NotNull(message = "processId cannot be null")
    private String processId;

    /**
     * 日志查询参数
     */
    @NotNull(message = "jobQuery cannot be null")
    private ElLogQuery jobQuery;

    @NotNull(message = "pageIndex cannot be null")
    private int pageIndex;

    @NotNull(message = "pageSize cannot be null")
    private int pageSize;

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public ElLogQuery getJobQuery() {
        return jobQuery;
    }

    public void setJobQuery(ElLogQuery jobQuery) {
        this.jobQuery = jobQuery;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
