package com.future.basis;

import java.lang.reflect.Method;

/**
 * 反射分析
 *
 * @author future
 */
public class ReflectionAnalyzer {
    private static int count = 0;

    public static void foo() {
        new Exception("foo#" + (count++)).printStackTrace();
    }

    /**
     * * java.lang.Exception: foo#15                                                                    // 第 16 次
     * * 	at com.future.basis.ReflectionAnalyzer.foo(ReflectionAnalyzer.java:14)
     * * 	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
     * * 	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)            // 此处是 NativeMethodAccessorImpl.invoke
     * * 	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
     * * 	at java.lang.reflect.Method.invoke(Method.java:498)
     * * 	at com.future.basis.ReflectionAnalyzer.main(ReflectionAnalyzer.java:21)
     * * java.lang.Exception: foo#16                                                                    // 第 17 次
     * *	at com.future.basis.ReflectionAnalyzer.foo(ReflectionAnalyzer.java:14)
     * *	at sun.reflect.GeneratedMethodAccessor1.invoke(Unknown Source)                              // 此处已经是 GeneratedMethodAccessor1.invoke
     * *	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
     * * 	at java.lang.reflect.Method.invoke(Method.java:498)
     * * 	at com.future.basis.ReflectionAnalyzer.main(ReflectionAnalyzer.java:21)
     * <p>
     * 上面的日志说明
     * <p>
     * 在 NativeMethodAccessorImpl 中判断了一个默认阈值 ReflectionFactory.inflationThreshold = 15
     * <p>
     * 代码如下。
     * *    public Object invoke(Object var1, Object[] var2) throws IllegalArgumentException, InvocationTargetException {
     * *        if (++this.numInvocations > ReflectionFactory.inflationThreshold() && !ReflectUtil.isVMAnonymousClass(this.method.getDeclaringClass())) {
     * *            MethodAccessorImpl var3 = (MethodAccessorImpl)(new MethodAccessorGenerator()).generateMethod(this.method.getDeclaringClass(), this.method.getName(), this.method.getParameterTypes(), this.method.getReturnType(), this.method.getExceptionTypes(), this.method.getModifiers());
     * *             this.parent.setDelegate(var3);
     * *        }
     * *
     * *        return invoke0(this.method, var1, var2);
     * *   }
     */
    public static void main(String[] args) throws Exception {
        Class<?> clz = Class.forName("com.future.basis.ReflectionAnalyzer");
        Method method = clz.getMethod("foo");
        for (int i = 0; i < 20; i++) {
            method.invoke(null);
        }
        System.in.read();
    }
}
