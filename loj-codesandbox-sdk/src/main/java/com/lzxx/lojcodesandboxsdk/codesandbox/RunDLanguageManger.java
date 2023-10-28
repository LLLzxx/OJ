package com.lzxx.lojcodesandboxsdk.codesandbox;

import com.lzxx.lojcodesandboxsdk.codesandbox.language.cpp.CppNativeCodeSandbox;


import com.lzxx.lojcodesandboxsdk.codesandbox.language.java.JavaNativeCodeSandbox;
import org.springframework.context.annotation.Configuration;


/**
 * 策略模式
 * 根据不同语言选择不同的 语言沙箱
 */
public class RunDLanguageManger {

    public static CodeSandbox languageController(String language) {
        switch (language) {
            case "java":
                return new JavaNativeCodeSandbox();
            case "cpp":
                return new CppNativeCodeSandbox();
            default:
                return new JavaNativeCodeSandbox();
        }
    }

}
