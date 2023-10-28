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
public class ExecuteCodeRequest {

    /**
     * 运行模式：
     *   non_interact_process - 非交互模式，
     *   interact_process - 交互模式，
     *   self_testcases - 自测用例模式
     */
    private String model;

    /**
     * 传入代码
     */
    private String code;

    private List<String> inputList;

    /**
     * 使用语言
     */
   private String language;
}
