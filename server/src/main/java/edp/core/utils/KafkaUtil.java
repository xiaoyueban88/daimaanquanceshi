package edp.core.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import com.alibaba.fastjson.JSONObject;
import com.iflytek.edu.elp.common.util.SpringContextUtil;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class KafkaUtil {

    private static Logger logger = LoggerFactory.getLogger(KafkaUtil.class);

//    private static String kafkaServers = (String) SpringContextUtil.getApplicationContext().getBean("kafka.servers");


    public  static String produceMessage(String topic, String message){
        KafkaProducer<String, String> producer = null;
        if(StringUtils.isEmpty(topic) || StringUtils.isEmpty(message)){
            return "topic and msg parameters cannot be empty";
        }
        logger.info("get topic: "+topic+", produce msg: "+message);
        try{
            Properties props = new Properties();
//            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
            props.put(ProducerConfig.CLIENT_ID_CONFIG, "DefaultProducer");
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            producer = new KafkaProducer<String,String>(props);

            ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, message);
            Future<RecordMetadata> res = producer.send(record);
            logger.info("msg send kafka ");
        }catch (Exception e){
            logger.error("produce msg error."+e.getMessage());
            return "fail: "+e.getMessage();

        }finally {
            if(null!=producer) producer.close();
        }
        return "success";
    }

    public static void main(String[] args){
//        KafkaUtil kafkaUtil = new KafkaUtil();
//        Map<String,String> mes = new HashMap<>();
//        mes.put("tableName","test_table");
//        mes.put("updateTime","2020-04-21");
//        mes.put("status","Completed");
//        String sendMes = kafkaUtil.produceMessage("clickhouseUpdate",JSONObject.toJSONString(mes));
//        System.out.println(sendMes);
        String msg = "${java.vm}";
        logger.info("test, {}", msg);
    }
}
