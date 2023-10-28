package com.lzxx.lojcodesandboxsdk.codesandbox;

import com.lzxx.lojcodesandboxsdk.model.ExecuteCodeRequest;
import com.lzxx.lojcodesandboxsdk.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口定义
 */
public interface CodeSandbox {

    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
