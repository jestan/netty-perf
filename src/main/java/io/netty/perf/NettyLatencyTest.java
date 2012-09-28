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
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;

public abstract class NettyLatencyTest {
    public final static int ECHO_FRAME_SIZE= 8;

    private final long[] latencyIntervals;

    protected final LatencyMeterHandler clientMeter = new LatencyMeterHandler();
    protected final LatencyMeterHandler serverMeter = new LatencyMeterHandler();

    private ServerBootstrap sb;
    private Bootstrap cb;


    public NettyLatencyTest(long[] latencyIntervals) {
        this.latencyIntervals = latencyIntervals;
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


    public abstract Class<? extends Channel> serverChannel();


    public Histogram execute(int count) {
        Histogram currentHistogram = new Histogram(latencyIntervals);
        for (int i = 0; i < count; i++) {
            ByteBuf buffer = Unpooled.buffer(ECHO_FRAME_SIZE);
            buffer.writeLong(System.nanoTime());
            clientMeter.write(buffer);
            try {
                currentHistogram.addObservation(serverMeter.observedLatency.take());
                currentHistogram.addObservation(clientMeter.observedLatency.take());
            } catch (InterruptedException e) {
                System.out.println("ERROR: Latency Test aborted");
                break;
            }
        }
        return currentHistogram;
    }

    void tearDown() {
        cb.shutdown();
        sb.shutdown();
    }

    private static class LatencyMeterHandler extends ChannelInboundByteHandlerAdapter {
        volatile Channel channel;
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        SynchronousQueue<Long> observedLatency = new SynchronousQueue<Long>();
        LatencyMeterHandler() {
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            channel = ctx.channel();
        }

        public void write(ByteBuf buf) {
            channel.write(buf);
        }

        @Override
        public void inboundBufferUpdated(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
            observedLatency.put((System.nanoTime() - (in.readLong())));
            if (channel.parent() != null) {
                ByteBuf buffer =  Unpooled.buffer(ECHO_FRAME_SIZE);
                buffer.writeLong(System.nanoTime());
                channel.write(buffer);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (exception.compareAndSet(null, cause)) {
                cause.printStackTrace();
                ctx.channel().close();
            }
        }
    }
}
