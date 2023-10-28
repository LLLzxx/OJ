package com.lzxx.lojcodesandboxsdk.codesandbox.language.java;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.lzxx.lojcodesandboxsdk.codesandbox.CodeSandbox;
import com.lzxx.lojcodesandboxsdk.model.*;
import com.lzxx.lojcodesandboxsdk.utils.model.NonInteractProcessUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaNativeCodeSandboxOld implements CodeSandbox {
    
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 10000L;

    // 黑白名单限制关键字
    private static final List<String> blackList = Arrays.asList("Files","exec");
    private static final WordTree WORD_TREE;
    static {
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(blackList);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

        // 0.前置准备，基本的数据
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();


        // 1-1 敏感词保护 —— 黑白名单限制
        FoundWord foundWord = WORD_TREE.matchWord(code);
        if(foundWord != null) {
            System.out.println("包含敏感词：" + foundWord.getFoundWord());
            return null;
        }

        // 1-2.存放用户代码
        String userDir = System.getProperty("user.dir");  // 获取用户当前的工作目录路径
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        if(!FileUtil.exist(globalCodePathName)) {  // 判断全局代码是否存在，没有则新建
            FileUtil.mkdir(globalCodePathName);
        }
        // 将用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID(); // 到类的父目录地址
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME; // 类存放的地址
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);


        // 2.编译代码，得到 class 文件
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsoluteFile());
        try {
            ExecuteMessage executeMessage = NonInteractProcessUtil.runProcessAndGetMessage(compileCmd,"编译");
            System.out.println(executeMessage);
        } catch (Exception e) {
            return getErrorResponse(e, ExecuteCodeResponseStatusEnum.COMPILATION_ERROR.getValue());
        }

        // 3.运行代码
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            // 内存保护 —— 但是只能限制JVM的运行时间，而不是整个程序的运行时间。
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -classpath %s Main %s", userCodeParentPath, inputArgs);
            try {
//                ExecuteMessage executeMessage = ProcessUtils.runInterProcessAndGetMessage(runProcess, "运行", inputArgs);
                ExecuteMessage executeMessage = NonInteractProcessUtil.runProcessAndGetMessage(runCmd, "运行");
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                return getErrorResponse(e, ExecuteCodeResponseStatusEnum.RUN_ERROR.getValue());
            }
        }

        // 4.收集整理输出结果
        List<String> outputList = new ArrayList<>();
        // 取用时最大值，判断是否超时
        Long maxTime = 0L;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if(StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 运行错误
                executeCodeResponse.setStatus(ExecuteCodeResponseStatusEnum.RUN_ERROR.getValue());
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if(time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        if(outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(ExecuteCodeResponseStatusEnum.PATH_SUCCESS.getValue());
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
//        judgeInfo.setMemory(); 比较复杂，没有学习的必要
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);

        // 5.文件清理，防止空间不足
        if(userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }
        return executeCodeResponse;
    }

    /**
     * 获取错误响应
     * @param e
     * @return
     */
    private static ExecuteCodeResponse getErrorResponse(Throwable e, Integer status) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(status);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }

    public static void main(String[] args) {
        JavaNativeCodeSandboxOld javaNativeCodeSandbox = new JavaNativeCodeSandboxOld();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2","4 5"));
        String code = ResourceUtil.readStr("testCode.simpleCompute/Main.java",StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
}
