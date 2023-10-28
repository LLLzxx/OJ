package com.lzxx.lojcodesandboxsdk.utils.model;

import cn.hutool.core.util.StrUtil;
import com.lzxx.lojcodesandboxsdk.utils.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StopWatch;
import com.lzxx.lojcodesandboxsdk.model.ExecuteMessage;
import com.lzxx.lojcodesandboxsdk.utils.ProcessUtils;
import java.io.*;

import java.util.ArrayList;
import java.util.List;


public class InteractProcessUtil implements ProcessUtils {

    private static final long TIME_OUT = 10000L;


    @Override
    public List<ExecuteMessage> runFile(String userCodeParentPath, List<String> inputList) {
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String input : inputList) {
            // Linux下的命令
             String runCmd = String.format("/software/jdk1.8.0_301/bin/java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main", userCodeParentPath);

            // Windows下的命令
            // String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main", userCodeParentPath, securityManagerPath, securityManagerPathClassName);
//            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main", userCodeParentPath);

            try {
                ExecuteMessage executeMessage = InteractProcessUtil.runInterProcessAndGetMessage(runCmd, "运行", input);
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return executeMessageList;
    }


    /**
     * 执行交互式进程 并 获取信息
     * @param cmd Java原生指令
     * @param opName 执行信息：编译 or 运行
     * @param input 执行参数
     * @return 返回信息
     */
    public static ExecuteMessage runInterProcessAndGetMessage(String cmd, String opName, String input) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        // 1.执行原生指令
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            // 1.执行原生指令
            Process runProcess = Runtime.getRuntime().exec(cmd);
            // 超时控制
            Thread monitorThread = new Thread(() -> {
                if (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println(opName + "超时，立即中断！");
                        runProcess.destroy();
                        System.out.println(opName + "线程中断成功！");
                    } catch (InterruptedException e) {
                        System.out.println(opName + "监控线程取消成功！");
                    }
                }
            });
            monitorThread.start();


            // 2.在控制台输入
//            OutputStream outputStream = runProcess.getOutputStream();
//            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
//            // 拼接输入的内容
//            String[] str = input.split(" ");
//            String join = StrUtil.join("\n", (Object) str) + "\n";
//            outputStreamWriter.write(join);
//            // 相当于回车，执行输入的发送
//            outputStreamWriter.flush();

            // （1）初始化打印参数
            StringReader inputReader = new StringReader(input);
            BufferedReader inputBufferedReader = new BufferedReader(inputReader);
            PrintWriter consoleInput = new PrintWriter(runProcess.getOutputStream());
            String line;
            while ((line = inputBufferedReader.readLine()) != null) {
                consoleInput.println(line);
                consoleInput.flush();
            }
            consoleInput.close();


            // 3.获取输出：等待程序执行，获取错误码
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);

            // 3-1.执行失败
            if (0 != exitValue) {
                System.out.println(opName + "失败，错误码：" + exitValue);
                executeMessage.setErrorMessage(StringUtils.join(FileUtil.getProcessOutput(runProcess.getErrorStream()), "\n"));
            } else { // 3-2.执行成功
                System.out.println(opName + "成功！");
                monitorThread.interrupt();

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream(), "GBK"));
                StringBuilder outputLineStringBuilder = new StringBuilder();
                String outputLine;
                while ((outputLine = bufferedReader.readLine()) != null) {
                    outputLineStringBuilder.append(outputLine);
                }
                executeMessage.setMessage(outputLineStringBuilder.toString());
            }

            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return executeMessage;

    }
}