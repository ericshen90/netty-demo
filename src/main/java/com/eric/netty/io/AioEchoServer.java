package com.eric.netty.io;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * AIO Server
 *
 * @author EricShen
 * @date 2021-03-17
 */
@Slf4j
public class AioEchoServer {


    public static void main(String[] args) throws IOException {
        // 开启异步server
        AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open();
        // 绑定端口
        serverSocketChannel.bind(new InetSocketAddress(8003));
        log.info("start server");

        // accept事件,完全异步,非阻塞
        serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {

            @Override
            public void completed(AsynchronousSocketChannel socketChannel, Object attachment) {
                try {
                    log.info("accept new conn: {}", socketChannel.getRemoteAddress());
                    // 继续监听accept事件
                    serverSocketChannel.accept(null, this);
                    // 处理消息
                    while (true) {
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        // 数据放入buffer中
                        Future<Integer> read = socketChannel.read(byteBuffer);
                        if (read.get() > 0) {
                            byteBuffer.flip();
                            byte[] byteArr = new byte[byteBuffer.remaining()];
                            // 数据读入到byte数组中
                            byteBuffer.get(byteArr);
                            String str = new String(byteArr, StandardCharsets.UTF_8.name());
                            // 换行符当成另一条消息传过来
                            if ("\r\n".equals(str)) {
                                continue;
                            }
                            log.info("receive msg: {}", str);
                        }

                    }
                } catch (IOException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                log.info("failed");
            }
        });
        // 阻塞主线程
        System.in.read();
    }

}
