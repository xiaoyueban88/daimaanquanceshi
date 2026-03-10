package edp.davinci.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.iflytek.edu.elp.common.dto.page.Pager;
import com.iflytek.edu.zx.etl.dto.ElPriorityConfigDto;
import com.iflytek.edu.zx.etl.model.ColumnBasicInfo;
import com.iflytek.edu.zx.etl.model.ElTask;
import com.iflytek.edu.zx.etl.model.Step;
import com.iflytek.edu.zx.etl.model.odeon.TableColumnModel;
import com.iflytek.edu.zx.etl.model.odeon.TableMetaInfoModel;
import com.iflytek.edu.zx.etl.service.*;

import edp.core.annotation.CurrentUser;
import edp.davinci.common.controller.BaseController;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.dto.DataTransDto.DataTransParamDto;
import edp.davinci.dto.DataTransDto.ElTaskLogQueryParams;
import edp.davinci.dto.DataTransDto.ProcessDeployParams;
import edp.davinci.dto.DataTransDto.ProcessDetailDto;
import edp.davinci.dto.DataTransDto.ProcessUpdateDto;
import edp.davinci.dto.DataTransDto.ProcessWithLatestStatusDto;
import edp.davinci.model.User;
import edp.davinci.service.DataTransService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(value = "/dataTransController", tags = "dataTrans", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "project not found"))
@Slf4j
@RestController
@RequestMapping(value = Constants.BASE_API_PATH + "/dataTrans", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class DataTransController extends BaseController {

    @Autowired
    private OdeonApiService odeonApiService;

    @Autowired
    private ColumnMappingService columnMappingService;

    @Autowired
    private ClickHouseHttpService clickHouseHttpService;

    @Autowired
    private StepService stepService;

    @Autowired
    private DataTransService dataTransService;

    @Resource
    private ElTaskService elTaskService;

    @Resource
    private ElPriorityConfigService elPriorityConfigService;

    /**
     * 获取hive表
     *
     * @param projectName 项目名称
     * @param userName    用户名
     * @param tableName   表名关键字
     * @param request
     * @return
     */
    @ApiOperation(value = "get hive tables")
    @GetMapping("/getHiveTables")
    public ResponseEntity getProjectTables(@RequestParam String projectName,
                                           @RequestParam String userName,
                                           @RequestParam String tableName,
                                           HttpServletRequest request) {
        List<String> tableList = odeonApiService.getHiveTablesWithMetas(projectName, userName, tableName, 0, 100);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payloads(tableList));
    }

    @ApiOperation(value = "get table columns")
    @GetMapping("/getTableColumn")
    public ResponseEntity getTableColumns(@RequestParam String projectName,
                                          @RequestParam String descDataBase,
                                          @RequestParam String userName,
                                          @RequestParam String tableName,
                                          HttpServletRequest request) {
        List<TableColumnModel> tableColumnList = odeonApiService.getHiveTableColumns(projectName, userName, tableName);
        List<ColumnBasicInfo> destColumnList = null;

        List<ColumnBasicInfo> odeonColumnBasicInfo = new ArrayList<ColumnBasicInfo>();
        for (TableColumnModel tableColumnModel : tableColumnList) {
            odeonColumnBasicInfo.add(new ColumnBasicInfo(tableColumnModel.getColumn(), tableColumnModel.getType()));
        }
        if ("clickhouse".equalsIgnoreCase(descDataBase)) {
            destColumnList = columnMappingService.hive2CH(odeonColumnBasicInfo);
        }
        List<Step> steps = stepService.selectAll();
        Integer stepId = null;
        for (Step step : steps) {
            if ("txt2clickhouse".equals(step.getName())) {
                stepId = step.getId();
                break;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("odeon_table", odeonColumnBasicInfo);
        result.put("dest_table", destColumnList);
        result.put("step_id", stepId);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(result));
    }

    @ApiOperation(value = "get table metainfo")
    @GetMapping("/getTableMetaInfo")
    public ResponseEntity getTableMetaInfo(@RequestParam String projectName,
                                           @RequestParam String userName,
                                           @RequestParam String tableName,
                                           HttpServletRequest request) {
        TableMetaInfoModel tableList = odeonApiService.getHiveTableMetadata(projectName, userName, tableName);
        return ResponseEntity.ok(tableList);
    }

    @ApiOperation(value = "get column mapping")
    @GetMapping("/getColumnMapping")
    public ResponseEntity getColumnMapping(@RequestParam String hiveColumns,
                                           @RequestParam String descDataBase,
                                           HttpServletRequest request) {
        List<ColumnBasicInfo> hiveColumnList = JSON.parseArray(hiveColumns, ColumnBasicInfo.class);
        List<ColumnBasicInfo> destColumnList = null;
        if ("clickhosue".equalsIgnoreCase(descDataBase)) {
            destColumnList = columnMappingService.hive2CH(hiveColumnList);
        }

        return ResponseEntity.ok(destColumnList);
    }

    @ApiOperation(value = "get dest db column type list")
    @GetMapping("/getColumnTypeList")
    public ResponseEntity getColumnMapping(@RequestParam String destDataBase,
                                           HttpServletRequest request) {
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payloads(columnMappingService.getDBColumnTypeList(destDataBase)));
    }

    @ApiOperation(value = "mk ch table")
    @PostMapping("/mkCHTable")
    public ResponseEntity mkCHTable(@Valid @RequestBody DataTransParamDto dataTransParamDto,
                                    HttpServletRequest request) {
        List<ColumnBasicInfo> chColumnsBasicInfo = dataTransParamDto.getChColumnInfo();
        String dbName = dataTransParamDto.getDbName();
        String tableName = dataTransParamDto.getTableName();
        String partition = dataTransParamDto.getPartition();
        String orderBy = dataTransParamDto.getOrderColumns();
        String createLocalRes = clickHouseHttpService.createTable(dbName, tableName, chColumnsBasicInfo, partition, orderBy, true, false);
        log.info("createLocalRes:" + createLocalRes);
        String createRouteLocalRes = "ok";
        if (!dataTransParamDto.getIsIncr()) {
            createRouteLocalRes = clickHouseHttpService.createTable(dbName, tableName + "_ck_route", chColumnsBasicInfo, partition, orderBy, true, false);
            log.info("createRouteLocalRes:" + createRouteLocalRes);
        }
        String createDistriRes = null;
        if ("ok".equalsIgnoreCase(createLocalRes) && "ok".equals(createRouteLocalRes)) {
            createDistriRes = clickHouseHttpService.createTable(dbName, tableName, chColumnsBasicInfo, partition, orderBy, true, true);
            if (!dataTransParamDto.getIsIncr()) {
                createDistriRes = clickHouseHttpService.createTable(dbName, tableName + "_ck_route", chColumnsBasicInfo, partition, orderBy, true, true);
            }
            log.info("createDistriRes:" + createDistriRes);
        }
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(createDistriRes));
    }

    @ApiOperation(value = "get etl process into")
    @RequestMapping("/process/pager")
    public ResponseEntity getPagerProcessWithLatestStatus(@RequestParam String processName,
                                                          @RequestParam int pageIndex,
                                                          @RequestParam int pageSize,
                                                          HttpServletRequest request) {
        Pager<ProcessWithLatestStatusDto> pagerProcessWithLatestStatus = dataTransService.getPagerProcessWithLatestStatus(processName, pageIndex, pageSize);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(pagerProcessWithLatestStatus));
    }

    @ApiOperation(value = "get process detail")
    @RequestMapping("/process/detail")
    public ResponseEntity getProcessDetail(@RequestParam String processId,
                                           HttpServletRequest request) {
        ProcessDetailDto processDetail = dataTransService.getProcessDetail(processId);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(processDetail));
    }

    @ApiOperation(value = "update process")
    @PutMapping("/process/update")
    public ResponseEntity updateProcess(@Valid @RequestBody ProcessUpdateDto processUpdate,
                                        @ApiIgnore BindingResult bindingResult,
                                        HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        dataTransService.updateProcess(processUpdate);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    @ApiOperation(value = "delete process")
    @DeleteMapping("/process/delete")
    public ResponseEntity deleteProcess(@RequestParam String processId,
                                        HttpServletRequest request) {
        try {
            dataTransService.deleteProcess(processId);
            return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("delete process error");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
    }

    @ApiOperation(value = "get pagerlog")
    @RequestMapping("/log/pager")
    public ResponseEntity getPagerLog(@Valid @RequestBody ElTaskLogQueryParams params,
                                      @ApiIgnore BindingResult bindingResult,
                                      HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        try {
            Pager<ElTask> pagerLog = dataTransService.getPagerLog(params);
            return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(pagerLog));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("get log error");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
    }

    @ApiOperation(value = "delete log info")
    @DeleteMapping("/log/deleteInfo")
    public ResponseEntity deleteInfo(@RequestParam Integer taskId,
                                     HttpServletRequest request) {
        try {
            elTaskService.deleteInfo(taskId);
            return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("delete log info error");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
    }

    @ApiOperation(value = "task retry")
    @RequestMapping("/job/taskRetry")
    public ResponseEntity taskRetry(@RequestParam String processId,
                                    @RequestParam String dateParts,
                                    @RequestParam String params,
                                    HttpServletRequest request) {
        try {
            dataTransService.taskRetry(processId, dateParts, params);
            return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("taskRetry error");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
    }

    @ApiOperation(value = "deploy process")
    @PostMapping("/process/deploy")
    public ResponseEntity deploy(@Valid @RequestBody ProcessDeployParams params,
                                 @ApiIgnore BindingResult bindingResult,
                                 @ApiIgnore @CurrentUser User user,
                                 HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        dataTransService.deploy(params, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    @ApiOperation(value = "get priority config")
    @GetMapping("/priority/config")
    public ResponseEntity listPriorityConfig(HttpServletRequest request) {
        List<ElPriorityConfigDto> priorityConfigDtos = elPriorityConfigService.batchAll();
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payloads(priorityConfigDtos));
    }
}