package edp.davinci.common.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iflytek.edu.elp.common.util.StringUtils;

import edp.core.enums.NodeType;
import edp.davinci.dto.DataTransDto.NodeLine;
import edp.davinci.dto.DataTransDto.NodeList;
import edp.davinci.dto.DataTransDto.ProcessDto;

/**
 * @author wenzhang8
 * @date 2019/2/26
 * @description 工具类
 */
public class ToolUtils {

    /**
     * 节点数据转换为xml流程模板
     * @param processDto
     * @param nodeLists
     * @param nodeLines
     * @return
     */
    public static String object2XML(ProcessDto processDto, List<NodeList> nodeLists, List<NodeLine> nodeLines) {
        StringBuilder builder = new StringBuilder();
        Map<String, List<Map<String, String>>> lineMap = getNodeLineForMap(nodeLines);
        builder.append("<process name=\"")
        .append(processDto.getName()).append("\" displayName=\"")
        .append(processDto.getDisplayName()).append("\" instanceUrl=\"").append(processDto.getInstanceUrl())
        .append("\" creator=\"").append(processDto.getCreator()).append("\">\n");
        for (NodeList nodeList : nodeLists) {
            if (NodeType.START.getType().equals(nodeList.getType())) {
                builder.append("<start displayName=\"").append(nodeList.getNodeId()).append("\" name=\"").append(nodeList.getNodeId()).append("\" uuid=\"").append(StringUtils.join(nodeList.getUuid(), ",")).append("\" anchor=\"").append(StringUtils.join(nodeList.getAnchor(), ",")).append("\" type=\"").append(nodeList.getType()).append("\">\n");
                for (Map<String, String> map : lineMap.get(nodeList.getNodeId())) {
                    builder.append("<transition name=\"").append(map.get("name")).append("\" from=\"").append(map.get("from")).append("\" to=\"").append(map.get("to")).append("\" source=\"").append(map.get("source")).append("\" target=\"").append(map.get("target")).append("\"/>\n");
                }
                builder.append("</start>\n");
            } else if (NodeType.END.getType().equals(nodeList.getType())) {
                builder.append("<end displayName=\"").append(nodeList.getNodeId()).append("\" name=\"").append(nodeList.getNodeId()).append("\" uuid=\"").append(StringUtils.join(nodeList.getUuid(), ",")).append("\" anchor=\"").append(StringUtils.join(nodeList.getAnchor(), ",")).append("\" type=\"").append(nodeList.getType()).append("\"/>\n");
            } else if (NodeType.TASK.getType().equals(nodeList.getType())) {
                builder.append("<task displayName=\"").append(nodeList.getNodeId()).append("\" name=\"").append(nodeList.getNodeId()).append("\" assignee=\"job-admin\" autoExecute=\"Y\" performType=\"ANY\" taskType=\"Major\"").append(" uuid=\"").append(StringUtils.join(nodeList.getUuid(), ",")).append("\" anchor=\"").append(StringUtils.join(nodeList.getAnchor(), ",")).append("\" type=\"").append(nodeList.getType()).append("\">\n");
                for (Map<String, String> map : lineMap.get(nodeList.getNodeId())) {
                    builder.append("<transition name=\"").append(map.get("name")).append("\" from=\"").append(map.get("from")).append("\" to=\"").append(map.get("to")).append("\" source=\"").append(map.get("source")).append("\" target=\"").append(map.get("target")).append("\"/>\n");
                }
                builder.append("</task>\n");
            }
        }
        builder.append("</process>");
        return builder.toString();
    }

    /**
     *
     * @param xml xml文本
     * @param nodeLists 节点列表
     * @param nodeLines 节点连线
     */
    public static void xml2Object(String xml, List<NodeList> nodeLists, List<NodeLine> nodeLines) {
    }

    /**
     * 生成节点关联的map数据
     * @param nodeLines
     * @return
     */
    private static Map<String, List<Map<String, String>>> getNodeLineForMap(List<NodeLine> nodeLines) {
        Map<String, List<Map<String, String>>> result = new HashMap<>();
        for (NodeLine nodeLine : nodeLines) {
            List<Map<String, String>> mapList;
            if (result.containsKey(nodeLine.getFrom())) {
                mapList = result.get(nodeLine.getFrom());
            } else {
                mapList = new ArrayList<>();
            }
            Map<String, String> map = new HashMap<>();
            map.put("name", nodeLine.getName());
            map.put("from", nodeLine.getFrom());
            map.put("to", nodeLine.getTo());
            map.put("source", nodeLine.getSource());
            map.put("target", nodeLine.getTarget());
            mapList.add(map);
            result.put(nodeLine.getFrom(), mapList);
        }
        return result;
    }
}
