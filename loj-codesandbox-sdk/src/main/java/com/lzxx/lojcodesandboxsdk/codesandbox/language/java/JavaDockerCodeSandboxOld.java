package com.lzxx.lojcodesandboxsdk.codesandbox.language.java;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.lzxx.lojcodesandboxsdk.codesandbox.CodeSandbox;
import com.lzxx.lojcodesandboxsdk.model.*;
import com.lzxx.lojcodesandboxsdk.utils.model.NonInteractProcessUtil;


import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class JavaDockerCodeSandboxOld implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 10000L;

    private static final Boolean FIRST_INIT = true;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

        // 0.前置准备，基本的数据
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();


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
        // （1）获取默认的 Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        // （2）拉取镜像
        String image = "openjdk:8-alpine";
        if(FIRST_INIT) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
        }
        System.out.println("下载成功");

        // （3）创建容器
        // 1）直接再创建的时候就复制，而不是创建后再复制。直接就能查看最开始的赋值状态。
        HostConfig hostConfig = new HostConfig();
        // 2）限制保护处理
        // 限制内存 -- 内存空间限制
        hostConfig.withMemory(100 * 1000 * 1000L);
        // 限制CPU使用的核数
        hostConfig.withCpuCount(1L);
        // 控制 -- 限制内存与硬盘的交换
        hostConfig.withMemorySwap(0L);
        // todo 控制 -- Linux内核的安全管理配置
        hostConfig.withSecurityOpts(Arrays.asList("seccomp=安全管理配置字符串"));
        // 3）path 是本地路径；volume 是容器文件路径；这样能够把本地文件同步到容器中
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/loj")));


        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image);
        CreateContainerResponse createContainerResponse = createContainerCmd
                .withHostConfig(hostConfig)
                // 控制 -- 限制网络
                .withNetworkDisabled(true)
                // 控制 -- 限制用户不能向 root 根目录写文件
                .withReadonlyRootfs(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                // 可交互的命令行工具，有一个守护进程，能够读取输入
                .withTty(true)
                .exec();
        System.out.println(createContainerResponse);
        // 4）启动容器
        String containId = createContainerResponse.getId();
        dockerClient.startContainerCmd(containId).exec();
        // 5）执行命令

        // 4.收集整理输出结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        ExecuteMessage executeMessage = new ExecuteMessage();
        // A.创建命令数组
        for (String inputArgs : inputList) {
            String[] inputArgsArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[] {"java", "-classpath", "/loj", "Main"}, inputArgsArray);
            // B.可交互的容器，接受多次输入与输出
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containId)
                    .withCmd(cmdArray)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();
            System.out.println("创建执行命令：" + executeCodeResponse);
            // C1.执行命令 -- 需要回调参数
            // 内部类需要一个相对final的
            final String[] message = {null};
            final String[] errorMessage = {null};

            // 控制 -- 是否超时
            final boolean[] timeout = {true};
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    timeout[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if(StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };
            // C2.执行命令 -- 需要容器ID
            String execId = execCreateCmdResponse.getId();
            // 计算内存
            final Long[] maxMemory = {0L};
            // 计算用时
            StopWatch stopWatch = new StopWatch();
            Long maxTime = 0L;

            // 计算内存的占用 -- 因为内存的是时刻发生变化的
            StatsCmd statsCmd = dockerClient.statsCmd(containId);
            statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }


                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
            });
            try {
                // 计时开始
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        // 控制运行时间 -- 需要回调函数的 onComplete
                        .awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS);
                // 计时结束
                stopWatch.stop();
                maxTime = stopWatch.getLastTaskTimeMillis();
                statsCmd.close();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(maxTime);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }



        // 4.收集整理输出结果
        List<String> outputList = new ArrayList<>();
        Long maxTime = 0L;
        Long maxMemory = 0L;
        for (ExecuteMessage executeMessageResult : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if(StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 运行错误
                executeCodeResponse.setStatus(ExecuteCodeResponseStatusEnum.RUN_ERROR.getValue());
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            Long memory = executeMessage.getMemory();
            if(time != null) {
                maxTime = Math.max(maxTime, time);
                maxMemory = Math.max(maxMemory,memory);
            }
        }
        if(outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(ExecuteCodeResponseStatusEnum.PATH_SUCCESS.getValue());
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMemory(maxMemory);
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

        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2","4 5"));
        String code = ResourceUtil.readStr("testCode.simpleCompute/Main.java",StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");

        JavaDockerCodeSandboxOld javaDockerCodeSandbox = new JavaDockerCodeSandboxOld();
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
}
