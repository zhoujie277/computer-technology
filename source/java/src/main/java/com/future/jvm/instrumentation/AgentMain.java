package com.future.jvm.instrumentation;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

@SuppressWarnings("all")
public class AgentMain {

    /**
     * 通过 java -javaagent:/path/to/agent.jar InstrumentationMain 使用
     */
    public static void premain(String agentArgs, Instrumentation inst) throws ClassNotFoundException, UnmodifiableClassException {
        System.out.println("premain agentArgs:" + agentArgs);
        inst.addTransformer(new InjectLogClassFileTransformer(), true);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) throws ClassNotFoundException, UnmodifiableClassException {
        System.out.println("agentmain agentArgs:" + agentArgs);
        inst.addTransformer(new InjectLogClassFileTransformer(), true);
        Class[] allLoadedClasses = inst.getAllLoadedClasses();
        for (Class clazz : allLoadedClasses) {
            if (clazz.getName().equals("com.future.jvm.instrumentation.InstrumentationMain")) {
                System.out.println(clazz.getName());
                System.out.println("==============");
                inst.retransformClasses(clazz);
                break;
            }
        }
    }
}
