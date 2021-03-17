package com.eric.netty.io;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * NIO聊天室
 *
 * @author EricShen
 * @date 2021-03-17
 */
@Slf4j
public class NioChatRoom {

    public static void main(String[] args) throws IOException {
        log.info("start chat room");
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8004));
        serverSocketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        log.info("open chat room");
        while (true) {
            selector.select();//阻塞在此
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                if (selectionKey.isAcceptable()) {
                    ServerSocketChannel socketChannel = (ServerSocketChannel) selectionKey.channel();
                    SocketChannel channel = socketChannel.accept();
                    log.info("accept new conn: {}", channel.getRemoteAddress());
                    channel.configureBlocking(false);
                    channel.register(selector, SelectionKey.OP_READ);
                    ChatHolder.join(channel);
                } else if (selectionKey.isReadable()) {
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int length = socketChannel.read(byteBuffer);
                    if (length > 0) {
                        byteBuffer.flip();
                        byte[] bytes = new byte[byteBuffer.remaining()];
                        byteBuffer.get(bytes);
                        String msg = new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "");
                        if ("quit".equalsIgnoreCase(msg)) {
                            ChatHolder.quit(socketChannel);
                            selectionKey.cancel();
                            socketChannel.close();
                        } else {
                            ChatHolder.propagate(socketChannel, msg);
                        }

                    }
                }
                iterator.remove();
            }
        }


    }


    private static class ChatHolder {
        //用户map
        private static final Map<SocketChannel, String> USER_MAP = Maps.newConcurrentMap();

        /**
         * 加入群聊
         *
         * @param socketChannel
         */
        public static void join(@NotNull SocketChannel socketChannel) {
            // 随机生成用户id
            String userId = "user_" + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
            // 告知用户id
            send(socketChannel, String.format("your userId is %s\r\n", userId));
            // 群发xxx加入群聊
            sendAll(String.format("userId:%s joined the chatting\r\n", userId));
            // 加入到用户map中
            USER_MAP.put(socketChannel, userId);
        }

        public static void propagate(@NotNull SocketChannel socketChannel, @NotNull String msg) {
            Optional.of(USER_MAP.get(socketChannel)).ifPresent(userId -> sendAll(String.format("%s: %s \r\n", userId, msg)));
        }

        /**
         * 离开群聊
         *
         * @param socketChannel
         */
        public static void quit(@NotNull SocketChannel socketChannel) {
            String userId = USER_MAP.get(socketChannel);
            send(socketChannel, String.format("%s you quit the chat room\r\n", userId));
            USER_MAP.remove(socketChannel);
            sendAll(String.format("%s quit the chat room\r\n", userId));
        }

        /**
         * 发送消息
         *
         * @param socketChannel
         * @param msg
         */
        private static void send(@NotNull SocketChannel socketChannel, @NotNull String msg) {
            try {
                ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
                writeBuffer.put(msg.getBytes(StandardCharsets.UTF_8));
                writeBuffer.flip();
                log.info("send msg:{}", msg);
                socketChannel.write(writeBuffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * 群发消息
         *
         * @param msg
         */
        private static void sendAll(String msg) {
            if (!CollectionUtils.isEmpty(USER_MAP.keySet())) {
                for (SocketChannel channel : USER_MAP.keySet()) {
                    if (Objects.nonNull(channel)) {
                        send(channel, msg);
                    }
                }
            }
        }


    }


}
