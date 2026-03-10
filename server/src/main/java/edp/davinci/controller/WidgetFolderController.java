package edp.davinci.controller;

import javax.servlet.http.HttpServletRequest;

import edp.davinci.common.controller.BaseController;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.dto.WidgetFolder.WidgetFolderDto;
import edp.davinci.model.WidgetFolder;
import edp.davinci.service.WidgetFolderService;
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
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/5/7
 */

@Api(value = "/widgetfolder", tags = "widgets", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "widgetfolder not found"))
@Slf4j
@RestController
@RequestMapping(value = Constants.BASE_API_PATH + "/widgetfolder", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class WidgetFolderController extends BaseController {

    @Autowired
    private WidgetFolderService widgetFolderService;

    @ApiOperation(value = "get widgetfolders")
    @GetMapping
    public ResponseEntity getWidgetFolders(@RequestParam Long projectId,
                                           HttpServletRequest request) {
        if (invalidId(projectId)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid project id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        WidgetFolderDto result = widgetFolderService.getWidgetFolderDto(projectId);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(result));
    }

    @ApiOperation(value = "create widgetFolder")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createWidgetFolders(@RequestBody WidgetFolder widgetFolder,
                                              @ApiIgnore BindingResult bindingResult,
                                              HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        WidgetFolder newWidgetFolder = widgetFolderService.create(widgetFolder);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(newWidgetFolder));
    }

    @ApiOperation(value = "update widgetFolder")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity updateWidgetFolder(@RequestBody WidgetFolder widgetFolder,
                                             @ApiIgnore BindingResult bindingResult,
                                             HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        widgetFolderService.update(widgetFolder);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    @ApiOperation(value = "delete widgetFolder")
    @DeleteMapping("/{id}")
    public ResponseEntity deleteWidgetFolder(@PathVariable Long id,
                                             @RequestParam Long projectId,
                                             HttpServletRequest request) {
        if (invalidId(id)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        widgetFolderService.loopDeleteFolder(projectId, id);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }
}
