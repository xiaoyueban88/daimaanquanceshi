package edp.davinci.common.utils;

import org.apache.commons.codec.digest.DigestUtils;

public class EncryptUtil {
    public static String encodeSHA(String data) {
        // 执行消息摘要
        return DigestUtils.sha256Hex(data);
    }
}
