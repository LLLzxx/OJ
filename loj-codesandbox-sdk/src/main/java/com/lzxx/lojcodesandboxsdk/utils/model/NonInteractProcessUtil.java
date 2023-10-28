package com.lzxx.lojcodesandboxsdk.utils.model;

import com.lzxx.lojcodesandboxsdk.model.ExecuteMessage;
import com.lzxx.lojcodesandboxsdk.utils.FileUtil;
import com.lzxx.lojcodesandboxsdk.utils.ProcessUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class NonInteractProcessUtil implements ProcessUtils {

    private static final long TIME_OUT = 10000L;

    private String SECURITY_MANAGER_PATH;

    private String SECURITY_MANAGER_CLASS_NAME;


    @Override
    public List<ExecuteMessage> runFile(String userCodeParentPath, List<String> inputList) {
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            // Linux下的命令
            String runCmd = String.format("/software/jdk1.8.0_301/bin/java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);

            // Windows下命令
            // String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);

            try {
                ExecuteMessage executeMessage = NonInteractProcessUtil.runProcessAndGetMessage(runCmd, "运行");
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return executeMessageList;

    }


    /**
     * 执行进程 并 获取信息
     * @param cmd Java原生指令
     * @param opName 执行信息：编译 或 运行
     * @return 返回信息
     */
    public static ExecuteMessage runProcessAndGetMessage(String cmd, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            // 1.执行原生指令
            Process runProcess = Runtime.getRuntime().exec(cmd);
            // 超时控制
            Thread monitorThread = new Thread(() -> {
                if(!Thread.currentThread().isInterrupted()) {
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

            // 2.等待程序执行，获取错误码
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);

            // 3-1.执行失败
            if(0 != exitValue) {
                System.out.println(opName + "失败，错误码：" + exitValue);
                executeMessage.setErrorMessage(StringUtils.join(FileUtil.getProcessOutput(runProcess.getErrorStream()),"\n"));
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
//                executeMessage.setMessage(FileUtil.getProcessOutput(runProcess.getInputStream()));
//                executeMessage.setMessage(StringUtils.join(FileUtil.getProcessOutput(runProcess.getInputStream()),"\n"));
            }

            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return executeMessage;
    }


}
