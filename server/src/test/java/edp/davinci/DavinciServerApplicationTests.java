/*
 * <<
 * Davinci
 * ==
 * Copyright (C) 2016 - 2018 EDP
 * ==
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * >>
 */

package edp.davinci;

import edp.core.common.jdbc.JdbcDataSource;
import edp.core.model.JdbcSourceInfo;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pentaho.di.trans.steps.olapinput.olap4jhelper.CellSetFormatter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DavinciServerApplicationTests {

    @Test
    public void contextLoads() {
        Workbook workbook = new SXSSFWorkbook();
        Sheet sheet = workbook.createSheet("aaaaaa");
        sheet.setColumnWidth(0,10 * 256);
        CellStyle cellStyle = workbook.createCellStyle();
        DataFormat dataForma = workbook.createDataFormat();
        Object c = 0.44;
        cellStyle.setDataFormat((short) 10);
        Row row = sheet.createRow(0);
        row.setHeightInPoints(20);
        Cell cell = row.createCell(0);
        cell.setCellValue("a");
        cell.setCellStyle(cellStyle);
        try {
            OutputStream outputStream = new FileOutputStream("E:\\tst.xlsx");
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void test() throws SQLException, ClassNotFoundException {
       String a = "SELECT  district_name,school_name FROM (SELECT * FROM dm_qyjy_class_subject_fact_dis WHERE exam_name <> 'bg-1' AND exam_id IN ('002bc239-c0c8-49bb-b6f2-649543c1112d', '001c8f2d-906c-48dc-81a6-ffc42661fcc1') AND `subject_name` = '总分') GROUP BY `district_name`,`school_name`,`district_id` ,`school_id`,exam_time ORDER BY  `district_id` asc ,`school_id` asc,exam_time asc";//ORDER BY
       String b ="SELECT `exam_name`, `district_name`, `school_name`, if(max(toFloat64(province_student_avg_score)) = 0, -1000, round(((max(toFloat64(school_student_avg_score)) - max(toFloat64(province_student_avg_score))) / max(toFloat64(province_student_avg_score))), 4)) AS \"customize(超均率-新)\" FROM (SELECT if(province_student_avg_score = 'bg-1', '0', province_student_avg_score) AS province_student_avg_score, if(school_student_avg_score = 'bg-1', '0', school_student_avg_score) AS school_student_avg_score, dm_qyjy_class_subject_fact_dis.`school_name`, dm_qyjy_class_subject_fact_dis.`exam_name`, dm_qyjy_class_subject_fact_dis.`exam_time`, dm_qyjy_class_subject_fact_dis.`subject_name`, dm_qyjy_class_subject_fact_dis.`district_name` FROM dm_qyjy_class_subject_fact_dis WHERE exam_name <> 'bg-1' AND exam_id IN ('002bc239-c0c8-49bb-b6f2-649543c1112d', '001c8f2d-906c-48dc-81a6-ffc42661fcc1') AND `subject_name` = '总分') T GROUP BY `exam_name`, `district_name`, `school_name` ORDER BY min(`exam_time`) ASC,";
       String c = " ";
       Class.forName("com.mysql.jdbc.Driver");
//数据库连接所需参数
        String user = "root";
        String password = "123456";
        String url = "jdbc:clickhouse://172.31.65.126:8123/jzjx";
//2、获取连接对象
        Connection conn = DriverManager.getConnection(url, "", "");
        DataSource dataSource = new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return conn;
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return null;
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return false;
            }

            @Override
            public PrintWriter getLogWriter() throws SQLException {
                return null;
            }

            @Override
            public void setLogWriter(PrintWriter out) throws SQLException {

            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {

            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return 0;
            }

            @Override
            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                return null;
            }
        };
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        for (Map map : jdbcTemplate.queryForList(a)){
            System.out.println(map);
        }

    }
    public static boolean isChinese(char c) {

        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);

        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS

                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS

                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A

                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION

                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION

                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {

            return true;

        }

        return false;
    }
}
