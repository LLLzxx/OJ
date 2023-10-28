package com.lzxx.lojcodesandboxsdk.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    /**
     * 获取某个流的输出
     * @param inputStream 输入流
     * @return 输出结果
     * @throws IOException 异常
     */
    public static String getProcessOutput(InputStream inputStream) throws IOException {
        // （1）分批获取进程输出 -- 获取标准错误 or 标准输出
        // 分批获取进程输出 -- 获取控制台的信息
        // InputStreamReader -- 输入流
        // BufferReader -- 一块一块的发送

        // Linux写法
         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        // Windows写法
//        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "GBK"));

        // （2）逐行读取
        List<String> outputStrList = new ArrayList<>();
        String outputLine;
        while ((outputLine = bufferedReader.readLine()) != null) {
            outputStrList.add(outputLine);
        }
        bufferedReader.close();
        return outputStrList.toString();
    }
}