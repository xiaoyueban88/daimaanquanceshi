package edp.davinci.core.utils;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelWaterMarkUtil {
    /*
     * 为Excel打上水印工具函数 请自行确保参数值，以保证水印图片之间不会覆盖。 在计算水印的位置的时候，并没有考虑到单元格合并的情况，请注意
     *
     * @param wb
     *            Excel Workbook
     * @param sheet
     *            需要打水印的Excel
     * @param waterRemarkPath
     *            水印地址，classPath，目前只支持png格式的图片，
     *            因为非png格式的图片打到Excel上后可能会有图片变红的问题，且不容易做出透明效果。
     *            同时请注意传入的地址格式，应该为类似："\\excelTemplate\\test.png"
     * @param startXCol
     *            水印起始列
     * @param startYRow
     *            水印起始行
     * @param betweenXCol
     *            水印横向之间间隔多少列
     * @param betweenYRow
     *            水印纵向之间间隔多少行
     * @param XCount
     *            横向共有水印多少个
     * @param YCount
     *            纵向共有水印多少个
     * @param waterRemarkWidth
     *            水印图片宽度为多少列
     * @param waterRemarkHeight
     *            水印图片高度为多少行
     * @throws IOException
     */
    public static void putWaterRemarkToExcel(Workbook wb, Sheet sheet, String waterRemarkPath, int startXCol,
                                             int startYRow, int betweenXCol, int betweenYRow, int XCount, int YCount, int waterRemarkWidth,
                                             int waterRemarkHeight) throws IOException {

        // 校验传入的水印图片格式
        if (!waterRemarkPath.endsWith("png") && !waterRemarkPath.endsWith("PNG")) {
            throw new RuntimeException("向Excel上面打印水印，目前支持png格式的图片。");
        }
        // 加载图片
        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        InputStream imageIn = new FileInputStream(waterRemarkPath);
        //InputStream imageIn = Thread.currentThread().getContextClassLoader().getResourceAsStream(waterRemarkPath);
        if (null == imageIn || imageIn.available() < 1) {
            throw new RuntimeException("向Excel上面打印水印，读取水印图片失败(1)。");
        }
        BufferedImage bufferImg = ImageIO.read(imageIn);
        if (null == bufferImg) {
            throw new RuntimeException("向Excel上面打印水印，读取水印图片失败(2)。");
        }
        ImageIO.write(bufferImg, "png", byteArrayOut);

        // 开始打水印
        Drawing drawing = sheet.createDrawingPatriarch();
        // 按照共需打印多少行水印进行循环
        for (int yCount = 0; yCount < YCount; yCount++) {
            // 按照每行需要打印多少个水印进行循环
            for (int xCount = 0; xCount < XCount; xCount++) {
                // 创建水印图片位置
                int xIndexInteger = startXCol + (xCount * waterRemarkWidth) + (xCount * betweenXCol);
                int yIndexInteger = startYRow + (yCount * waterRemarkHeight) + (yCount * betweenYRow);
                /*
                 * 参数定义： 第一个参数是（x轴的开始节点）； 第二个参数是（是y轴的开始节点）； 第三个参数是（是x轴的结束节点）；
                 * 第四个参数是（是y轴的结束节点）； 第五个参数是（是从Excel的第几列开始插入图片，从0开始计数）；
                 * 第六个参数是（是从excel的第几行开始插入图片，从0开始计数）； 第七个参数是（图片宽度，共多少列）；
                 * 第8个参数是（图片高度，共多少行）；
                 */
                ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, xIndexInteger,
                        yIndexInteger, xIndexInteger + waterRemarkWidth, yIndexInteger + waterRemarkHeight);


//                anchor.setAnchorType(ClientAnchor.DONT_MOVE_AND_RESIZE);
                Picture pic = drawing.createPicture(anchor,
                        wb.addPicture(byteArrayOut.toByteArray(), Workbook.PICTURE_TYPE_PNG));
                pic.resize();
            }
        }
    }


    public static void createWaterMark(String content, String path) throws IOException {
        Integer width = 200;
        Integer height = 100;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);// 获取bufferedImage对象
        String fontType = "黑体";
        Integer fontStyle = Font.ITALIC;
        Integer fontSize = 15;
        Font font = new Font(fontType, fontStyle, fontSize);
        Graphics2D g2d = image.createGraphics(); // 获取Graphics2d对象
        image = g2d.getDeviceConfiguration().createCompatibleImage(width, height, Transparency.TRANSLUCENT);
        g2d.dispose();
        g2d = image.createGraphics();
        g2d.setColor(new Color(0, 0, 0, 80)); //设置字体颜色和透明度
        g2d.setStroke(new BasicStroke(1)); // 设置字体
        g2d.setFont(font); // 设置字体类型  加粗 大小
        g2d.rotate(Math.toRadians(-10), (double) image.getWidth() / 2, (double) image.getHeight() / 2);//设置倾斜度
        FontRenderContext context = g2d.getFontRenderContext();
        Rectangle2D bounds = font.getStringBounds(content, context);
        double x = (width - bounds.getWidth()) / 2;
        double y = (height - bounds.getHeight()) / 2;
        double ascent = -bounds.getY();
        double baseY = y + ascent;
        // 写入水印文字原定高度过小，所以累计写水印，增加高度
        g2d.drawString(content, (int) x, (int) baseY);
        // 设置透明度
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        // 释放对象
        g2d.dispose();
        ImageIO.write(image, "png", new File(path));
    }

    public static void main1(String[] args) throws IOException {
        //读取excel文件
        Workbook wb = new XSSFWorkbook(new FileInputStream("E:\\test.xlsx"));
        Workbook wb1 = new XSSFWorkbook(new FileInputStream("E:\\test1.xlsx"));
        //设置水印图片路径
        String imgPath = "E:\\水印.png";
        createWaterMark("zswu3@iflytek.com(吴兆生)", imgPath);
        //获取excel sheet个数
        int sheets = wb.getNumberOfSheets();
        //循环sheet给每个sheet添加水印
        for (int i = 0; i < sheets; i++) {
            Sheet sheet = wb.getSheetAt(i);
            //excel加密只读
            //sheet.protectSheet(UUID.randomUUID().toString());
            //获取excel实际所占行
            int row = sheet.getFirstRowNum() + sheet.getLastRowNum();
            //获取excel实际所占列
            int cell = sheet.getRow(sheet.getFirstRowNum()).getLastCellNum() + 1;
            //根据行与列计算实际所需多少水印
            putWaterRemarkToExcel(wb, sheet, imgPath, 0, 0, 5, 5, cell / 5 + 1, row / 5 + 1, 0, 0);
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            wb.write(os);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            wb = null;
        }

        byte[] content = os.toByteArray();
        // Excel文件生成后存储的位置。
        File file1 = new File("E:\\watermark.xlsx");
        OutputStream fos = null;
        try {
            fos = new FileOutputStream(file1);
            fos.write(content);
            os.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

