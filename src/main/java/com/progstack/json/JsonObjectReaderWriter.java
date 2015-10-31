package com.progstack.json;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonGenerator;

public class JsonObjectReaderWriter {

    public static <T> T readObject(Class<T> type, InputStream inputStream) {
        JsonReader reader = Json.createReader(inputStream);
        JsonObject jobject = reader.readObject();
        try {

            Field[] fields = type.getDeclaredFields();
            T instance = type.newInstance();
            for (Field field : fields) {
                String fname = field.getName();
                if (jobject.isNull(fname)) {
                    continue;
                }
                String val = jobject.get(fname).toString();
                Class<?> ftype = field.getType();
                if (ftype.isAssignableFrom(String.class)) {
                    callSetter(instance, type, field, val);
                } else if (ftype.isAssignableFrom(boolean.class)) {
                    callSetter(instance, type, field, Boolean.parseBoolean(val));
                } else if (ftype.isAssignableFrom(int.class)) {
                    callSetter(instance, type, field, Integer.parseInt(val));
                } else if (ftype.isAssignableFrom(long.class)) {
                    callSetter(instance, type, field, Long.parseLong(val));
                } else if (ftype.isAssignableFrom(short.class)) {
                    callSetter(instance, type, field, Short.parseShort(val));
                } else if (ftype.isAssignableFrom(float.class)) {
                    callSetter(instance, type, field, Float.parseFloat(val));
                } else if (ftype.isAssignableFrom(double.class)) {
                    callSetter(instance, type, field, Double.parseDouble(val));
                }
            }
            return instance;
        } catch (Exception e) {
            throw new InstantiationError(e.getMessage());
        } finally {
            reader.close();
        }

    }

    public static <T> void writeObject(Object entity, Class<T> type, OutputStream outputStream) {

        JsonGenerator generator = Json.createGenerator(outputStream);

        try {
            generator = generator.writeStartObject();
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                callGetter(entity, type, field, generator);
            }
            generator.writeEnd();
            generator.flush();
        } catch (Exception ex) {
            throw new InstantiationError(ex.getMessage());
        } finally {
            generator.close();
        }
    }

    private static void callSetter(Object instance, Class<?> clazz, Field field, Object value)
        throws Exception {
        String fieldName = field.getName();
        String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        Method setter = clazz.getMethod(methodName, field.getType());
        setter.invoke(instance, value);
    }

    private static void callGetter(Object instance, Class<?> clazz, Field field, JsonGenerator generator)
        throws Exception {
        String fieldName = field.getName();
        Class<?> ftype = field.getType();
        String methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        Method getter = clazz.getMethod(methodName);
        Object value = getter.invoke(instance);
        if (value == null)
            return;

        if (ftype.isAssignableFrom(String.class)) {
            generator.write(fieldName, value.toString());
        } else if (ftype.isAssignableFrom(boolean.class)) {
            generator.write(fieldName, Boolean.parseBoolean(value.toString()));
        } else if (ftype.isAssignableFrom(int.class)) {
            generator.write(fieldName, Integer.parseInt(value.toString()));
        } else if (ftype.isAssignableFrom(long.class)) {
            generator.write(fieldName, Long.parseLong(value.toString()));
        } else if (ftype.isAssignableFrom(short.class)) {
            generator.write(fieldName, Short.parseShort(value.toString()));
        } else if (ftype.isAssignableFrom(float.class)) {
            generator.write(fieldName, Float.parseFloat(value.toString()));
        } else if (ftype.isAssignableFrom(double.class)) {
            generator.write(fieldName, Double.parseDouble(value.toString()));
        }
    }
}
