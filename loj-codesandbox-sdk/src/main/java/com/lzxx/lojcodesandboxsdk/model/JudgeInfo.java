package com.lzxx.lojcodesandboxsdk.model;

import lombok.Data;

/**
 * 判题信息
 */
@Data
public class JudgeInfo {

    /**
     * 程序执行结果 -- 枚举中的value
     */
    private String result;

    /**
     * 错误信息 -- 执行之后JVM的报错
     */
    private String message;

    /**
     * 消耗时间
     */
    private Long time;

    /**
     * 消耗内存（KB）
     */
    private Long memory;

}
