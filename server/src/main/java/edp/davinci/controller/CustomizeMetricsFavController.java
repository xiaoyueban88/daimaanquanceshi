package edp.davinci.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import edp.davinci.common.controller.BaseController;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.model.CustomizeMetricsFav;
import edp.davinci.service.CustomizeMetricsFavService;
import io.swagger.annotations.Api;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/4/28
 */
@Api(value = "/fav", tags = "CustomizeMetricsFavController", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "CustomizeMetricsFav not found"))
@Slf4j
@RestController
@RequestMapping(value = Constants.BASE_API_PATH + "/customizeMetricsFav", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class CustomizeMetricsFavController extends BaseController {

    @Autowired
    private CustomizeMetricsFavService customizeMetricsFavService;


    @PostMapping("/fav")
    public ResponseEntity favCustomizeMetrics(@Valid @RequestBody CustomizeMetricsFav customizeMetricsFav,
                                              @ApiIgnore BindingResult bindingResult,
                                              HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        CustomizeMetricsFav customize = customizeMetricsFavService.favCustomizeMetrics(customizeMetricsFav);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(customize));
    }

    @PostMapping("/update")
    public ResponseEntity updateFavCustomizeMetrics(@Valid @RequestBody CustomizeMetricsFav customizeMetricsFav,
                                                    @ApiIgnore BindingResult bindingResult,
                                                    HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message(bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        customizeMetricsFavService.updateFavMetrics(customizeMetricsFav);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity delFavCustomizeMetrics(@PathVariable Long id,
                                                 HttpServletRequest request) {
        customizeMetricsFavService.removeFavMetrics(id);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    @GetMapping("/favlist/{viewId}")
    public ResponseEntity getFavCustomizeMetricsList(@PathVariable Long viewId,
                                                     HttpServletRequest request) {
        List<CustomizeMetricsFav> result = customizeMetricsFavService.getMyFavMetricsByViewId(viewId);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payloads(result));
    }

}
