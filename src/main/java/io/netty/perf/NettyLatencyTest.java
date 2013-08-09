package io.netty.perf;
/*
 * Copyright 2011 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.perf.collection.Histogram;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public abstract class NettyLatencyTest {
    public final static int ECHO_FRAME_SIZE = 8;
    public final static int LAST_PING = 0xBABE;

    private final Histogram currentHistogram;

    protected final LatencyClientMeter clientMeter = new LatencyClientMeter();
    protected final LatencyServerMeter serverMeter = new LatencyServerMeter();


    private ServerBootstrap sb;
    private Bootstrap cb;
    private final CountDownLatch latch = new CountDownLatch(2);

    public NettyLatencyTest(long[] latencyIntervals) {
        currentHistogram = new Histogram(latencyIntervals);
    }

    public void setup() {

        sb = mkServerBootStrap();

        cb = mkClientBootStrap();

        Channel sc = sb.
                channel(serverChannel()).
                localAddress(new InetSocketAddress("127.0.0.1", 0)).
                bind().syncUninterruptibly().channel();

        int port = ((InetSocketAddress) sc.localAddress()).getPort();

        cb.channel(clientChannel()).
                remoteAddress(new InetSocketAddress("127.0.0.1", port)).
                connect().syncUninterruptibly();
    }

    public abstract ServerBootstrap mkServerBootStrap();

    public abstract Bootstrap mkClientBootStrap();


    public abstract Class<? extends Channel> clientChannel();


    public abstract Class<? extends ServerChannel> serverChannel();

    public Histogram execute(int count) throws InterruptedException {


        for (int i = 0; i < count; i++) {
            ByteBuf buffer = Unpooled.buffer(ECHO_FRAME_SIZE);
            buffer.writeLong(System.nanoTime());
            clientMeter.write(buffer);
        }

        ByteBuf last = Unpooled.buffer(ECHO_FRAME_SIZE);
        last.writeLong(LAST_PING);

        clientMeter.writeAndFlush(last).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    System.out.println("Sent all messages from client");
                } else {
                    System.out.println("Failed to send all messages from client");
                    Throwable cause = future.cause();
                    if (cause != null) {
                        cause.printStackTrace();
                    }
                }
            }
        });

        latch.await();
        return currentHistogram;
    }

    void tearDown() {
        cb.group().shutdownGracefully();
        sb.group().shutdownGracefully();
    }

    private class LatencyServerMeter extends ChannelInboundHandlerAdapter {
        Channel channel;

        LatencyServerMeter() {
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            channel = ctx.channel();
        }


        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf byteBuf = (ByteBuf) msg;
            final long in = byteBuf.readLong();

            if (in != LAST_PING) {
                currentHistogram.addObservation(System.nanoTime() - in);
                ByteBuf ack = Unpooled.buffer(ECHO_FRAME_SIZE);
                ack.writeLong(System.nanoTime());
                channel.write(ack);
            } else {
                ByteBuf ping = Unpooled.buffer(ECHO_FRAME_SIZE);
                ping.writeLong(LAST_PING);
                channel.write(ping);
                latch.countDown();
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.channel().close();
        }
    }

    private class LatencyClientMeter extends ChannelInboundHandlerAdapter {
        Channel channel;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            channel = ctx.channel();
        }

        public void write(ByteBuf buf) {
            channel.write(buf);
        }

        public ChannelFuture writeAndFlush(ByteBuf buf) {
            return channel.writeAndFlush(buf);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf buffer = (ByteBuf) msg;
            final long in = buffer.readLong();

            if (in != LAST_PING) {
                currentHistogram.addObservation(System.nanoTime() - in);
            } else {
                latch.countDown();
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.channel().close();
        }
    }
}
