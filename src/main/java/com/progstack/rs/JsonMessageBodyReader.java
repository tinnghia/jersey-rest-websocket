package com.progstack.rs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import com.progstack.json.JsonObjectReaderWriter;

@Consumes(MediaType.APPLICATION_JSON)
public class JsonMessageBodyReader implements MessageBodyReader<Object> {

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations,
                           MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                           InputStream entityStream) throws IOException, WebApplicationException {
        return JsonObjectReaderWriter.readObject(type, entityStream);
    }

}
