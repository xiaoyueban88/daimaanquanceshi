package edp.davinci.aspect;

import java.util.Set;

import javax.annotation.Resource;

import com.alibaba.druid.util.StringUtils;
import com.iflytek.edu.zx.etl.model.ElRoute;
import com.iflytek.edu.zx.etl.service.RouterService;

import edp.core.utils.CollectionUtils;
import edp.core.utils.SqlUtils;
import edp.davinci.dto.viewDto.ViewExecuteSql;
import edp.davinci.dto.viewDto.ViewWithSource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author zswu3
 * @Description 根据路由表更新情况替换查询sql中的表名
 * @date 2020/10/26
 */
@Aspect
@Component
@Slf4j
public class ViewTableReplaceAspect {

    @Resource
    private RouterService routerService;

    @Resource
    private SqlUtils sqlUtils;

    @Pointcut("execution(* edp.davinci.service.impl.ViewServiceImpl.getResultDataList(..))" +
            " || execution(* edp.davinci.service.impl.ViewServiceImpl.getCrossResultDataList(..))" +
            " || execution(* edp.davinci.service.impl.ViewServiceImpl.getSQLContext(..))" +
            " || execution(* edp.davinci.service.impl.ViewServiceImpl.getDistinctSql(..))")
    public void pointCut() {
    }

    @Pointcut("execution(* edp.davinci.service.impl.ViewServiceImpl.executeSql(..))")
    public void pointCut1() {
    }

    @Around("pointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Object[] args = point.getArgs();
        ViewWithSource source = (ViewWithSource) args[1];
        replaceSqlTable(source);
        args[1] = source;
        return point.proceed(args);
    }

    @Around("pointCut1()")
    public Object aroundExecuteSql(ProceedingJoinPoint point) throws Throwable {
        Object[] args = point.getArgs();
        ViewExecuteSql viewExecuteSql = (ViewExecuteSql) args[0];
        replaceSqlTable(viewExecuteSql);
        args[0] = viewExecuteSql;
        return point.proceed(args);
    }

    /**
     * 表名替换
     *
     * @param source
     */
    private void replaceSqlTable(ViewWithSource source) {
        if (null == source || StringUtils.isEmpty(source.getSql())) {
            return;
        }

        // 获取sql中涉及到的表名(带数据库名)
        Set<String> tableNames = sqlUtils.getTableNames(source.getId());
        source.setSql(getNewSq(source.getSql(), tableNames));
    }

    /**
     * 表名替换
     *
     * @param viewExecuteSql
     */
    private void replaceSqlTable(ViewExecuteSql viewExecuteSql) {
        if (null == viewExecuteSql || StringUtils.isEmpty(viewExecuteSql.getSql())) {
            return;
        }

        Set<String> tableNames = sqlUtils.getTableNameBySql(viewExecuteSql.getSourceId(), viewExecuteSql.getSql());
        viewExecuteSql.setSql(getNewSq(viewExecuteSql.getSql(), tableNames));
    }

    /**
     * 获取替换tableNames后的sql
     *
     * @param sourceSql  源sql
     * @param tableNames 要替换的表名
     * @return
     */
    private String getNewSq(String sourceSql, Set<String> tableNames) {
        if (StringUtils.isEmpty(sourceSql) || CollectionUtils.isEmpty(tableNames)) {
            return sourceSql;
        }
        for (String tableName : tableNames) {
            try {
                // 获取激活的路由表名
                String[] dbAndTable = tableName.split("\\.");
                String routeTable = null;
                ElRoute activeElTable = routerService.getActiveTableName(tableName);
                if (null == activeElTable) {
                    activeElTable = routerService.getActiveTableName(dbAndTable[1]);
                    if (null != activeElTable) {
                        routeTable = activeElTable.getRouteTable();
                    }
                } else {
                    String[] tableSplits = activeElTable.getRouteTable().split("\\.");
                    if (tableSplits.length > 1) {
                        routeTable = tableSplits[1];
                    } else if (tableSplits.length == 1) {
                        routeTable = tableSplits[0];
                    }
                }
                if (!StringUtils.isEmpty(routeTable)) {
                    sourceSql = sourceSql.replace(dbAndTable[1], routeTable);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return sourceSql;
    }
}
