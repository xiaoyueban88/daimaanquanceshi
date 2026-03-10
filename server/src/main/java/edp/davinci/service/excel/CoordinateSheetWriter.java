package edp.davinci.service.excel;

import edp.core.utils.CollectionUtils;
import edp.davinci.core.common.Constants;
import edp.davinci.core.model.ExcelHeader;
import edp.davinci.core.utils.ExcelUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

/**
 * @ClassName CoordinateSheetWriter
 * @Author rqliu3
 * @CreateDate 2021/08/11 11:24
 * @Description :
 **/
public class CoordinateSheetWriter extends AbstractSheetWriter{
    //行维度
    private List<Map<String,Object>> colData;
    //列维度
    private List<Map<String,Object>> rowData;
    //原点 k:坐标轴名称 X Y  ,  v:初始位置
    private Map<String,Integer> originPoint;
    private GroupsContext groupsContext;
    private Workbook workbook;
    private Sheet sheet;
    private SheetContext sheetContext;
    //每列的最大宽度
    private List<Integer> colLengthList;
    private CellStyle valuesCellStyle;
    //存放行列维度的坐标
    private Map<String,Integer> xIndexMap;
    private Map<String,Integer> yIndexMap;


    /*
    * 初始化
    * */
    public CoordinateSheetWriter(SheetContext sheetContext) {
        this.sheetContext = sheetContext;
        this.groupsContext = sheetContext.getGroupsContext();
        this.workbook =sheetContext.getWorkbook();
        this.sheet = sheetContext.getSheet();
        //初始化xy轴
        //初始化列宽
        colLengthList = new ArrayList<>();
        //初始化原点
        this.originPoint = new HashMap<>();

        this.yIndexMap = new HashMap<>();
        this.xIndexMap = new HashMap<>();
        //获取原点坐标
        this.originPoint.put("x",sheetContext.getGroupsContext().getRowGroups().size());
        this.originPoint.put("y",sheetContext.getGroupsContext().getColGroups().size());



        //获取指标的指标的格式
        String dataFormat = null;
        DataFormat valuesDataFormat = null;
        valuesCellStyle = this.workbook.createCellStyle();
        try {
            dataFormat = ExcelUtils.getDataFormat(sheetContext.getExcelHeaders().get(sheetContext.getExcelHeaders().size()-1).getFormat());
            valuesDataFormat = sheetContext.getWorkbook().createDataFormat();
            short format = valuesDataFormat.getFormat(dataFormat);
            valuesCellStyle.setDataFormat(format);
        } catch (NullPointerException e) {

        }
    }

    /*
    * 写入坐标轴
    * */
    public void writeCoordinate(JdbcTemplate template,String allDataSql)  {
       //所有数据
       List<Map<String, Object>> allData = template.queryForList(allDataSql);
       //行维度
       List<Map<String, Object>> colListMap = template.queryForList(this.sheetContext.getColSql());
       //列维度
       List<Map<String, Object>> rowListMap = template.queryForList(this.sheetContext.getRowSql());
       //用于暂时存放行列维度
       this.colData = new ArrayList<>();
       this.rowData = new ArrayList<>();

        //存入列维度坐标
        for (int i = 0; i < rowListMap.size(); i++) {
            Map<String,Object> rowMap = new HashMap<>();
            for (int j = 0; j < this.groupsContext.getRowGroups().size(); j++) {
                rowMap.put(this.groupsContext.getRowGroups().get(j),rowListMap.get(i).get(this.groupsContext.getRowGroups().get(j)));
            }
            this.yIndexMap.put(concatenate(rowMap),i);
            this.rowData.add(rowMap);
        }

        //存入行维度坐标
        for (int i = 0; i < colListMap.size(); i++) {
            Map<String,Object> colMap = new HashMap<>();
            for (int j = 0; j < this.groupsContext.getColGroups().size(); j++) {
                colMap.put(this.groupsContext.getColGroups().get(j),colListMap.get(i).get(this.groupsContext.getColGroups().get(j)));
            }
            this.xIndexMap.put(concatenate(colMap),i);
            this.colData.add(colMap);
        }
        //设置表头的格式
        CellStyle coordinateCellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("黑体");
        font.setBold(true);
        coordinateCellStyle.setAlignment(HorizontalAlignment.CENTER);
        coordinateCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        coordinateCellStyle.setFont(font);
        //写出行维度
        for (int i = 0; i < this.groupsContext.getColGroups().size(); i++) {
            Row row = sheet.createRow(i);
            row.setHeightInPoints(20);
            for (int j = 0; j < this.colData.size(); j++) {
                Cell cell = row.createCell(j + this.originPoint.get("x"));
                cell.setCellValue(String.valueOf(this.colData.get(j).get(this.groupsContext.getColGroups().get(i))));
                cell.setCellStyle(coordinateCellStyle);
            }
        }


        //用于存放列的别名 k:key v:alias
        Map<String,String> rowHeaderAliasMap = new HashMap<>();

        //Key和Alias存入Map
        for (ExcelHeader excelHeader:this.sheetContext.getExcelHeaders()){
            rowHeaderAliasMap.put(excelHeader.getKey(),excelHeader.getAlias().isEmpty()?excelHeader.getKey():excelHeader.getAlias());
        }

        //写入列的表头
        int indexNo = this.groupsContext.getRowGroups().size();
        for (int i = 0; i < indexNo; i++) {
            Cell cell =sheet.getRow(0).createCell(i);
            //如果列的表头有Alias，替换为Alias
            cell.setCellValue(rowHeaderAliasMap.get(this.groupsContext.getRowGroups().get(i)));
            cell.setCellStyle(coordinateCellStyle);
        }


        //如果行大于1行，合并列表头的单元格
        if (this.originPoint.get("y")>1){
            for (int i = 0; i < indexNo; i++) {
                CellRangeAddress cellRangeAddress = new CellRangeAddress(0,this.groupsContext.getColGroups().size()-1,i,i);
                sheet.addMergedRegion(cellRangeAddress);
            }
        }

        //列出行维度
        for (int i = 0; i < this.rowData.size(); i++) {
            Row row = sheet.createRow(i+this.originPoint.get("y"));
            row.setHeightInPoints(20);
            for (int j = 0; j < this.rowData.get(0).size(); j++) {
               row.createCell(j).setCellValue(String.valueOf(this.rowData.get(i).get(this.groupsContext.getRowGroups().get(j))));
            }
        }
        //往坐标系里面写入指标
        insertValues(allData,sheet);
        //设置列宽
        setColWidth(sheet);
    }

