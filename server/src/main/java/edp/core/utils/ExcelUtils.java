package edp.core.utils;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.druid.util.StringUtils;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/7/16
 */
public class ExcelUtils {
    public static Workbook exportSimpleExcel(List<String> titleKeyList, Map<String, String> titleMap,
                                             List<Map<String, Object>> dataList) {
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        SXSSFSheet sheet = workbook.createSheet();


        if (CollectionUtils.isEmpty(titleKeyList)) {
            return null;
        }

        int rowIndex = 0;

        // 创建标题
        Row titleRow = sheet.createRow(rowIndex++);
        for (int i = 0; i < titleKeyList.size(); i++) {
            String titleKey = titleKeyList.get(i);
            String title = !StringUtils.isEmpty(titleMap.get(titleKey)) ? titleMap.get(titleKey) : titleKey;
            titleRow.createCell(i).setCellValue(title);
            sheet.trackAllColumnsForAutoSizing();
            sheet.autoSizeColumn(i);
        }

        // 创建数据列
        for (Map<String, Object> map : dataList) {
            int cellIndex = 0;
            Row row = sheet.createRow(rowIndex++);
            for (String titleKey : titleKeyList) {
                String value = map.get(titleKey) == null ? "" : map.get(titleKey).toString();
                row.createCell(cellIndex++).setCellValue(value);
            }
        }

        return workbook;
    }

    /**
     * 设置下载响应
     */
    public static void setDownLoadResponse(HttpServletRequest request, HttpServletResponse response, String fileName) throws Exception {
        String msie = "msie";
        String chrome = "chrome";
        String windows = "windows";
        String firefox = "firefox";
        String browserType = request.getHeader("User-Agent").toLowerCase();
        if (browserType.contains(firefox) || browserType.contains(chrome)) {
            fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        } else if (browserType.contains(msie) || browserType.contains(windows)) {
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } else {
            fileName = new String(fileName.getBytes());
        }
        // 重置
        response.reset();
        // 告知浏览器不缓存
        response.setHeader("pragma", "no-cache");
        response.setHeader("cache-control", "no-cache");
        response.setHeader("expires", "0");
        // 响应编码
        response.setCharacterEncoding("UTF-8");
        // 用给定的名称和值添加一个响应头
        response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
    }
}
