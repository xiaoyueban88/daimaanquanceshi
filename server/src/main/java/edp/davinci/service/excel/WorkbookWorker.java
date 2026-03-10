/*
 * <<
 *  Davinci
 *  ==
 *  Copyright (C) 2016 - 2019 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package edp.davinci.service.excel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import edp.core.utils.CollectionUtils;
import edp.core.utils.FileUtils;
import edp.core.utils.SqlUtils;
import edp.davinci.common.utils.ScriptUtiils;
import edp.davinci.core.config.SpringContextHolder;
import edp.davinci.core.enums.ActionEnum;
import edp.davinci.core.enums.FileTypeEnum;
import edp.davinci.core.model.ExcelHeader;
import edp.davinci.core.utils.ExcelUtils;
import edp.davinci.core.utils.FileCloudUtils;
import edp.davinci.dao.ViewMapper;
import edp.davinci.dto.cronJobDto.MsgMailExcel;
import edp.davinci.dto.viewDto.Aggregator;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.dto.viewDto.ViewWithProjectAndSource;
import edp.davinci.service.ViewService;
import edp.system.util.SimpleDownloadConLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author daemon
 * @Date 19/5/29 19:29
 * To change this template use File | Settings | File Templates.
 */
@Slf4j
public class WorkbookWorker<T> extends MsgNotifier implements Callable {

    @Value("${file.userfiles-path}")
    public String fileBasePath;

    private WorkBookContext context;


    public WorkbookWorker(WorkBookContext context) {
        this.context = context;
    }


