package com.progstack.rs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import com.progstack.json.JsonObjectReaderWriter;

@Produces(MediaType.APPLICATION_JSON)
public class JsonMessageBodyWriter implements MessageBodyWriter<Object> {

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    public long getSize(Object bean, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType) {
        // deprecated by JAX-RS 2.0 and ignored by Jersey runtime
        return -1;
    }

    public void writeTo(Object bean, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        JsonObjectReaderWriter.writeObject(bean, type, entityStream);

    }
}
