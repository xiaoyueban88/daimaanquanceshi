package edp.davinci;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/11/6
 */
public class TestExcel {
    public static void main(String[] args) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File("E://tan/3.xlsm"));
            XSSFWorkbook book = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = book.createSheet("5");
            XSSFSheet sheet8 = book.getSheet("8");
            int physicalNumberOfRows = sheet8.getPhysicalNumberOfRows();
            for (int i = 0; i < physicalNumberOfRows; i++) {
                XSSFRow row = sheet.createRow(i);
                XSSFRow sourceRow = sheet8.getRow(i);
                int physicalNumberOfCells = sourceRow.getPhysicalNumberOfCells();
                for (int j = 0; j < physicalNumberOfCells; j++) {
                    XSSFCell targetCell = row.createCell(j);
                    XSSFCell sourceCell = sourceRow.getCell(j);
                    getCellValue(sourceCell, targetCell);
                }
            }

            // 添加一行内容
            XSSFRow extraRow = sheet.createRow(physicalNumberOfRows);
            XSSFCell cell0 = extraRow.createCell(0);
            cell0.setCellValue(6);
            XSSFCell cell1 = extraRow.createCell(1);
            cell1.setCellValue("入门测");
            XSSFCell cell2 = extraRow.createCell(2);
            cell2.setCellValue(800);
            book.write(new FileOutputStream("E://tan/4.xlsm"));
            System.out.println("a");
        } catch (Exception e) {

        }

    }

    public static void getCellValue(XSSFCell sourCell, XSSFCell targetCell) {
        if (sourCell == null) {
            targetCell.setCellValue("");
        }

        switch (sourCell.getCellType()) {
            case BOOLEAN:
                targetCell.setCellValue(sourCell.getBooleanCellValue());
                break;
            case ERROR:
                targetCell.setCellValue(sourCell.getErrorCellValue());
                break;
            case STRING:
                targetCell.setCellValue(sourCell.getStringCellValue());
                break;
            case NUMERIC:
                targetCell.setCellValue(sourCell.getNumericCellValue());
                break;
        }
    }

}
