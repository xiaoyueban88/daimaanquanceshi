package edp.davinci.service.excel;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.iflytek.edu.elp.common.util.JSONUtils;
import edp.core.consts.Consts;
import edp.core.utils.FileUtils;
import edp.davinci.core.config.SpringContextHolder;
import edp.davinci.core.enums.DownloadType;
import edp.davinci.core.enums.FileTypeEnum;
import edp.davinci.core.model.SqlFilter;
import edp.davinci.core.utils.FileCloudUtils;
import edp.davinci.model.DashBoardConfig;
import edp.davinci.model.DashBoardConfigFilter;
import edp.davinci.service.DashboardDownloadTemplateService;
import edp.system.util.SimpleDownloadConLimiter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class WorkbookBatchTaskWorker<T> extends MsgNotifier implements Callable {

    private List<WorkBookContext> workBookContexts;

    private MsgWrapper wrapper;

    private DownloadConfig downloadConfig;

    private Set<String> fileNames = new HashSet<>();

    private DownloadType type;

    private Long id;

    private String downloadFileName;

    public WorkbookBatchTaskWorker(List<WorkBookContext> workBookContexts, MsgWrapper wrapper, DownloadConfig downloadConfig, DownloadType type, Long id, String downloadFileName) {
        this.workBookContexts = workBookContexts;
        this.wrapper = wrapper;
        this.downloadConfig = downloadConfig;
        this.type = type;
        this.id = id;
        this.downloadFileName = downloadFileName;
    }

    @Override
    public T call() throws Exception {
        log.info("WorkbookBatchTask worker start: id={}, 任务数量={}", id, workBookContexts.size());
        int completeTaskSum = 0;
        String filePath = null;
        ByteArrayOutputStream template = null;
        ByteArrayOutputStream out = null;
        ZipOutputStream zipOut = null;
        String zipFoldPath = null;
        try {
            zipFoldPath = ((FileUtils) SpringContextHolder.getBean(FileUtils.class)).getDirPath(FileTypeEnum.ZIP, wrapper);
            // 如果type是dashboard,需要查询dashboard是否有模板
            if (DownloadType.DashBoard == type) {
                template = getTemplate();
            }
            List<Future<Map>> futures = Lists.newArrayList();
            for (WorkBookContext context : workBookContexts) {
                if (null != template) {
                    context.setTemplate(new ByteArrayInputStream(template.toByteArray()));
                }
                context.getWrapper().setFolderPath(zipFoldPath);
                Future<Map> future = ExecutorUtil.submitWorkbookBatchTask(context, null);
                futures.add(future);
            }
            out = new ByteArrayOutputStream();
            zipOut = new ZipOutputStream(out);

            for (Future<Map> future : futures) {
                WorkBookContext context = null;
                InputStream is = null;
                try {
                    Map<String, Object> map = future.get(1, TimeUnit.HOURS);
                    context = (WorkBookContext) map.get("context");
                    Object file = map.get("filePath");
                    if (null == file) {
                        future.cancel(true);
                    } else {
                        FileTypeEnum typeEnum = null;
                        if (null == context.getTemplate()) {
                            typeEnum = FileTypeEnum.XLSX;
                        } else {
                            typeEnum = FileTypeEnum.XLSM;
                        }
                        String fileName = getFileName(context);
                        ZipEntry entry = new ZipEntry(fileName + typeEnum.getFormat());
                        zipOut.putNextEntry(entry);
                        //
                        is = new FileInputStream(new File(file.toString()));
                        IOUtils.copy(is, zipOut);
                        // 关闭输入流
                        zipOut.closeEntry();
                    }
                    log.info("WorkbookBatchTask worker doing: id={}, 完成任务数量={}", id, ++completeTaskSum);
                } catch (InterruptedException | ExecutionException e) {
                    log.error("WorkbookBatchTask worker error, task={}, e={}", null != context ? context.getTaskKey() : null, e.getMessage());
                } catch (TimeoutException e) {
                    log.error("WorkbookBatchTask worker error, task={} timeout, e={}", null != context ? context.getTaskKey() : null, e.getMessage());
                } catch (Exception e) {
                    log.error("WorkbookBatchTask worker error, task={} exception, e={}", null != context ? context.getTaskKey() : null, e.getMessage());
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
            // 先关闭zipOut输出流,否则上传的压缩文件会有损坏
            IOUtils.closeQuietly(zipOut);
            filePath = ((FileCloudUtils) SpringContextHolder.getBean(FileCloudUtils.class)).uploadFile(
                "file", downloadFileName + FileTypeEnum.ZIP.getFormat(),
                new ByteArrayInputStream(out.toByteArray()));
            wrapper.setRst(filePath);
            super.tell(wrapper);
        } catch (Exception e) {
            log.error("WorkbookBatchTask worker error, wrapper={}, e={}", wrapper, e);
            super.tell(wrapper);
        } finally {
            IOUtils.closeQuietly(zipOut);
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(template);
            if (null != zipFoldPath) {
                // 删除文件夹
                ((FileUtils) SpringContextHolder.getBean(FileUtils.class)).deleteDir(new File(zipFoldPath));
            }
            // 文件生成，释放资源
            SimpleDownloadConLimiter.INSTANCE.release();
        }
        return (T) filePath;
    }

    private ByteArrayOutputStream getTemplate() throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = ((DashboardDownloadTemplateService) SpringContextHolder.getBean(DashboardDownloadTemplateService.class)).getDownloadTemplate(id);
            if (null != inputStream) {
                ByteArrayOutputStream template = new ByteArrayOutputStream();
                IOUtils.copy(inputStream, template);
                return template;
            }
        } catch (Exception e) {
            log.error("获取模板异常," + id);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return null;
    }

    private String getFileName(WorkBookContext context) {
        List<SqlFilter> distinctFilters = new ArrayList<>(context.getWidgets().stream()
                .filter(widget -> null != widget.getExecuteParam() && null != widget.getExecuteParam().getFilters())
                .flatMap(widget -> widget.getExecuteParam().getFilters().stream().map(filter -> JSONUtils.parseObject(filter, SqlFilter.class)))
                .collect(Collectors.toMap(sqlFilter -> getKey(sqlFilter), o -> o, (o1, o2) -> o1))
                .values());

        String nameTemplate = downloadConfig.getNameTemplate();
        String result = nameTemplate;
        DashBoardConfig dashBoardConfig = JSONObject.parseObject(context.getWidgets().get(0).getDashboard().getConfig(), DashBoardConfig.class);
        for (DashBoardConfigFilter filter : dashBoardConfig.getFilters()) {
            SqlFilter sqlFilter = distinctFilters.stream().filter(f -> ObjectUtils.equals(getKey(f), filter.getKey())).findFirst().orElse(null);
            if (null != sqlFilter) {
                Object value = sqlFilter.getValue();
                String name;
                if (value instanceof List<?>) {
                    name = ((List) value).get(0).toString();
                } else {
                    name = value.toString();
                }
                if (name.startsWith(Consts.APOSTROPHE) && name.endsWith(Consts.APOSTROPHE)) {
                    name = name.substring(1, name.length() - 1);
                }
                result = result.replace("${" + filter.getKey() + "}", name);
            }
        }
        if (fileNames.add(result)) {
            return result;
        } else { // 文件名冲突处理
            for (int index = 1; index < 100; ++index) {
                if (!fileNames.contains(result + index)) {
                    result = result + index;
                    fileNames.add(result);
                    return result;
                }
            }
            return result + "_" + UUID.randomUUID().toString();
        }
    }


    private String getKey(SqlFilter sqlFilter) {
        return sqlFilter.getKey();
    }

}
