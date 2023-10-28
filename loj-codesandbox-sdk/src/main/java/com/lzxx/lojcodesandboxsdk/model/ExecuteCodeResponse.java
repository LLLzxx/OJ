package com.lzxx.lojcodesandboxsdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder // 构造器写法
@NoArgsConstructor // 生成无参构造方法
@AllArgsConstructor // 生成所有参数都有的构造方法
public class ExecuteCodeResponse {

    /**
     * 执行状态
     */
    private Integer status;

    /**
     * 接口信息
     */
    private String message;

    /**
     * 输出结果
     */
    private List<String> outputList;

    /**
     * 判题信息
     *  程序执行信息
     *  消耗内存
     *  消耗时间（KB）
     */
    private JudgeInfo judgeInfo;
}
