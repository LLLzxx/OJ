package com.lzxx.lojcodesandboxsdk.model;

import lombok.Data;

/**
 * 进程执行信息
 */
@Data
public class ExecuteMessage {

    /**
     * 执行结果错误码
     *  0 - 正常退出
     *  !0 -- 错误方式退出
     */
    private Integer exitValue;

    /**
     * 标准输出
     */
    private String message;

    /**
     * 标准错误
     */
    private String errorMessage;

    /**
     * 执行时间
     */
    private Long time;

    /**
     * 执行空间
     */
    private Long memory;
}
