package edp.core.utils;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;


/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/9/7
 */
public class UUIDUtils {
    private static final List<String> CHARS = Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
            "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
            "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z");


    public static String generateShortUuid() {
        StringBuffer shortBuffer = new StringBuffer();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < 8; i++) {
            String str = uuid.substring(i * 4, i * 4 + 4);
            int x = Integer.parseInt(str, 16);
            shortBuffer.append(CHARS.get(x % 0x34));
        }
        return shortBuffer.toString();
    }
}
