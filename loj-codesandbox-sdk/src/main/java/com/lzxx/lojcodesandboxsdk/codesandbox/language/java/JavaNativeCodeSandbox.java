package com.lzxx.lojcodesandboxsdk.codesandbox.language.java;

import com.lzxx.lojcodesandboxsdk.model.*;
import org.springframework.stereotype.Component;


@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
