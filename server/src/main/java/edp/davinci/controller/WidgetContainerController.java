package edp.davinci.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import edp.core.annotation.CurrentUser;
import edp.davinci.common.controller.BaseController;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.dto.WidgetContainer.WidgetContainerCreate;
import edp.davinci.dto.WidgetContainer.WidgetContainerUpdate;
import edp.davinci.model.User;
import edp.davinci.model.WidgetContainer;
import edp.davinci.service.DeployService;
import edp.davinci.service.WidgetContainerService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/2/28
 */
@Api(value = "/widgetcontainer", tags = "widgetcontainer", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "widgetcontainer not found"))
@Slf4j
@RestController
@RequestMapping(value = Constants.BASE_API_PATH + "/widgetcontainer", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class WidgetContainerController extends BaseController {

    @Autowired
    private WidgetContainerService widgetContainerService;

    @Autowired
    private DeployService deployService;


    /**
     * 新建widgetcontainer
     *
     * @param widgetContainer
     * @param bindingResult
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "create widgetcontainer")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createWidgetContainer(@Valid @RequestBody WidgetContainerCreate widgetContainer,
                                                @ApiIgnore BindingResult bindingResult,
                                                @ApiIgnore @CurrentUser User user,
                                                HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        widgetContainerService.createWidgetContainer(widgetContainer, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));

    }

    /**
     * 修改widgetcontainer
     *
     * @param id
     * @param widgetContainer
     * @param bindingResult
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "update widgetcontiner")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity updateWidgetContainer(@PathVariable Long id,
                                                @Valid @RequestBody WidgetContainerUpdate widgetContainer,
                                                @ApiIgnore BindingResult bindingResult,
                                                @ApiIgnore @CurrentUser User user,
                                                HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if (invalidId(id) || !id.equals(widgetContainer.getId())) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        widgetContainerService.updateWidgetContainer(widgetContainer, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }


    /**
     *
     * @param widgetContainers
     * @param bindingResult
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "batch update widgetcontiner dispose")
    @PutMapping(value = "/batchUpdateDispose", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity updateWidgetContainer(@Valid @RequestBody List<WidgetContainerUpdate> widgetContainers,
                                                @ApiIgnore BindingResult bindingResult,
                                                @ApiIgnore @CurrentUser User user,
                                                HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        widgetContainerService.batchUpdateWidgetContainerDispose(widgetContainers, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    /**
     * 删除widgetcontainer
     *
     * @param id
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "delete widget")
    @DeleteMapping("/{id}")
    public ResponseEntity deleteWidgetContainer(@PathVariable Long id,
                                                @ApiIgnore @CurrentUser User user,
                                                HttpServletRequest request) {
        if (invalidId(id)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        widgetContainerService.deleteWidgetContainer(id, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    /**
     * create and publish widgetcontainer
     *
     * @param widgetContainer
     * @param bindingResult
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "create and publish widgetcontainer")
    @PostMapping("/publish")
    public ResponseEntity createAndDeployWidgetContainer(@Valid @RequestBody WidgetContainerCreate widgetContainer,
                                                         @ApiIgnore BindingResult bindingResult,
                                                         @ApiIgnore @CurrentUser User user,
                                                         HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        WidgetContainer newWidgetContainer = widgetContainerService.createWidgetContainer(widgetContainer, user);
        deployService.deployWidgetContainer(newWidgetContainer.getId(), user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }


    /**
     * update and publish widgetcontainer
     *
     * @param id
     * @param widgetContainer
     * @param bindingResult
     * @param user
     * @param request
     * @return
     */
    @ApiOperation(value = "update and publish widgetcontainer")
    @PutMapping("/{id}/publish")
    public ResponseEntity updateAndDeployWidget(@PathVariable Long id,
                                                @Valid @RequestBody WidgetContainerUpdate widgetContainer,
                                                @ApiIgnore BindingResult bindingResult,
                                                @ApiIgnore @CurrentUser User user,
                                                HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        if (invalidId(id) || !id.equals(widgetContainer.getId())) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        //修改widget
        widgetContainerService.updateWidgetContainer(widgetContainer, user);
        //发布widget
        deployService.deployWidgetContainer(id, user);

        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    @ApiOperation(value = "move widget")
    @PutMapping("/{id}/move/{folderId}")
    public ResponseEntity changeFolder(@PathVariable("id") Long id,
                                       @PathVariable("folderId") Long folderId,
                                       @ApiIgnore @CurrentUser User user,
                                       HttpServletRequest request) {
        widgetContainerService.changeFolder(id, folderId, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }
}
