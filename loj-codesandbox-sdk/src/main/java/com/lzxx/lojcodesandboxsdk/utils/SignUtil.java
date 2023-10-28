package com.lzxx.lojcodesandboxsdk.utils;

import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;

/**
 * 签名生成算法
 */
public class SignUtil {

    public static String getSign(String body, String secretKey) {
        String content = body + "." + secretKey;
        Digester md5 = new Digester(DigestAlgorithm.SHA256);
        return md5.digestHex(content);
    }
}


