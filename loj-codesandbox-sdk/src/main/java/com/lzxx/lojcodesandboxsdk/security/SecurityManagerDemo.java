package com.lzxx.lojcodesandboxsdk.security;

import java.security.Permission;

/**
 * 保护：默认安全管理器
 *   能够限制用户对文件、内存、CPU、网络 等资源的操作和访问。
 *   是 Java 保护 JVM、Java安全的机制，可以实现更加严格的资源和操作限制
 */
public class SecurityManagerDemo extends SecurityManager {


    // 对所有权限的限制
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不做任何限制");
        System.out.println(perm);
        super.checkPermission(perm); // 可执行所有权限
        throw new SecurityException("权限不足" + perm.getActions()); // 拒绝所有权限
    }


    // 可执行的情况就是直接 supeer()  ;  不可执行就是抛异常
    // 程序是否可执行
    @Override
    public void checkExec(String cmd) {
        super.checkExec(cmd);
    }

    // 程序是否可读：必须一个个去限制
    @Override
    public void checkRead(String file, Object context) {
        if(file.contains("cdscwd")) { // 可读文件，需要一个个添加
            return;
        }
        super.checkRead(file, context);
    }

    // 程序是否可写
    @Override
    public void checkWrite(String file) {
        super.checkWrite(file);
    }

    // 程序是否可删除
    @Override
    public void checkDelete(String file) {
        super.checkDelete(file);
    }


    // 程序是否可连接网络
    @Override
    public void checkConnect(String host, int port) {
        super.checkConnect(host, port);
    }
}
