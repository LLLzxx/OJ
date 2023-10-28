package com.lzxx.lojcodesandboxsdk.codesandbox.language.cpp;

import com.lzxx.lojcodesandboxsdk.codesandbox.CodeSandbox;
import com.lzxx.lojcodesandboxsdk.model.ExecuteCodeRequest;
import com.lzxx.lojcodesandboxsdk.model.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class CppCodeSandboxTemplate implements CodeSandbox {
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("执行CPP代码沙箱");
        return null;
    }
}