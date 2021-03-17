package com.eric.netty.io;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO Server
 *
 * @author EricShen
 * @date 2021-03-17
 */
@Slf4j
public class NioEchoServer {

    public static void main(String[] args) throws IOException {
        // 创建一个Server
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        // 绑定端口
        serverSocketChannel.bind(new InetSocketAddress(8002));
        // 设为非阻塞
        serverSocketChannel.configureBlocking(false);
        // 创建一个Selector(IO 多路复用)
        Selector selector = Selector.open();
        // 绑定selector,设置accept模式
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        log.info("start server");

        while (true) {
            // select阻塞(第一阶段阻塞)
            selector.select();
            // 如果使用的是select(timeout)或selectNow()需要判断返回值是否大于0

            // 获取到就绪的key
            Set<SelectionKey> selectionKeySet = selector.selectedKeys();
            // 使用迭代器遍历
            Iterator<SelectionKey> iterator = selectionKeySet.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {// accept事件
                    // 强转为ServerSocketChannel
                    ServerSocketChannel socketChannel = (ServerSocketChannel) key.channel();
                    // accept连接
                    SocketChannel channel = socketChannel.accept();
                    log.info("accept remote conn: {}", channel.getRemoteAddress());
                    // 设为非阻塞
                    channel.configureBlocking(false);
                    // 以read模式绑定selector
                    channel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {// read事件
                    // 强转为SocketChannel
                    SocketChannel channel = (SocketChannel) key.channel();
                    // 读取数据用的buffer
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    // 数据读取到buffer中(第二个阶段阻塞)
                    int length = channel.read(buffer);
                    if (length > 0) {
                        buffer.flip();
                        byte[] byteArray = new byte[buffer.remaining()];
                        // 数据加载到byte数组中
                        buffer.get(byteArray);
                        // 替换换行符
                        String msg = new String(byteArray, StandardCharsets.UTF_8).replace("\r\n", "");
                        log.info("receive msg: {}", msg);
                    }
                }
                iterator.remove();
            }


        }


    }
}
