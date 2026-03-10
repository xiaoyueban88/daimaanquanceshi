package edp.davinci.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Maps;

import edp.core.annotation.AuthIgnore;
import edp.core.annotation.CurrentUser;
import edp.core.annotation.PaperShare;
import edp.core.model.Paginate;
import edp.core.utils.ExcelUtils;
import edp.davinci.common.controller.BaseController;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ResultMap;
import edp.davinci.dto.CollectDto.CollectPaperDto;
import edp.davinci.dto.CollectDto.PaperAnswerInfoDto;
import edp.davinci.dto.CollectDto.PaperApproveDto;
import edp.davinci.dto.CollectDto.PaperConfigDto;
import edp.davinci.model.CollectPaper;
import edp.davinci.model.CollectPaperTopic;
import edp.davinci.model.CollectPaperTopicAnswer;
import edp.davinci.model.User;
import edp.davinci.service.CollectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/8
 */

@Api(value = "/collect", tags = "collect", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@ApiResponses(@ApiResponse(code = 404, message = "collect not found"))
@Slf4j
@RestController
@RequestMapping(value = Constants.BASE_API_PATH + "/collect", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class CollectController extends BaseController {

    @Autowired
    private CollectService collectService;

    /**
     * 获取我创建的表单
     *
     * @param projectId 项目id
     * @param request
     * @return
     */
    @GetMapping("/{projectId}/mycreate")
    public ResponseEntity getMyCreatePaper(@PathVariable Long projectId,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam Integer pageNo,
                                           @RequestParam Integer pageSize,
                                           @ApiIgnore @CurrentUser User user,
                                           HttpServletRequest request) {
        if (invalidId(projectId)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        Paginate<CollectPaper> pagerPapers = collectService.getPagerPapers(projectId, keyword, pageNo, pageSize, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(pagerPapers));
    }

    /**
     * 修改问卷配置
     *
     * @param config
     * @param request
     * @return
     */
    @PostMapping("/updatePaperConfig")
    public ResponseEntity updatePaperConfig(@RequestBody PaperConfigDto config,
                                            HttpServletRequest request) {
        if (invalidId(config.getPaperId())) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        collectService.updateConfigPaper(config);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    /**
     * 删除问卷
     *
     * @param paperId
     * @param request
     * @return
     */
    @DeleteMapping("/deletePaper")
    public ResponseEntity removePaperConfig(Long paperId, HttpServletRequest request) {
        if (invalidId(paperId)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        collectService.removePaper(paperId);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    /**
     * 获取问卷题目信息
     *
     * @param token
     * @return
     */
    @GetMapping("/getTopics/{token:.+}")
    @PaperShare
    public ResponseEntity getPaperTopics(@PathVariable("token") String token) {
        Pair<String, List<CollectPaperTopic>> paperInfo = collectService.getPaperTopics(token);
        Map<String, Object> result = Maps.newHashMap();
        result.put("title", paperInfo.getLeft());
        result.put("topics", paperInfo.getRight());
        return ResponseEntity.ok(new ResultMap(tokenUtils).payload(result));
    }

    /**
     * 提交问卷答题
     *
     * @param token
     * @param answers
     * @param request
     * @param user
     * @return
     */
    @PostMapping("/submitAnswer/{token:.+}")
    @PaperShare
    public ResponseEntity submitPaperAnswer(@PathVariable("token") String token,
                                            @RequestBody List<CollectPaperTopicAnswer> answers,
                                            HttpServletRequest request,
                                            @ApiIgnore @CurrentUser User user) {
        collectService.submitPaperAnswer(answers, token, user);
        if (user == null) {
            return ResponseEntity.ok(new ResultMap().success());
        } else {
            return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
        }

    }

    /**
     * 获取我的评审
     *
     * @param projectId
     * @param keyword
     * @param pageNo
     * @param pageSize
     * @param user
     * @param request
     * @return
     */
    @GetMapping("/{projectId}/myapprove")
    public ResponseEntity getMyApprovePaper(@PathVariable Long projectId,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam Integer pageNo,
                                            @RequestParam Integer pageSize,
                                            @ApiIgnore @CurrentUser User user,
                                            HttpServletRequest request) {
        if (invalidId(projectId)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        Paginate<PaperApproveDto> approveInfo = collectService.getApproveInfo(projectId, user, keyword, pageNo, pageSize);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(approveInfo));
    }

    /**
     * 获取问卷提交记录的评审记录
     *
     * @param paperId
     * @param pageNo
     * @param pageSize
     * @param status
     * @param request
     * @return
     */
    @GetMapping("/approve/answers")
    public ResponseEntity getApproveTopics(@RequestParam Long paperId,
                                           @RequestParam Integer pageNo,
                                           @RequestParam Integer pageSize,
                                           @RequestParam(required = false) Short status,
                                           HttpServletRequest request) {
        if (invalidId(paperId)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }

        PaperAnswerInfoDto topicAnswers = collectService.getTopicAnswers(paperId, pageNo, pageSize, status);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(topicAnswers));
    }

    @PostMapping("/approve/{paperId}/submit")
    public ResponseEntity approveSubmitRecord(@PathVariable("paperId") Long paperId,
                                              @RequestBody List<Long> submitIds,
                                              @ApiIgnore @CurrentUser User user,
                                              HttpServletRequest request) {
        if (invalidId(paperId)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        collectService.approveSubmit(paperId, submitIds, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }


    @GetMapping("/{projectId}/mysubmit")
    public ResponseEntity getMySubmitPaper(@PathVariable Long projectId,
                                           @ApiIgnore @CurrentUser User user,
                                           HttpServletRequest request) {
        return null;
    }

    /**
     * 创建问卷
     *
     * @param collectPaperDto
     * @param user
     * @param request
     * @return
     */
    @PostMapping("/create")
    public ResponseEntity createPaper(@RequestBody CollectPaperDto collectPaperDto,
                                      @ApiIgnore @CurrentUser User user,
                                      HttpServletRequest request) {
        collectService.createCollectPaper(collectPaperDto, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    @GetMapping("/{paperId}/topics")
    public ResponseEntity getPaperTopic(@PathVariable("paperId") Long paperId,
                                        @ApiIgnore @CurrentUser User user,
                                        HttpServletRequest request) {

        CollectPaperDto paperTopicInfo = collectService.getPaperTopicInfo(paperId, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(paperTopicInfo));
    }

    @PostMapping("/{paperId}/updateTopic")
    public ResponseEntity updatePaperTopic(@PathVariable("paperId") Long paperId,
                                           @RequestBody CollectPaperDto collectPaperDto,
                                           @ApiIgnore @CurrentUser User user,
                                           HttpServletRequest request) {
        if (invalidId(paperId)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        collectService.updatePaperTopic(paperId, collectPaperDto);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    @DeleteMapping("submit/{submitId}/delete")
    public ResponseEntity deleteSubmitRecord(@PathVariable("submitId") Long submitId,
                                             @ApiIgnore @CurrentUser User user,
                                             HttpServletRequest request) {
        if (invalidId(submitId)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        collectService.deleteSubmitRecord(submitId, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    @PostMapping("submit/{submitId}/update")
    public ResponseEntity updateSubmitRecord(@PathVariable("submitId") Long submitId,
                                             @RequestBody List<CollectPaperTopicAnswer> answers,
                                             HttpServletRequest request,
                                             @ApiIgnore @CurrentUser User user) {
        if (invalidId(submitId)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        collectService.updateSubmitRecord(submitId, answers, user);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request));
    }

    @GetMapping("submit/{submitId}/query")
    public ResponseEntity getAnswerRecord(@PathVariable("submitId") Long submitId,
                                          HttpServletRequest request,
                                          @ApiIgnore @CurrentUser User user) {
        if (invalidId(submitId)) {
            ResultMap resultMap = new ResultMap(tokenUtils).failAndRefreshToken(request).message("Invalid id");
            return ResponseEntity.status(resultMap.getCode()).body(resultMap);
        }
        List<CollectPaperTopicAnswer> answers = collectService.getSubmitRecord(submitId);
        return ResponseEntity.ok(new ResultMap(tokenUtils).successAndRefreshToken(request).payload(answers));
    }

    @GetMapping("/{paperId}/download")
    @AuthIgnore
    public ResponseEntity download(@PathVariable("paperId") Long paperId,
                                   @ApiIgnore @CurrentUser User user,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        Workbook workbook = null;
        try {
            Pair<String, Workbook> download = collectService.download(paperId);
            String title = download.getLeft();
            workbook = download.getRight();
            ExcelUtils.setDownLoadResponse(request, response, title + ".xlsx");
            workbook.write(response.getOutputStream());
        } catch (Exception e) {
            log.error("下载异常", e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.error("关闭异常", e);
                }
            }
        }
        return null;
    }


}
