package edp.davinci.extention;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.AbstractDateDeserializer;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;

import java.io.IOException;
import java.lang.reflect.Type;

public class DatartDateCodec extends AbstractDateDeserializer implements ObjectSerializer, ObjectDeserializer {

    public static final  DatartDateCodec instance = new DatartDateCodec();

    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.out;
        if (object == null) {
            out.writeNull();
            return;
        }

        Class<?> clazz = object.getClass();
        if (clazz == java.sql.Date.class) {
            out.writeString(object.toString());
        }
    }

    @Override
    protected <T> T cast(DefaultJSONParser parser, Type clazz, Object fieldName, Object value) {
        return null;
    }

    @Override
    public int getFastMatchToken() {
        return JSONToken.LITERAL_INT;
    }
}