    /*
    * 往坐标系里面写入指标
    * */
    public void insertValues(List<Map<String, Object>> allData,Sheet sheet){
        for (Map<String,Object> lineData : allData){

            Map<String,Object> map = new HashMap<>();

            //取出列维度的values,放入map
            for (int i = 0; i < this.groupsContext.getRowGroups().size(); i++) {
                map.put(this.groupsContext.getRowGroups().get(i),lineData.get(this.groupsContext.getRowGroups().get(i)));
            }
            //得到列维度的坐标
            String yKey = concatenate(map);

            map.clear();

            //取出行维度的values,放入map
            for (int i = 0; i < this.groupsContext.getColGroups().size(); i++) {
                map.put(this.groupsContext.getColGroups().get(i),lineData.get(this.groupsContext.getColGroups().get(i)));
            }
            //得到行维度的坐标
            String xKey = concatenate(map);

            //根据行列维度的坐标写入指标
            Row row = sheet.getRow(this.yIndexMap.get(yKey)+this.groupsContext.getColGroups().size());
            Cell cell = row.createCell(this.xIndexMap.get(xKey)+this.groupsContext.getRowGroups().size());
            cell.setCellStyle(this.valuesCellStyle);
            try {
                cell.setCellValue(Double.parseDouble(String.valueOf(lineData.get(this.sheetContext.getMetricKey()))));
            } catch (Exception e) {
                cell.setCellValue(String.valueOf(lineData.get(this.sheetContext.getMetricKey())));
            }

        }

    }

    //将Map的所有的value拼接为key
    public String concatenate(Map<String,Object> map){
        if(CollectionUtils.isEmpty(map)){
            return null;
        }
        List<Object> valuesList = new ArrayList<>(map.values());
        StringBuilder key = null;
        for (int i = 0; i < valuesList.size(); i++) {
            if(i==0){
                key = new StringBuilder(String.valueOf(valuesList.get(i)));
            }else {
                key.append(Constants.SPLIT_CHAR_STRING).append(valuesList.get(i));
            }
        }
        return key.toString();
    }
    /*
    * 设置列宽
    * */
    public void setColWidth(Sheet sheet){
        //按列遍历获取每列最大宽度
        for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
            this.colLengthList.add(0);
            for (int j = 0; j <=sheet.getLastRowNum(); j++) {
                Cell cell=sheet.getRow(j).getCell(i);
                int length;
                    try {
                        if(String.valueOf(cell.getNumericCellValue())!=null){
                            length = String.valueOf(sheet.getRow(j).getCell(i).getNumericCellValue()).length()*2;
                        }else {
                            length = 0;
                        }
                } catch (Exception Exception){
                        if(cell!=null){
                            length = cell.getStringCellValue().length()*2;
                        }else {
                            length = 0;
                        }
                }
                //存入每列的最大宽度
                if(length>this.colLengthList.get(i)){
                    this.colLengthList.set(i,length);
                }
            }
            //按列设置宽度
            sheet.setColumnWidth(i,(this.colLengthList.get(i)+2)*256);
        }
    }

}