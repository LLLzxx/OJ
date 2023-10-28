package com.lzxx.lojcodesandboxsdk.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.util.List;

public class DockerDemo {
    public static void main(String[] args) throws InterruptedException {
        // 获取默认的 Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

//        PingCmd pingCmd = dockerClient.pingCmd();
//        pingCmd.exec();

        String image = "nginx:latest";
        // 拉取镜像
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
        // 回调参数：异步输出！
        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
            @Override
            public void onNext(PullResponseItem item) {
                super.onNext(item);
            }
        };
        pullImageCmd
                // 执行 （回调参数）
                .exec(pullImageResultCallback)
                // 阻塞，知道镜像下载完成才执行下一步
                .awaitCompletion();
        System.out.println("下载完成");


        // 创建容器
        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image);
        CreateContainerResponse createContainerResponse = createContainerCmd
                .withCmd("echo","Hello Docker")
                .exec();
        System.out.println(createContainerResponse);


        // 查看容器状态
        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
        List<Container> containerList = listContainersCmd.withShowAll(true).exec();
        for (Container container : containerList) {
            System.out.println(container);
        }


        // 启动容器
        String containerId = createContainerResponse.getId();
        dockerClient.startContainerCmd(containerId).exec();


        // 查看日志：可能找不到日志，因为是异步执行，日志可能还没输出
        LogContainerResultCallback logContainerResultCallback = new LogContainerResultCallback() {
            @Override
            public void onNext(Frame item) {
                System.out.println(item.getStreamType());
                System.out.println("日志：" + new String(item.getPayload()));
                super.onNext(item);
            }
        };
        dockerClient.logContainerCmd(containerId)
                .withStdErr(true)
                .withStdOut(true)
                .exec(logContainerResultCallback)
                // 阻塞等待日志输出
                .awaitCompletion();


        // 删除容器
        dockerClient.removeContainerCmd(containerId)
                // 强制删除
                .withForce(true)
                .exec();


        // 删除镜像
        dockerClient.removeConfigCmd(image).exec();
    }
}