    @Override
    public T call() throws Exception {
        Stopwatch watch = Stopwatch.createStarted();
        Workbook wb = null;

        MsgWrapper wrapper = context.getWrapper();
        Object[] logArgs = {context.getTaskKey(), wrapper.getAction(), wrapper.getxId()};
        log.info("workbook worker start: taksKey={}, action={}, xid={}", logArgs);
        if (context.getCustomLogger() != null) {
            context.getCustomLogger().info("workbook worker start: taksKey={}, action={}, xid={}", logArgs);
        }

        String filePath = null;
        try {
            List<SheetContext> sheetContextList = buildSheetContextList();
            if (CollectionUtils.isEmpty(sheetContextList)) {
                throw new IllegalArgumentException("sheetContextList is empty");
            }
            if (null == context.getTemplate()) {
                wb = new SXSSFWorkbook(1000);
            } else {
                wb = new SXSSFWorkbook(new XSSFWorkbook(context.getTemplate()), 1000);
            }

            List<Future> futures = Lists.newArrayList();
            int sheetNo = 0;
            for (SheetContext sheetContext : sheetContextList) {
                sheetNo++;
                // 对sheetname格式化，防止出现非法字符串
                String safeSheetName = WorkbookUtil.createSafeSheetName(sheetContext.getName());
                Sheet sheet = wb.createSheet(safeSheetName);
                sheetContext.setSheet(sheet);
                sheetContext.setWorkbook(wb);
                sheetContext.setSheetNo(sheetNo);
                sheetContext.setShowMode(context.getWidgets().get(sheetNo-1).getShowMode());

                if(context.getWidgets().get(sheetNo-1).getShowMode()==2) {
                    ViewExecuteParam executeParam =  context.getWidgets().get(sheetNo-1).getExecuteParam();
                    List<String> filters  = executeParam.getFilters();
                    sheetContext.setGroupsContext(
                            new GroupsContext(executeParam.getColGroups(), executeParam.getRowGroups(),
                                    executeParam.getColOrders(), executeParam.getRowOrders(),
                                    executeParam.getAggregators().get(0))
                    );
                }
                Future<Boolean> future = ExecutorUtil.submitSheetTask(sheetContext, context.getCustomLogger());
                futures.add(future);
            }
            Boolean rst = false;

            try {
                for (Future<Boolean> future : futures) {
                    rst = future.get(10, TimeUnit.MINUTES);
                    if (!rst) {
                        future.cancel(true);
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                log.error("workbook worker error, task={}, e={}", context.getTaskKey(), e.getMessage());
                if (context.getCustomLogger() != null) {
                    context.getCustomLogger().error("workbook worker error, task={}, e={}", context.getTaskKey(), e.getMessage());
                }
            } catch (TimeoutException e) {
                e.printStackTrace();
                log.error("workbook worker error, task={} timeout, e={}", context.getTaskKey(), e.getMessage());
                if (context.getCustomLogger() != null) {
                    context.getCustomLogger().error("workbook worker error, task={} timeout, e={}", context.getTaskKey(), e.getMessage());
                }
                if (wrapper.getAction() == ActionEnum.MAIL) {
                    MsgMailExcel msg = (MsgMailExcel) wrapper.getMsg();
                    msg.setException(new TimeoutException("Get data timeout"));
                    super.tell(wrapper);
                }
                return (T) filePath;
            }

            if (rst) {
                // 设置水印
                if (StringUtils.isNotEmpty(context.getWaterMarkContext()) && null == context.getTemplate()) {
                    String watermarkPath = ((FileUtils) SpringContextHolder.getBean(FileUtils.class)).fileBasePath + "download/" + UUID.randomUUID() + ".png";
                    ExcelUtils.setExcelBackground(((SXSSFWorkbook) wb).getXSSFWorkbook(), context.getWaterMarkContext(), watermarkPath, null);
                }
                FileTypeEnum typeEnum = null;
                if (null == context.getTemplate()) {
                    typeEnum = FileTypeEnum.XLSX;
                } else {
                    typeEnum = FileTypeEnum.XLSM;
                }

//
                FileOutputStream out = null;
                ByteArrayOutputStream bos = null;

                try {
                    if (context.isLocalPath()) {
                        filePath = ((FileUtils) SpringContextHolder.getBean(FileUtils.class)).getFilePath(typeEnum, this.context.getWrapper());
                        System.out.println("filepath === " + filePath);
                        out = new FileOutputStream(filePath);
                        if (wb == null) {
                            System.out.println("null is === wb");
                        }
                        if (out == null) {
                            System.out.println("null is === out");
                        }
                        wb.write(out);
                        out.flush();
                        out.close();
                    } else {
                        bos = new ByteArrayOutputStream();
                        wb.write(bos);
                        filePath = ((FileCloudUtils) SpringContextHolder.getBean(FileCloudUtils.class)).uploadFile(
                                "file", UUID.randomUUID().toString() + typeEnum.getFormat(),
                                new ByteArrayInputStream(bos.toByteArray()));
                    }
                } catch (Exception e) {
                    filePath = null;
                    throw e;
                } finally {
                    if (bos != null) {
                        bos.flush();
                        bos.close();
                    }
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                }
                wrapper.setRst(filePath);
            } else {
                wrapper.setRst(null);
            }
            super.tell(wrapper);
        } catch (Exception e) {
            log.error("workbook worker error, task={}, e={}", context.getTaskKey(), e.getMessage());
            log.error(e.getMessage(),e);
            if (context.getCustomLogger() != null) {
                context.getCustomLogger().error("workbook worker error, task={}, e={}", context.getTaskKey(), e.getMessage());
            }
            if (wrapper.getAction() == ActionEnum.MAIL) {
                MsgMailExcel msg = (MsgMailExcel) wrapper.getMsg();
                msg.setException(e);
            }
            super.tell(wrapper);
            if (StringUtils.isNotEmpty(filePath)) {
                FileUtils.delete(filePath);
            }
        } finally {
            if (null != wb) {
                wb.close();
            }
            SimpleDownloadConLimiter.INSTANCE.release();
        }
        if (wrapper.getAction() == ActionEnum.DOWNLOAD) {
            Object[] args = {context.getTaskKey(), StringUtils.isNotEmpty(filePath), wrapper.getAction(), wrapper.getxId(), filePath, watch.elapsed(TimeUnit.MILLISECONDS)};
            log.info("workbook worker complete task={}, status={},action={},xid={},filePath={},cost={}ms", args);
            if (context.getCustomLogger() != null) {
                context.getCustomLogger().info("workbook worker complete task={}, status={},action={},xid={},filePath={},cost={}ms", args);
            }
        } else if (wrapper.getAction() == ActionEnum.SHAREDOWNLOAD || wrapper.getAction() == ActionEnum.MAIL) {
            Object[] args = {context.getTaskKey(), StringUtils.isNotEmpty(filePath), wrapper.getAction(), wrapper.getxUUID(), filePath, watch.elapsed(TimeUnit.MILLISECONDS)};
            log.info("workbook worker complete task={}, status={},action={},xUUID={},filePath={},cost={}ms", args);
            if (context.getCustomLogger() != null) {
                context.getCustomLogger().info("workbook worker complete task={}, status={},action={},xUUID={},filePath={},cost={}ms", args);
            }
        }

        return (T) filePath;
    }


    private List<SheetContext> buildSheetContextList() throws Exception {
        List<SheetContext> sheetContextList = Lists.newArrayList();
        for (WidgetContext context : context.getWidgets()) {

            ViewExecuteParam executeParam;
            if (context.isHasExecuteParam() && null != context.getExecuteParam()) {
                executeParam = context.getExecuteParam();
            } else {
                executeParam = ScriptUtiils.getViewExecuteParam(ScriptUtiils.getExecuptParamScriptEngine(),
                        context.getDashboard() != null ? context.getDashboard().getConfig() : null,
                        context.getWidget().getConfig(),
                        context.getMemDashboardWidget() != null ? context.getMemDashboardWidget().getId() : null);
            }

            ViewWithProjectAndSource viewWithProjectAndSource = ((ViewMapper) SpringContextHolder.getBean(ViewMapper.class)).getViewWithProjectAndSourceById(context.getWidget().getViewId());

            //查询行维度的sql
            String colSql = null;
            //查询列维度的sql
            String rowSql = null;
            //指标的key
            String metricKey = null;
            if(context.getShowMode()==2) {
                Pair<String, String> pivotGroupSqlPair = ((ViewService) SpringContextHolder.getBean(ViewService.class))
                        .getPivotGroupSqlContext(context.getIsMaintainer(), viewWithProjectAndSource, executeParam, this.context.getUser());
                colSql = pivotGroupSqlPair.getLeft();
                rowSql = pivotGroupSqlPair.getRight();
                Aggregator aggregator = executeParam.getAggregators().get(0);
                //获取指标的key
                metricKey = executeParam.getMetricKey(aggregator.getColumn(), aggregator.getFunc(),
                        aggregator.getCalculateRules(), viewWithProjectAndSource.getSource().getJdbcUrl(),
                        viewWithProjectAndSource.getSource().getDbVersion(), false);
            }


            SQLContext sqlContext = ((ViewService) SpringContextHolder.getBean(ViewService.class)).getSQLContext(context.getIsMaintainer(), viewWithProjectAndSource, executeParam, this.context.getUser());
            //如果sqlContext为空，则跳过，主要解决在看板中，设置空白widget的问题
            if (sqlContext == null) {
                continue;
            }
            SqlUtils sqlUtils = ((SqlUtils) SpringContextHolder.getBean(SqlUtils.class)).init(viewWithProjectAndSource.getSource());
            List<ExcelHeader> excelHeaders = ScriptUtiils.formatHeader(ScriptUtiils.getCellValueScriptEngine(),
                    context.getWidget().getConfig(), sqlContext.getViewExecuteParam().getParams());
            SheetContext sheetContext = SheetContext.SheetContextBuilder.newBuilder()
                    .withExecuteSql(sqlContext.getExecuteSql())
                    .withQuerySql(sqlContext.getQuerySql())
                    .withColSql(colSql)
                    .withRowSql(rowSql)
                    .withMetricKey(metricKey)
                    .withExcludeColumns(sqlContext.getExcludeColumns())
                    .withContain(Boolean.FALSE)
                    .withSqlUtils(sqlUtils)
                    .withIsTable(true)
                    .withExcelHeaders(excelHeaders)
                    .withDashboardId(null != context.getDashboard() ? context.getDashboard().getId() : null)
                    .withWidgetId(context.getWidget().getId())
                    .withName(context.getWidget().getName())
                    .withWrapper(this.context.getWrapper())
                    .withResultLimit(this.context.getResultLimit())
                    .withTaskKey(this.context.getTaskKey())
                    .withCustomLogger(this.context.getCustomLogger())
                    .withHasTemplate(null != this.context.getTemplate())
                    .build();
            sheetContextList.add(sheetContext);
        }
        return sheetContextList;
    }
}
