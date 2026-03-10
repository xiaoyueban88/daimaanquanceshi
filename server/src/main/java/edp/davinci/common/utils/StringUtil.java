package edp.davinci.common.utils;

import com.alibaba.druid.sql.visitor.functions.Char;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class StringUtil {

    public static String getNormalString(String origValue) {
        List<Character> builder = new ArrayList<>();
        for (int i = 0; i < origValue.length(); i++) {
            Character c = new Character(origValue.charAt(i));
            if ((c >= 0x4E00 && c <= 0x9FA5) || Character.isLetterOrDigit(c)) {
                builder.add(c);
            }
        }

        builder.sort(new Comparator<Character>(){

            @Override
            public int compare(Character o1, Character o2) {
                return  o1.compareTo(o2);
            }
        });
        StringBuilder sb = new StringBuilder(builder.size());

        builder.stream().forEach(c->sb.append(c));
        return sb.toString();
    }

    public static void main(String[] args) {
        getNormalString("222");
    }
}
