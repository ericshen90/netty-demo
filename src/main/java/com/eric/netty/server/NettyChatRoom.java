package com.eric.netty.server;

import com.google.common.collect.Maps;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Netty聊天室
 *
 * @author EricShen
 * @date 2021-03-17
 */
public class NettyChatRoom {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));


    public static void main(String[] args) {
        // 1.声明线程池
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        // 2.创建服务端引导程序
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        try {
            // 3.设置线程池
            serverBootstrap.group(bossGroup, workerGroup)
                    // 4.设置ServerSocketChannel类型
                    .channel(NioServerSocketChannel.class)
                    // 5.设置参数
                    .option(ChannelOption.SO_BACKLOG, 100)
                    // 6.设置Handler
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 7.编写并设置子Handler
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 此处可添加多个子handler
                            pipeline.addLast(new LoggingHandler(LogLevel.INFO));
                            pipeline.addLast(new NettyChatHandler());
                        }
                    })
            ;
            // 8.绑定端口
            ChannelFuture future = serverBootstrap.bind(PORT).sync();
            // 9.等待服务器端口关闭(此处阻塞主线程)
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 10.优雅关闭线程池
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * 聊天控制器
     *
     * @author EricShen
     * @date 2021-03-17
     */
    @Slf4j
    private static class NettyChatHandler extends SimpleChannelInboundHandler<ByteBuf> {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            log.info("一个连接激活 -> {}", ctx.channel());
            ChatHolder.join((SocketChannel) ctx.channel());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            String msg = new String(bytes, StandardCharsets.UTF_8);
            log.info("读取消息 {}", msg);
            if ("quit\n".equals(msg)) {
                ctx.channel().close();
            } else {
                ChatHolder.propagate((SocketChannel) ctx.channel(), msg);
            }

        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            log.info("一个连接断开 -> {}", ctx.channel());
            ChatHolder.quit((SocketChannel) ctx.channel());
        }
    }

    private static class ChatHolder {
        // 用户map
        private static final Map<SocketChannel, String> USER_MAP = Maps.newConcurrentMap();

        /**
         * 加入聊天室
         *
         * @param socketChannel
         */
        static void join(SocketChannel socketChannel) {
            String userId = "用户_" + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
            send(socketChannel, String.format("你的用户ID是%s\r\n", userId));
            for (SocketChannel channel : USER_MAP.keySet()) {
                send(channel, String.format("%s加入聊天室\r\n", userId));
            }
            USER_MAP.put(socketChannel, userId);
        }

        /**
         * 退出聊天室
         *
         * @param socketChannel
         */
        static void quit(SocketChannel socketChannel) {
            String userId = USER_MAP.get(socketChannel);
            send(socketChannel, "你退出了聊天室\r\n");
            USER_MAP.remove(socketChannel);
            for (SocketChannel channel : USER_MAP.keySet()) {
                send(channel, String.format("%s退出聊天室\r\n", userId));
            }
        }

        /**
         * 扩散说话的内容
         *
         * @param socketChannel
         * @param msg
         */
        static void propagate(SocketChannel socketChannel, String msg) {
            String userId = USER_MAP.get(socketChannel);
            for (SocketChannel channel : USER_MAP.keySet()) {
                send(channel, String.format("%s: %s", userId, msg));
            }
        }


        /**
         * 发送消息
         *
         * @param socketChannel
         * @param msg
         */
        private static void send(SocketChannel socketChannel, String msg) {
            try {
                ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
                ByteBuf byteBuf = allocator.buffer(msg.getBytes().length);
                byteBuf.writeCharSequence(msg, Charset.defaultCharset());
                socketChannel.writeAndFlush(byteBuf);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
