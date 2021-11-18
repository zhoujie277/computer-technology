package com.future.other.protobuf;

import java.util.Arrays;

import com.future.other.protocol.Hello;
import com.future.other.protocol.Hello.ProtocolTypes;
import com.google.protobuf.InvalidProtocolBufferException;

public class ProtocalTypeDemo {

    private void printBinary(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < bytes.length; i++) {
            String binary = String.format("%8s", Integer.toBinaryString(bytes[i] & 0xFF));
            builder.append(binary.replace(' ', '0')).append(" ");
        }
        builder.append("]");
        System.out.println(builder);
    }

    public void run() {
        Hello.ProtocolTypes inst = Hello.ProtocolTypes.newBuilder()
                // .setDoubleV(3.5d)
                // .setInt32V(-1)
                .setLargeseq(100)
                // .setInt64V(888L)
                // .setUint32V(999).setUint64V(111L)
                // .setSint32V(-1)
                //.setSint64V(Integer.MAX_VALUE)
                // .setFixed32V(Integer.MAX_VALUE).setFixed64V(Integer.MAX_VALUE).setBoolv(false).setStringv("未来")
                // .setBytesv(ByteString.copyFrom(("abcd").getBytes()))
                .build();
        System.out.println(inst);
        printBinary(inst.toByteArray());
        System.out.println(Arrays.toString(inst.toByteArray()));
        System.out.println(inst.toByteArray().length);

        ProtocolTypes parseFrom;
        try {
            parseFrom = Hello.ProtocolTypes.parseFrom(inst.toByteArray());
            System.out.println("---------parsefrom-----------");
            System.out.println(parseFrom);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ProtocalTypeDemo().run();
    }

}
