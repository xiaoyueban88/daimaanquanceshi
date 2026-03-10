package edp.core.utils;

import java.util.List;
import java.util.Stack;

import com.alibaba.druid.util.StringUtils;
import com.google.common.collect.Lists;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/9/9
 */
public class MatchUtils {
    public static final String parenthesesPre = "(";
    public static final String parenthesesSuffix = ")";

    public static void main(String[] args) {
        String source = "arrayJoin(arrayFilter(x,i -> i = 1,groupArray (if(toInt16 (is_zero_trans) = 1, 1, 0)),arrayEnumerateUniq (groupArray(user_id))))" +
                "                \",course_cate";
        List<String> parenthesesMatchStr = getParams(source);
        System.out.println(parenthesesMatchStr);
    }

    public static List<String> getParenthesesMatchStr(String source, String pre) {
        List<String> matchers = Lists.newArrayList();
        if (StringUtils.isEmpty(pre) || StringUtils.isEmpty(source)) {
            return matchers;
        }
        int index = source.indexOf(pre);
        if (index < 0) {
            return matchers;
        }
        Stack<String> stack = new Stack();
        StringBuilder builder = new StringBuilder();
        for (int i = index; i < source.length(); i++) {
            String str = source.substring(i, i + 1);
            builder = builder.append(str);
            if (parenthesesPre.equals(str)) {
                stack.push(parenthesesPre);
            }
            if (parenthesesSuffix.equals(str)) {
                stack.pop();
            }
            if (builder.length() > 0) {
                if (builder.toString().contains(pre) && stack.empty()) {
                    matchers.add(builder.toString());
                    builder = new StringBuilder();
                }
            }
        }
        return matchers;
    }

    public static String getInnerParenthesesStr(String source) {
        if (StringUtils.isEmpty(source)) {
            return null;
        }
        int index = source.indexOf(parenthesesPre);
        if (index < 0) {
            return null;
        }
        Stack<String> stack = new Stack();
        stack.push(parenthesesPre);
        StringBuilder builder = new StringBuilder();
        for (int i = index + 1; i < source.length(); i++) {
            String str = source.substring(i, i + 1);
            if (parenthesesPre.equals(str)) {
                stack.push(parenthesesPre);
            } else if (parenthesesSuffix.equals(str)) {
                stack.pop();
            }
            if (stack.isEmpty()) {
                break;
            }
            builder = builder.append(str);
        }
        return builder.toString();
    }

    public static List<String> getParams(String source) {
        List<String> params = Lists.newArrayList();
        Stack<String> stack = new Stack();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < source.length(); i++) {
            String str = source.substring(i, i + 1);
            if (",".equals(str) && stack.isEmpty()) {
                params.add(builder.toString());
                builder = new StringBuilder();
                continue;
            }
            if (parenthesesPre.equals(str)) {
                stack.push(parenthesesPre);
            } else if (parenthesesSuffix.equals(str)) {
                stack.pop();
            }
            builder.append(str);
            if (i == source.length() - 1) {
                params.add(builder.toString());
            }
        }
        return params;
    }

    /**
     * 去除两符号间内容
     * @param context
     * @param left
     * @param right
     * @return
     */
    public static String clearBracket(String context, char left, char right) {
        int head = context.indexOf(left);
        if (head == -1) {
            return context;
        } else {
            int next = head + 1;
            int count = 1;
            do {
                if (context.charAt(next) == left) {
                    count++;
                } else if (context.charAt(next) == right) {
                    count--;
                }
                next++;
                if (count == 0) {
                    String temp = context.substring(head, next);
                    context = context.replace(temp, "");
                    head = context.indexOf(left);
                    next = head + 1;
                    count = 1;
                }
            } while (head != -1);
        }
        return context;
    }
}
