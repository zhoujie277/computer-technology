package com.future.netty.chat.common.codec;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.future.netty.chat.common.message.LoginResponse;
import com.future.netty.chat.common.message.Message;
import com.future.netty.chat.common.message.User;
import com.future.netty.chat.common.message.Response.ResultCode;
import com.future.util.ReflectUtil;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;

public class ProtobufSerializer implements Serializer<Message> {

    private Class<?> getProtoClass(int msgType) throws ClassNotFoundException {
        Class<? extends Message> msgClazz = Message.getMessageClass(Message.MsgType.values()[msgType]);
        return mapProtoClass(msgClazz);
    }

    @Override
    public Message deserialize(int msgType, byte[] in) {
        try {
            Class<?> protoClass = getProtoClass(msgType);
            Object protoObj = protoClass.getMethod("parseFrom", byte[].class).invoke(protoClass, in);
            // protoObj -> message
            Class<?> msgClass = mapMessageClass(protoObj.getClass());
            Object msgObject = msgClass.newInstance();
            convertMsgObject(protoObj, msgObject);
            return (Message) msgObject;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public byte[] serialize(Message message) {
        try {
            // message -> protoobj
            Class<?> pbClass = mapProtoClass(message.getClass());
            Object pbBuilder = newBuilder(pbClass);
            convertPbObject(pbBuilder, message);
            Object result = pbBuilder.getClass().getDeclaredMethod("build").invoke(pbBuilder);
            return (byte[]) result.getClass().getMethod("toByteArray").invoke(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    private static void convertPbObject(Object pbBuilder, Object msgObject) throws Exception {
        Class<?> msgClazz = msgObject.getClass();
        Class<?> pbClazz = pbBuilder.getClass();
        Descriptor descriptor = (Descriptor) pbClazz.getMethod("getDescriptor").invoke(pbClazz);
        List<FieldDescriptor> fields = descriptor.getFields();
        for (FieldDescriptor field : fields) {
            String getMethodName = ReflectUtil.fieldToGetMethod(field.getName());
            if (field.isRepeated()) {
                convertRepeatedProto(pbBuilder, msgObject, msgClazz, pbClazz, field, getMethodName);
            } else {
                convertNonRepeatedProto(pbBuilder, msgObject, msgClazz, pbClazz, field, getMethodName);
            }
        }
    }

    private static void convertNonRepeatedProto(Object pbBuilder, Object msgObject, Class<?> msgClazz, Class<?> pbClazz,
            FieldDescriptor field, String getMethodName) throws Exception {
        Class<?> pbReturnType = pbClazz.getMethod(getMethodName).getReturnType();
        Method pbSetMethod = pbClazz.getMethod(ReflectUtil.convertSet(getMethodName), pbReturnType);
        if (pbReturnType == ProtoMsg.Request.class || pbReturnType == ProtoMsg.Response.class) {
            Object newBuilder = newBuilder(pbReturnType);
            convertPbObject(newBuilder, msgObject);
            pbSetMethod.invoke(pbBuilder, build(newBuilder));
        } else {
            Method msgGetMethod = msgClazz.getMethod(getMethodName);
            Object msgValue = msgGetMethod.invoke(msgObject);
            if (msgValue != null) {
                if (field.getJavaType() == JavaType.MESSAGE) {
                    Class<?> mapPbClass = mapProtoClass(msgGetMethod.getReturnType());
                    Object newBuilder = newBuilder(mapPbClass);
                    convertPbObject(newBuilder, msgValue);
                    msgValue = build(newBuilder);
                }
                pbSetMethod.invoke(pbBuilder, msgValue);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void convertRepeatedProto(Object pbBuilder, Object msgObject, Class<?> msgClazz, Class<?> pbClazz,
            FieldDescriptor field, String getMethodName) throws Exception {
        Method msgGetMethod = msgClazz.getMethod(getMethodName);
        List<Object> list = (List<Object>) msgGetMethod.invoke(msgObject);
        if (!list.isEmpty()) {
            Class<?> mapPbClass = mapProtoClass(list.get(0).getClass());
            Method addMethod = pbClazz.getMethod(ReflectUtil.fieldToMethod("add", field.getName()), mapPbClass);
            for (Object object : list) {
                Object newBuilder = newBuilder(mapPbClass);
                convertPbObject(newBuilder, object);
                addMethod.invoke(pbBuilder, build(newBuilder));
            }
        }
    }

    private static Object newBuilder(Class<?> pbClass) throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        return pbClass.getDeclaredMethod("newBuilder").invoke(pbClass);
    }

    private static Object build(Object pbBuilder) throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        return pbBuilder.getClass().getDeclaredMethod("build").invoke(pbBuilder);
    }

    /**
     * 根据命名规则：从 业务消息类型 -> proto 消息类型
     */
    private static Class<?> mapProtoClass(Class<?> msgClazz) throws ClassNotFoundException {
        String simpleName = msgClazz.getSimpleName();
        String pbClassName = ProtoMsg.class.getName() + "$" + simpleName;
        return Class.forName(pbClassName);
    }

    /**
     * 根据命名规则：从 proto 消息类型 -> 业务消息类型
     */
    private static Class<?> mapMessageClass(Class<?> pbClazz) throws ClassNotFoundException {
        String simpleName = pbClazz.getSimpleName();
        String name = Message.class.getName();
        String pkgName = name.substring(0, name.lastIndexOf("."));
        String className = pkgName + "." + simpleName;
        return Class.forName(className);
    }

    private static void convertMsgObject(Object pbObject, Object msgObject) throws Exception {
        Class<?> pbClazz = pbObject.getClass();
        Class<?> msgClazz = msgObject.getClass();
        Descriptor descriptor = (Descriptor) pbClazz.getMethod("getDescriptor").invoke(pbClazz);
        List<FieldDescriptor> fields = descriptor.getFields();
        for (FieldDescriptor field : fields) {
            if (field.isRepeated()) {
                convertRepeatedMsg(pbObject, msgObject, pbClazz, msgClazz, field);
            } else {
                convertNonRepeatedMsg(pbObject, msgObject, pbClazz, msgClazz, field);
            }
        }
    }

    private static void convertRepeatedMsg(Object pbObject, Object msgObject, Class<?> pbClazz, Class<?> msgClazz,
            FieldDescriptor field) throws Exception {
        String countMethod = ReflectUtil.fieldToMethod("get", field.getName()) + "Count";
        int count = (int) pbClazz.getMethod(countMethod).invoke(pbObject);
        if (count > 0) {
            String getListMethod = ReflectUtil.fieldToMethod("get", field.getName());
            Method getIndexMethod = pbClazz.getMethod(getListMethod, int.class);
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Object protoListObj = getIndexMethod.invoke(pbObject, i);
                if (field.getJavaType() == JavaType.MESSAGE) {
                    Class<?> mapClazz = mapMessageClass(protoListObj.getClass());
                    Object mapObj = mapClazz.newInstance();
                    convertMsgObject(protoListObj, mapObj);
                    list.add(mapObj);
                } else {
                    list.add(protoListObj);
                }
            }
            msgClazz.getMethod(ReflectUtil.convertSet(getListMethod), List.class).invoke(msgObject, list);
        }
    }

    private static void convertNonRepeatedMsg(Object pbObj, Object msgObject, Class<?> pbClazz, Class<?> msgClass,
            FieldDescriptor field) throws Exception {
        String getMethodName = ReflectUtil.fieldToGetMethod(field.getName());
        Method pbGetMethod = pbClazz.getMethod(getMethodName);
        Object pbValue = pbGetMethod.invoke(pbObj);
        Class<?> protoReturnType = pbGetMethod.getReturnType();
        if (field.getJavaType() == JavaType.MESSAGE) {
            if (protoReturnType == ProtoMsg.Request.class || protoReturnType == ProtoMsg.Response.class) {
                convertMsgObject(pbValue, msgObject);
            } else {
                Class<?> msgReturnType = msgClass.getMethod(getMethodName).getReturnType();
                Method setMethod = msgClass.getMethod(ReflectUtil.convertSet(getMethodName), msgReturnType);
                Object toObj = msgReturnType.newInstance();
                convertMsgObject(pbValue, toObj);
                setMethod.invoke(msgObject, toObj);
            }
        } else {
            Method setMethod = msgClass.getMethod(ReflectUtil.convertSet(getMethodName), protoReturnType);
            setMethod.invoke(msgObject, pbValue);
        }
    }

    public static void main(String[] args) {
        LoginResponse response = new LoginResponse(ResultCode.SUCCESS);
        response.setSequence(900);
        response.setSessionId("sessionId");
        response.setCode(0);
        User user0 = new User();
        user0.setName("lisi");
        User user1 = new User();
        user1.setName("wangwu");
        response.addUser(user0);
        response.addUser(user1);
        User user = new User();
        user.setName("zhangsan");
        user.setToken("password");
        response.setUser(user);
        ProtobufSerializer serializer = new ProtobufSerializer();
        byte[] serialize = serializer.serialize(response);
        serializer.deserialize(response.getMessageType().ordinal(), serialize);
    }

}
