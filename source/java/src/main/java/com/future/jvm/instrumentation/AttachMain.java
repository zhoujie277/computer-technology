package com.future.jvm.instrumentation;

import com.sun.tools.attach.VirtualMachine;

public class AttachMain {

    public static void main(String[] args) throws Exception {
        VirtualMachine vm = null;
        try {
            System.out.println("vm main:" + args[0]);
            vm = VirtualMachine.attach(args[0]);
            vm.loadAgent("/Users/jayzhou/work/owner/code/github/computer-technology/source/java/target/hello-1.0-SNAPSHOT.jar");
        } finally {
            if (vm != null)
                vm.detach();
        }
    }
}
