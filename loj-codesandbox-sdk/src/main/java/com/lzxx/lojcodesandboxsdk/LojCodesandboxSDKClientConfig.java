package com.lzxx.lojcodesandboxsdk;

import com.lzxx.lojcodesandboxsdk.client.CodeSandboxClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties("loj.codesandbox")
@ComponentScan
@Configuration
public class LojCodesandboxSDKClientConfig {

    private String accessKey;

    private String secretKey;

    private String securityManagerPath;

    private String securityManagerPathClassName;

    @Bean
    public CodeSandboxClient codeSandboxClient() {
        return new CodeSandboxClient(accessKey, secretKey);
    }


    @Bean
    public CodeSandboxClient codeSandboxClientForSecurity() {
        return new CodeSandboxClient(accessKey, secretKey, securityManagerPath, securityManagerPathClassName);
    }


}
