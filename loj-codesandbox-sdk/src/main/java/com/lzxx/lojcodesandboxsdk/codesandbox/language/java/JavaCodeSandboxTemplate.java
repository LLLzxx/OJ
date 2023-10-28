package com.lzxx.lojcodesandboxsdk.codesandbox.language.java;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.lzxx.lojcodesandboxsdk.codesandbox.CodeSandbox;
import com.lzxx.lojcodesandboxsdk.codesandbox.RunDProcessManager;
import com.lzxx.lojcodesandboxsdk.model.*;
import com.lzxx.lojcodesandboxsdk.utils.ProcessUtils;
import com.lzxx.lojcodesandboxsdk.utils.model.NonInteractProcessUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        // 0.前置准备，基本的数据
        String model = executeCodeRequest.getModel();
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();


        // 1.存放 与 获取用户代码
        File userCodeFile = sandCodeToFile(code);

        // 2.【编译代码】，得到 class 文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        if(compileFileExecuteMessage.getExitValue() != 0) {
            deleteFile(userCodeFile);
            return getErrorResponse(compileFileExecuteMessage);
        }

        // 3.编译成功，则【运行代码】
        List<ExecuteMessage> executeMessageList = runFile(model, userCodeFile, inputList);


        // 4.收集整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(userCodeFile, executeMessageList);

        // 5.文件清理
        boolean flag = deleteFile(userCodeFile);
        if(!flag) {
            log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsoluteFile());
        }
        return outputResponse;
    }

    /**
     * 1.把用户的代码保存为文件
     * @param code 提交代码
     * @return 返回类所在的地址
     */
    public File sandCodeToFile(String code) {
        // 获取用户当前的工作目录路径
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码是否存在，没有则新建
        if(!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 将用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID(); // 到类的父目录地址
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME; // 类存放的地址
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        return userCodeFile;
    }


    /**
     * 2-1.编译代码得到文件
     * @param userCodeFile 代码做在文件
     * @return 进程执行信息
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsoluteFile());
        try {
            ExecuteMessage executeMessage = NonInteractProcessUtil.runProcessAndGetMessage(compileCmd,"编译");
            return executeMessage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 2-2.获取代码 编译错误 响应
     * @param executeMessage 进程执行信息
     * @return 编译结果
     */
    private static ExecuteCodeResponse getErrorResponse(ExecuteMessage executeMessage) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setStatus(ExecuteCodeResponseStatusEnum.COMPILATION_ERROR.getValue());
        executeCodeResponse.setMessage(executeMessage.getErrorMessage());
        return executeCodeResponse;
    }

    /**
     * 3.运行代码
     * @param userCodeFile 主文件
     * @param inputList 参数列表
     * @return 返回运行结果
     */
    public List<ExecuteMessage> runFile(String model, File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        // 根据不同参数执行不同模式
        ProcessUtils processUtils = RunDProcessManager.newInstance(model);
        List<ExecuteMessage> executeMessageList = processUtils.runFile(userCodeParentPath, inputList);
        return executeMessageList;
    }


    /**
     * 4.获取代码 运行输出结果
     * @param userCodeFile 代码文件
     * @param executeMessageList 结果列表
     * @return 返回执行响应参数
     */
    public ExecuteCodeResponse getOutputResponse(File userCodeFile, List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();

        // 输出结果
        List<String> outputList = new ArrayList<>();
        // 取最大占用时间
        long maxTime = 0;
        // 取最大占用空间
        long maxMemory = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if(StrUtil.isNotBlank(errorMessage)) { // 运行错误
                // 执行状态
                executeCodeResponse.setStatus(ExecuteCodeResponseStatusEnum.RUN_ERROR.getValue());
                // 接口信息
                executeCodeResponse.setMessage(errorMessage);
                // 删除文件
                deleteFile(userCodeFile);
                break;
            }
            // 输出结果
            outputList.add(executeMessage.getMessage());
            // 判题信息 -- 时间
            Long time = executeMessage.getTime();
            if(null != time) {
                maxTime = Math.max(maxTime, time);
            }
            // 判题信息 -- 空间
            Long memory = executeMessage.getMemory();
            if(null != memory) {
                maxMemory = Math.max(maxMemory,memory);
            }
        }
        // 输出结果
        executeCodeResponse.setOutputList(outputList);
        // 执行状态
        if(outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(ExecuteCodeResponseStatusEnum.PATH_SUCCESS.getValue());
        }
        // 整理判题信息
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);
        executeCodeResponse.setJudgeInfo(judgeInfo);

        return executeCodeResponse;
    }


    /**
     * 5.删除文件
     * @param userCodeFile 代码所在文件
     * @return 是否删除成功
     */
    public boolean deleteFile(File userCodeFile) {
        if(userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }


}
