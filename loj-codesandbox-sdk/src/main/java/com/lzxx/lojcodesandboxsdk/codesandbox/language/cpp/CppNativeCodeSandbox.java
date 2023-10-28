package com.lzxx.lojcodesandboxsdk.codesandbox.language.cpp;

import com.lzxx.lojcodesandboxsdk.model.ExecuteCodeRequest;
import com.lzxx.lojcodesandboxsdk.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;


@Component
public class CppNativeCodeSandbox extends CppCodeSandboxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
