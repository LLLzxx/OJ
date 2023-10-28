package com.lzxx.lojcodesandboxsdk.utils;

import com.lzxx.lojcodesandboxsdk.model.ExecuteMessage;

import java.util.List;

public interface ProcessUtils {

    List<ExecuteMessage> runFile(String userCodeParentPath, List<String> inputList);
}
