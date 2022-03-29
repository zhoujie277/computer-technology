package com.future.jvm;

import com.future.bean.Person;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

/**
 * 破坏双亲委派加载的示例
 *
 * @author future
 */
@Slf4j
public class CustomClassLoader extends ClassLoader {

    CustomClassLoader() {
        super(getSystemClassLoader());
        registerAsParallelCapable();
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // 如果不是自定义的类，委托给父类加载。
            if (!name.startsWith("com.future")) return super.loadClass(name);
            // 是自定义的类，有自己的类加载器加载。
            return findClass(name);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            String filename = name.replace(".", "/") + ".class";
            // 注意，在该示例中。该类加载器类本身是由应用类加载器加载的。
            InputStream in = getResourceAsStream(filename);
            if (in == null) throw new ClassNotFoundException(name);
            byte[] b = new byte[in.available()];
            int read = in.read(b);
            return defineClass(name, b, 0, read);
        } catch (IOException e) {
            throw new ClassNotFoundException(name);
        }
    }

    public static void main(String[] args) {
        // 当前线程所在的类加载器就是系统类加载器
        log.debug("default Thread.currentThread().getContextClassLoader() == getSystemClassLoader()：{}", Thread.currentThread().getContextClassLoader() == getSystemClassLoader());
        CustomClassLoader customClassLoader = new CustomClassLoader();
        Thread.currentThread().setContextClassLoader(customClassLoader);
        log.debug("Thread's contextClassLoader was set to CustomClassLoader now.");
        log.debug("now Thread.currentThread().getContextClassLoader() == getSystemClassLoader()：{}", Thread.currentThread().getContextClassLoader() == getSystemClassLoader());
        try {
            Class<?> aClass = customClassLoader.loadClass("com.future.bean.Person");
            // Person.class 是由 AppClassLoader 来加载的。
            // aClass 是由自定义的类加载加载的。
            log.debug("Person's class loaded by CustomClassLoader == Person.class by SystemClassLoader ? {}", aClass == Person.class);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
