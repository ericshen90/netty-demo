package com.eric.netty.io;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

/**
 * Java BIO
 *
 * @author EricShen
 * @date 2021-03-16
 */
@Slf4j
public class BioEchoServer {


    public static void main(String[] args) throws IOException {
        // 启动服务端,绑定端口
        ServerSocket serverSocket = new ServerSocket(8001);
        log.info("start server");
        while (true) {
            // 接收服务端连接
            Socket socket = serverSocket.accept();

            log.info("on client conn: {}", socket);

            // 新建线程处理连接
            new Thread(() -> {
                try {
                    // 读取数据
                    InputStream inputStream = socket.getInputStream();

                    InputStreamReader bufferedInputStream = new InputStreamReader(inputStream);

                    BufferedReader bufferedReader = new BufferedReader(bufferedInputStream);

                    String msg;

                    while (Objects.nonNull((msg = bufferedReader.readLine()))) {
                        // 打印内容
                        log.info("received msg: {}", msg);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }).start();


        }

    }

}
