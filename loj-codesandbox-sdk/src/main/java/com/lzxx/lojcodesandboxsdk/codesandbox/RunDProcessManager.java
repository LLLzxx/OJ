package com.lzxx.lojcodesandboxsdk.codesandbox;

import com.lzxx.lojcodesandboxsdk.utils.ProcessUtils;
import com.lzxx.lojcodesandboxsdk.utils.model.InteractProcessUtil;
import com.lzxx.lojcodesandboxsdk.utils.model.NonInteractProcessUtil;


public class RunDProcessManager {

    /**
     * 创建代码沙箱示例
     * @param type 沙箱类型
     */
    public static ProcessUtils newInstance(String type) {
        switch(type) {
            case "non_interact_process":
                return new NonInteractProcessUtil();
            case "interact_process":
                return new InteractProcessUtil();
            case "self_testcases":
                return new InteractProcessUtil();
            default:
                return new NonInteractProcessUtil();
        }
    }
}
