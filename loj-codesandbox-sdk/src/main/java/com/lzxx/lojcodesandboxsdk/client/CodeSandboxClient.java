package com.lzxx.lojcodesandboxsdk.client;


import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.lzxx.lojcodesandboxsdk.codesandbox.CodeSandbox;
import com.lzxx.lojcodesandboxsdk.codesandbox.RunDLanguageManger;

import com.lzxx.lojcodesandboxsdk.model.ExecuteCodeRequest;
import com.lzxx.lojcodesandboxsdk.model.ExecuteCodeResponse;
import com.lzxx.lojcodesandboxsdk.model.ExecuteCodeResponseStatusEnum;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.lzxx.lojcodesandboxsdk.utils.CheckUtil.checkForSafe;
import static com.lzxx.lojcodesandboxsdk.utils.SignUtil.getSign;

public class CodeSandboxClient {

    private String accessKey;

    private String secretKey;

    private String securityManagerPath;

    private String securityManagerPathClassName;

    private final static Gson GSON = new Gson();


    private Map<String ,String> getHeaderMap(Integer nonce, Long timestamp, String body) {
        Map<String,String> hashMap = new HashMap<>();
        hashMap.put("accessKey",accessKey);
//        hashMap.put("secretKey",secretKey);  一定不能从前端发送给后端！！！
        hashMap.put("nonce", String.valueOf(nonce));
        hashMap.put("timestamp",String.valueOf(timestamp));
        hashMap.put("body",body);
        hashMap.put("sign",getSign(body, secretKey));
        return hashMap;
    }


    public CodeSandboxClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public CodeSandboxClient(String accessKey, String secretKey, String securityManagerPath, String securityManagerPathClassName) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.securityManagerPath = securityManagerPath;
        this.securityManagerPathClassName = securityManagerPathClassName;
    }


    public String judgeQuestion(ExecuteCodeRequest executeCodeRequest, Integer nonce, Long timestamp) {
        // 密钥校验 + 重放攻击校验 + 非空校验
        String json = JSONUtil.toJsonStr(executeCodeRequest);
        Map<String, String> clientMap = this.getHeaderMap(nonce, timestamp, json);
        ExecuteCodeResponse executeCodeResponse = checkForSafe(executeCodeRequest, clientMap);
        if(executeCodeResponse.getStatus().equals(ExecuteCodeResponseStatusEnum.UNSAFE_REQUEST.getValue())) {
            return GSON.toJson(executeCodeResponse);
        }

        // 根据不同语言，不同模式 --> 执行不同的代码沙箱
        String language = executeCodeRequest.getLanguage();
        CodeSandbox codeSandbox = RunDLanguageManger.languageController(language);
        executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        return GSON.toJson(executeCodeResponse);

//        String url = "http://localhost:8090/executeCode";
//        String json = JSONUtil.toJsonStr(executeCodeRequest);
//
//        HttpResponse httpResponse = HttpUtil.createPost(url)
//                .charset(StandardCharsets.UTF_8) // 因为传来的可能是中文，那就可能会乱码，所以要指定 字符集
//                .addHeaders(this.getHeaderMap(json))
//                .body(json)
//                .execute();
//
//        System.out.println(httpResponse.getStatus());
//        String result = httpResponse.body();
//        System.out.println(result);
//        return result;
    }
}
