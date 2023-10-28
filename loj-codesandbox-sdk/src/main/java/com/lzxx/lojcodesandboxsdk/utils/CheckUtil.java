package com.lzxx.lojcodesandboxsdk.utils;

import cn.hutool.core.util.StrUtil;
import com.lzxx.lojcodesandboxsdk.model.ExecuteCodeRequest;
import com.lzxx.lojcodesandboxsdk.model.ExecuteCodeResponse;
import com.lzxx.lojcodesandboxsdk.model.ExecuteCodeResponseStatusEnum;

import java.util.Date;
import java.util.Map;


public class CheckUtil {

    private static final String AUTH_REQUEST_HEADER = "accessKey";

    private static final String AUTH_REQUEST_SECRET = "secretKey";

    public static ExecuteCodeResponse checkForSafe(ExecuteCodeRequest executeCodeRequest, Map<String, String> clientMap) {

        String authHeader = clientMap.get("accessKey");
        String nonce = clientMap.get("nonce");
        String timestamp = clientMap.get("timestamp");
        String body = clientMap.get("body");
        String sign = clientMap.get("sign");

        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        // ak -sk 鉴权
        // 1.访问密钥
        if(!AUTH_REQUEST_HEADER.equals(authHeader)) {
            executeCodeResponse.setStatus(ExecuteCodeResponseStatusEnum.UNSAFE_REQUEST.getValue());
            executeCodeResponse.setMessage("请求密钥不正确");
            return executeCodeResponse;
        }

        // 防重放 -- 正常来讲 应该从数据库中取数据
        // 2-1.随机数
        if(Long.parseLong(nonce) > 10_000) { // 查看这个随机数是否已经出现过了
            executeCodeResponse.setStatus(ExecuteCodeResponseStatusEnum.UNSAFE_REQUEST.getValue());
            executeCodeResponse.setMessage("请求 随机数 过期");
        }
        // 2-2.时间戳校验 -- 比较时间戳的差值，淘汰过期的随机数
        if (StrUtil.isEmpty(timestamp) || (Long.parseLong(timestamp) - new Date().getTime()) > 120L) {
            executeCodeResponse.setStatus(ExecuteCodeResponseStatusEnum.UNSAFE_REQUEST.getValue());
            executeCodeResponse.setMessage("请求 时间戳 过期");
        }

        // 3.确认密钥是否正确
        String serverSign = SignUtil.getSign(body,AUTH_REQUEST_SECRET);
        if(!sign.equals(serverSign)) {
            executeCodeResponse.setStatus(ExecuteCodeResponseStatusEnum.UNSAFE_REQUEST.getValue());
            executeCodeResponse.setMessage("个人密钥不正确");
        }

        // 4.检查 executeCodeRequest
        if(executeCodeRequest == null) {
            executeCodeResponse.setStatus(ExecuteCodeResponseStatusEnum.UNSAFE_REQUEST.getValue());
            executeCodeResponse.setMessage("参数为空");
        }

        executeCodeResponse.setStatus(ExecuteCodeResponseStatusEnum.PATH_SUCCESS.getValue());
        return executeCodeResponse;
    }
}
