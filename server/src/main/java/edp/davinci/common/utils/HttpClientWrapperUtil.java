package edp.davinci.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.iflytek.edu.elp.common.exception.ELPSysException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

public class HttpClientWrapperUtil {

    private static Logger logger = LoggerFactory.getLogger(HttpClientWrapperUtil.class);

    public static String simpleHttpGetWithHeader(String url, Map<String,String> headerMap){
        Assert.notNull(url, "url is required!");
        try {
            String result=""; //返回信息
            HttpGet request= new HttpGet(url);
            //添加header
            if(headerMap!=null){
                for(String key:headerMap.keySet()){
                    if(request.containsHeader(key)){
                        request.setHeader(key, headerMap.get(key));
                    }else{
                        request.addHeader(key,headerMap.get(key));
                    }
                }

            }
            HttpClient httpClient=new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,3000);//连接时间
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,60000);//数据传输时间
            HttpResponse httpResponse=httpClient.execute(request);
            //获取返回状态
            int statusCode=httpResponse.getStatusLine().getStatusCode();
            if(statusCode== HttpStatus.SC_OK){
                //得到客户段响应的实体内容
                HttpEntity responseHttpEntity=httpResponse.getEntity();
                //得到输入流
                InputStream in=responseHttpEntity.getContent();
                //得到输入流的内容
                result=getData(in);
            }
            return result;
        }catch (Exception e){
            logger.error("http请求错误"+e.getMessage());
            throw new ELPSysException("http请求错误", e);
        }

    }

    /*
    *原HttpClient封装版本没有提供head参数，参数不支持多层对象，这里扩展下
    * */
    public static String httpPostWithHeader(String url, Map<String, Object> params, Map<String,String> headerMap){
        Assert.notNull(url, "url is required!");

        HttpClient httpClient = new DefaultHttpClient( new ThreadSafeClientConnManager());
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,3000);//连接时间
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,60000);//数据传输时间


        HttpPost httpPost = getHttpPost(url, params);

        //添加header
        if(headerMap!=null){
            for(String key:headerMap.keySet()){
                if(httpPost.containsHeader(key)){
                    httpPost.setHeader(key, headerMap.get(key));
                }else{
                    httpPost.addHeader(key,headerMap.get(key));
                }
            }
        }
        HttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);
            HttpEntity httpEntity = response.getEntity();
            return EntityUtils.toString(httpEntity);
        } catch (IOException e) {
            logger.error("http请求错误"+e.getMessage());
            throw new ELPSysException("http请求错误", e);
        }
    }

    /**
     * 获取httppost，改为json参数在body中,
     *
     * @param url    the url
     * @param params 参数
     * @return the httpost
     */
    private static HttpPost getHttpPost(String url, Map<String, Object> params) {
        HttpPost httpost = new HttpPost(url);
        StringEntity entity = new StringEntity(CollectionUtils.isEmpty(params)?"":JSONObject.toJSONString(params), Charset.forName("UTF-8"));
        try {
            httpost.setEntity(entity);
        } catch (Exception e) {
            throw new ELPSysException("http请求错误", e);
        }

        return httpost;
    }

    /**
     * getData
     *
     * @param httpResponseInputStream    http输入流
     * @return http响应字符串
     */
    private static String getData(InputStream httpResponseInputStream) {
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        int data = -1;
        try {
            //跳过bom头
            byte[] bytes = new byte[3];
            for (int index = 0; index < bytes.length; index++) {
                data = httpResponseInputStream.read();
                if (data != -1) {
                    bytes[index] = (byte) data;
                } else {
                    break;
                }
            }
            if (!(bytes.length == 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB
                    && (bytes[2] & 0xFF) == 0xBF)) {
                for (int index = 0; index < bytes.length; index++) {
                    byteArray.write(bytes[index]);
                }
            }
            while ((data = httpResponseInputStream.read()) != -1) {
                byteArray.write(data);
            }
            return byteArray.toString();
        } catch (IOException e) {
            throw new ELPSysException("http请求错误", e);
        }
    }
}
