package com.lzxx.lojcodesandboxsdk.model;

import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ExecuteCodeResponseStatusEnum {

    // 0 - 通过，1 - 编译错误，2 - 运行错误， 3 - 不安全请求
    PATH_SUCCESS("成功通过", 0),
    COMPILATION_ERROR("编译错误", 1),
    RUN_ERROR("运行错误", 2),
    UNSAFE_REQUEST("不安全请求", 3);

    private final String text;

    private final Integer value;

    ExecuteCodeResponseStatusEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     */
    public static ExecuteCodeResponseStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (ExecuteCodeResponseStatusEnum anEnum : ExecuteCodeResponseStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public Integer getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
