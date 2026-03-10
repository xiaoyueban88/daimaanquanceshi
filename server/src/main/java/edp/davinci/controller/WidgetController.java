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

package edp.davinci.controller;


import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.github.pagehelper.PageInfo;

import edp.core.annotation.CurrentUser;
import edp.davinci.common.controller.BaseController;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.dto.viewDto.ViewExecuteParam;
import edp.davinci.dto.widgetDto.WidgetCreate;
import edp.davinci.dto.widgetDto.WidgetQueryDto;
import edp.davinci.dto.widgetDto.WidgetUpdate;
import edp.davinci.model.UploadIcon;
import edp.davinci.model.User;
import edp.davinci.model.Widget;
import edp.davinci.service.DeployService;
import edp.davinci.service.UploadIconService;
import edp.davinci.service.WidgetContainerService;
import edp.davinci.service.WidgetService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

@Api(value = "/widgets", tags = "widgets", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "widget not found"))
@Slf4j
@RestController
@RequestMapping(value = Constants.BASE_API_PATH + "/widgets", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class WidgetController extends BaseController {

    @Autowired
    private WidgetService widgetService;

    @Autowired
    private WidgetContainerService widgetContainerService;

    @Autowired
    private DeployService deployService;

    @Autowired
    private UploadIconService uploadIconService;

    /**
     * 获取widget列表
     *
     * @param projectId 项目id
     * @param folderId  widget目录id
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "get widgets")
    @GetMapping
    public ResponseEntity getWidgets(@RequestParam Long projectId,
                                     @RequestParam(required = false) Long folderId,
                                     @ApiIgnore @CurrentUser User user,
                                     HttpServletRequest request) {
        if (invalidId(projectId)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid project id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        List<Widget> widgetList = widgetService.getWidgetList(projectId, folderId, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payloads(widgetList));
    }

    @ApiOperation(value = "get widgets by dashboardId")
    @GetMapping("/getDashboardWidget/{dashboardId}")
    public ResponseEntity getWidgetsByDashboard(@PathVariable Long dashboardId,
                                                @ApiIgnore @CurrentUser User user,
                                                HttpServletRequest request) {
        if (invalidId(dashboardId)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid project id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        List<Widget> dashboardWidgets = widgetService.getDashboardWidgets(dashboardId, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payloads(dashboardWidgets));
}

    @ApiOperation(value = "get pager widgets by projectId")
    @PostMapping("/getPagerWidgetByProject/{projectId}")
    public ResponseEntity getPagerWidget(@PathVariable Long projectId,
                                         @RequestBody WidgetQueryDto widgetQueryDto,
                                         @ApiIgnore @CurrentUser User user,
                                         HttpServletRequest request) {
        if (invalidId(projectId)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid project id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        PageInfo<Widget> pagerWidgets = widgetService.getPagerWidgets(projectId, widgetQueryDto, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(pagerWidgets));
    }

    /**
     * 获取widget信息
     *
     * @param id
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "get widget info")
    @GetMapping("/{id}")
    public ResponseEntity getWidgetInfo(@PathVariable Long id,
                                        @ApiIgnore @CurrentUser User user,
                                        HttpServletRequest request) {
        if (invalidId(id)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        Widget widget = widgetService.getWidget(id, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(widget));
    }


    /**
     * 新建widget
     *
     * @param widget
     * @param bindingResult
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "create widget")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createWidgets(@Valid @RequestBody WidgetCreate widget,
                                        @ApiIgnore BindingResult bindingResult,
                                        @ApiIgnore @CurrentUser User user,
                                        HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        Widget newWidget = widgetService.createWidget(widget, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(newWidget));
    }


    /**
     * 修改widget
     *
     * @param id
     * @param widget
     * @param bindingResult
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "update widget")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity updateWidget(@PathVariable Long id,
                                       @Valid @RequestBody WidgetUpdate widget,
                                       @ApiIgnore BindingResult bindingResult,
                                       @ApiIgnore @CurrentUser User user,
                                       HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if (invalidId(id) || !id.equals(widget.getId())) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        widgetService.updateWidget(widget, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    /**
     *
     * @param widgets
     * @param bindingResult
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "update widget config")
    @PutMapping(value = "/batchUpdateConfig", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity batchUpdateWidget(@Valid @RequestBody List<WidgetUpdate> widgets,
                                            @ApiIgnore BindingResult bindingResult,
                                            @ApiIgnore @CurrentUser User user,
                                            HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        widgetService.batchUpdateConfig(widgets, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }


    /**
     * 删除widget
     *
     * @param id
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "delete widget")
    @DeleteMapping("/{id}")
    public ResponseEntity deleteWidget(@PathVariable Long id,
                                       @ApiIgnore @CurrentUser User user,
                                       HttpServletRequest request) {

        if (invalidId(id)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        widgetService.deleteWidget(id, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }


    /**
     * 下载widget
     *
     * @param id
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "download widget")
    @PostMapping("/{id}/{type}")
    public ResponseEntity downloadWidget(@PathVariable("id") Long id,
                                         @PathVariable("type") String type,
                                         @Valid @RequestBody ViewExecuteParam executeParam,
                                         @ApiIgnore BindingResult bindingResult,
                                         @ApiIgnore @CurrentUser User user,
                                         HttpServletRequest request) {
        if (invalidId(id)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        String filePath = widgetService.generationFile(id, executeParam, user, type);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(filePath));
    }


    /**
     * 分享widget
     *
     * @param id
     * @param clientId
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "share widget")
    @GetMapping("/{id}/share")
    public ResponseEntity shareWidget(@PathVariable Long id,
                                      @RequestParam(required = false) String clientId,
                                      @ApiIgnore @CurrentUser User user,
                                      HttpServletRequest request) {
        if (invalidId(id)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        String shareToken = widgetService.shareWidget(id, user, clientId);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(shareToken));
    }

    /**
     * 修改并发布widget
     *
     * @param id
     * @param widget
     * @param bindingResult
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "update and publish widget")
    @PutMapping("/{id}/publish")
    public ResponseEntity updateAndDeployWidget(@PathVariable Long id,
                                                @Valid @RequestBody WidgetUpdate widget,
                                                @ApiIgnore BindingResult bindingResult,
                                                @ApiIgnore @CurrentUser User user,
                                                HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if (invalidId(id) || !id.equals(widget.getId())) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        //修改widget
        widgetService.updateWidget(widget, user);
        //发布widget
        deployService.deployWidget(id, user);

        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    @ApiOperation(value = "update and publish widget")
    @PostMapping("/publish")
    public ResponseEntity createAndDeployWidget(@Valid @RequestBody WidgetCreate widget,
                                                @ApiIgnore BindingResult bindingResult,
                                                @ApiIgnore @CurrentUser User user,
                                                HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        //新建widget
        Widget newWidget = widgetService.createWidget(widget, user);

        //发布widget
        deployService.deployWidget(newWidget.getId(), user);

        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    @ApiOperation(value = "move widget")
    @PutMapping("/{id}/move/{folderId}")
    public ResponseEntity changeFolder(@PathVariable("id") Long id,
                                       @PathVariable("folderId") Long folderId,
                                       @ApiIgnore @CurrentUser User user,
                                       HttpServletRequest request) {
        widgetService.changeFolder(id, folderId, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    @ApiOperation(value = "upload icon")
    @PostMapping("/uploadIcon")
    public ResponseEntity uploadIcon(@RequestParam("file") MultipartFile file,
                                     @RequestParam("name") String name,
                                     @ApiIgnore @CurrentUser User user,
                                     HttpServletRequest request) throws IOException {
        uploadIconService.upload(name, file.getInputStream(), user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    @ApiOperation(value = "get all upload icon")
    @GetMapping("/getUploadIcons")
    public ResponseEntity getAllIcon(@ApiIgnore @CurrentUser User user,
                                     HttpServletRequest request) {
        List<UploadIcon> allIcons = uploadIconService.getAllIcons();
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payloads(allIcons));
    }
}
