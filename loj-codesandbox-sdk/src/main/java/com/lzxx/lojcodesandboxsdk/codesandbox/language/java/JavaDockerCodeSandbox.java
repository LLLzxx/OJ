package com.lzxx.lojcodesandboxsdk.codesandbox.language.java;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.lzxx.lojcodesandboxsdk.model.*;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {

    private static final long TIME_OUT = 10000L;

    private static final Boolean FIRST_INIT = true;

    /**
     * 3.运行代码
     * @param model 运行模式
     * @param userCodeFile 代码文件
     * @param inputList 输入用例
     * @return 各个用例对应的运行结果
     */
    @Override
    public List<ExecuteMessage> runFile(String model, File userCodeFile, List<String> inputList) {
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
        // 1）HostConfig用于配置Docker容器的 运行环境 和 网络设置
        HostConfig hostConfig = new HostConfig();
        // 2) 容器挂载目录：在创建文件时，指定文件路径映射，把本地的文件同步到容器中，让容器也可以访问。
        // path - 本地路径；volume - 容器文件路径
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/loj")));
        // 3）安全防护处理
        // 限制内存的使用
        hostConfig.withMemory(100 * 1000 * 1000L);
        // 限制CPU使用的核数
        hostConfig.withCpuCount(1L);
        // 限制内存与硬盘的交换
        hostConfig.withMemorySwap(0L);
        // todo 限制权限 利用Linux内核中的安全管理配置
//        hostConfig.withSecurityOpts(Arrays.asList("seccomp=安全管理配置字符串"));

        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image);
        CreateContainerResponse createContainerResponse = createContainerCmd
                .withHostConfig(hostConfig)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                // 可交互的命令行工具，有一个守护进程，能够读取输入
                .withTty(true)
                // 限制网络资源（刷带宽等行为），直接关闭网络设置
                .withNetworkDisabled(true)
                // 限制用户向 root 根目录写文件的行为
                .withReadonlyRootfs(true)
                .exec();
        System.out.println(createContainerResponse);
        // （4） 启动容器 -- 执行docker指令
        String containId = createContainerResponse.getId();
        dockerClient.startContainerCmd(containId).exec();


        // （5） 整理获取输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        ExecuteMessage executeMessage = new ExecuteMessage();
        // 循环获取输入参数
        for (String inputArgs : inputList) {
            // 1) 将输入参数整理成数组
            String[] inputArgsArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-classpath", "/loj", "Main"}, inputArgsArray);
            // 2) 创建一个终端与容器进行交互
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containId)
                    .withCmd(cmdArray)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();
            System.out.println("创建执行命令：" + executeCodeResponse);

            // 3) 执行终端
            // 内部类参数需要一个相对final的值
//            final String[] message = {null};
//            final String[] errorMessage = {null};
            StringBuilder message = new StringBuilder();
            StringBuilder errorMessage = new StringBuilder();
            final boolean[] isTimeout = {true};

            // 执行命令参数 终端ID
            String execId = execCreateCmdResponse.getId();
            // 执行命令参数 回调参数
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {

                // 在异步操作完成时被调用，无论操作是否成功。在Docker中，这通常用于处理容器执行命令完成后的清理操作或者错误处理。
                // 此处表示：如果超时了就修改 超时标志
                @Override
                public void onComplete() {
                    isTimeout[0] = false;
                    super.onComplete();
                }


                // 用于处理容器的输出流，frame是一个事件帧对象
                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    // 如果执行出错，会输出错误结果
                    if (StreamType.STDERR.equals(streamType)) {
                        // getPayload()方法用于获取该事件帧的有效载荷，也就是事件的具体内容。
                        errorMessage.append(new String(frame.getPayload()));
                        System.out.println("输出错误结果：" + errorMessage.toString());
                    }
                    // 输出正确的结果
                    if (StreamType.STDOUT.equals(streamType)) {
                        String tempStr = new String(frame.getPayload());
                        if (!tempStr.contains("\n")) {
                            message.append(new String(frame.getPayload()));
                            System.out.println("输出结果：" + message.toString());
                        } else {
                            tempStr = tempStr.replaceAll("\\n", "");
                            message.append(tempStr);
                        }
                    }
                    super.onNext(frame);
                }
            };

//            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
//                @Override
//                public void onComplete() {
//                    isTimeout[0] = false;
//                    super.onComplete();
//                }
//                @Override
//                public void onNext(Frame frame) {
//                    StreamType streamType = frame.getStreamType();
//                    if(StreamType.STDERR.equals(streamType)) {
//                        errorMessage[0] = new String(frame.getPayload());
//                        System.out.println("输出错误结果：" + errorMessage[0]);
//                    } else {
//                        message[0] = new String(frame.getPayload());
//                        System.out.println("输出结果：" + message[0]);
//                    }
//                    super.onNext(frame);
//                }
//            };

            // A. 计算时间开销
            StopWatch stopWatch = new StopWatch();
            Long maxTime = 0L;
            // B. 计算内存占用 -- 内存是时刻发生变化的
            final Long[] maxMemory = {0L};
            // StatsCmd用于获取Docker容器统计信息的类，能够实时监控容器的资源使用情况，以便更好地管理和优化你的容器。
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
                        // 限制程序运行时间
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                // 计时结束
                stopWatch.stop();
                maxTime = Math.max(stopWatch.getLastTaskTimeMillis(),maxTime);
                // 关闭内存占用计算
                statsCmd.close();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
            executeMessage.setMessage(message.toString());
            executeMessage.setErrorMessage(errorMessage.toString());
            executeMessage.setTime(maxTime);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }

        return executeMessageList;
    }

    public static void main(String[] args) {

        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2","4 5"));
        String code = ResourceUtil.readStr("testCode.simpleCompute/Main.java",StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");

        JavaDockerCodeSandbox javaDockerCodeSandbox = new JavaDockerCodeSandbox();
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
}
