package edp.davinci;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.IOUtils;

/**
 * @author zswu3
 * @Description ${description}
 * @date 2020/8/6
 */
public class Test {
    public static final String HTML_TAG_BGN = "<html xmlns=\"http://www.w3.org/TR/REC-html40\" xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:w=\"urn:schemas-microsoft-com:office:word\" xmlns:m=\"http://schemas.microsoft.com/office/2004/12/omml\"><head><meta name=\"ProgId\" content=\"Word.Document\" /><meta name=\"Generator\" content=\"Microsoft Word 12\" /><meta name=\"Originator\" content=\"Microsoft Word 12\" /> <!--[if gte mso 9]><xml><w:WordDocument><w:View>Print</w:View></w:WordDocument></xml><[endif]-->";

    public static void main(String[] args) throws MalformedURLException {
//        System.out.println("hello world !!!");
        //创建一个URL实例
//        URL url = new URL("http://www.baidu.com");

        StringBuilder html = new StringBuilder();
        try {
//            InputStream is = url.openStream();
//            InputStreamReader isr = new InputStreamReader(is, "utf-8");
            String sourcePath = "E://myword/data.html";
            File sourceFile = new File(sourcePath);
            BufferedReader br = new BufferedReader(new FileReader(sourceFile));
            String data = br.readLine();//读取数据

            while (data != null) {//循环读取数据
                data = br.readLine();
                html.append(data);
            }
            br.close();
//            isr.close();
//            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String wordString = html.toString().replaceAll("<head>", "").replaceAll("<html>", HTML_TAG_BGN);
        POIFSFileSystem poifs = null;
        FileOutputStream ostream = null;
        ByteArrayInputStream bais = null;
        String uuid = "E://测试1.doc";
        byte[] bytes = wordString.getBytes();
        bais = new ByteArrayInputStream(bytes);
        poifs = new POIFSFileSystem();
        DirectoryEntry directory = poifs.getRoot();
        try {
            directory.createDocument("WordDocument", bais);
            ostream = new FileOutputStream(uuid);
            poifs.writeFilesystem(ostream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(poifs);
            IOUtils.closeQuietly(ostream);
            IOUtils.closeQuietly(bais);
        }


    }
}
