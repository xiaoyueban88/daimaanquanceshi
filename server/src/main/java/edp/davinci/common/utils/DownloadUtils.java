package edp.davinci.common.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Description ${description}
 * @author zswu3
 * @date 2021/8/24
 */
public class DownloadUtils {
    public static void encodeFileName(HttpServletRequest request, HttpServletResponse response, String filename) throws UnsupportedEncodingException {
        response.setHeader("Content-Type", "application/force-download");
        if (request.getHeader("User-Agent").toLowerCase().indexOf("firefox") > 0) {
            // firefox浏览器
            filename = new String(filename.getBytes(StandardCharsets.UTF_8), "ISO8859-1");
        } else if (isIE(request)) {
            //IE
            filename = URLEncoder.encode(filename, "UTF-8");
        } else {
            filename = new String(filename.getBytes(StandardCharsets.UTF_8), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    }

    private static boolean isIE(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent").toLowerCase();
        return ((ua.indexOf("rv") > 0 && ua.contains("like gecko")) || ua.indexOf("msie") > 0);
    }
}
