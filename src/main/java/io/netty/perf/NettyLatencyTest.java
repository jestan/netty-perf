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

import io.netty.bootstrap.ClientBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ChannelBuffer;
import io.netty.buffer.ChannelBuffers;
import io.netty.channel.*;
import io.netty.perf.collection.Histogram;
import io.netty.util.internal.ExecutorUtil;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;

public abstract class NettyLatencyTest {
    public final static int ECHO_FRAME_SIZE= 8;

    private final long[] latencyIntervals;

    private ExecutorService executor;

    private LatencyMeterHandler ch;
    private LatencyMeterHandler sh;


    public NettyLatencyTest(long[] latencyIntervals) {
        this.latencyIntervals = latencyIntervals;
    }

    public void setup() {
        executor = Executors.newCachedThreadPool();
        ServerBootstrap sb = new ServerBootstrap(newServerChannelFactory(executor));
        ClientBootstrap cb = new ClientBootstrap(newClientChannelFactory(executor));

        sh = new LatencyMeterHandler();
        ch = new LatencyMeterHandler();

        prepareServerBootStrap(sb, sh);


        prepareClientBootStrap(cb, ch);

        Channel sc = sb.bind(new InetSocketAddress("127.0.0.1", 0));
        int port = ((InetSocketAddress) sc.getLocalAddress()).getPort();

        ChannelFuture ccf = cb.connect(new InetSocketAddress("127.0.0.1", port));
        ccf.awaitUninterruptibly();
    }

    public abstract void prepareServerBootStrap(ServerBootstrap serverBootstrap, ChannelHandler sh);

    public abstract void prepareClientBootStrap(ClientBootstrap clientBootstrap, ChannelHandler ch);


    public abstract ChannelFactory newClientChannelFactory(ExecutorService executorService);


    public abstract ServerChannelFactory newServerChannelFactory(ExecutorService executorService);


    public Histogram execute(int count) {

        Histogram currentHistogram = new Histogram(latencyIntervals);
        for (int i = 0; i < count; i++) {
            ChannelBuffer buffer = ChannelBuffers.directBuffer(ECHO_FRAME_SIZE);
            buffer.writeLong(System.nanoTime());
            ch.channel.write(buffer);
            try {
                currentHistogram.addObservation(sh.observedLatency.take());
                currentHistogram.addObservation(ch.observedLatency.take());
            } catch (InterruptedException e) {
                System.out.println("ERROR: Latency Test aborted");
                break;
            }
        }
        return currentHistogram;
    }

    void tearDown() {
        ch.channel.close().awaitUninterruptibly();
        sh.channel.close().awaitUninterruptibly();
        ExecutorUtil.terminate(executor);
    }

    private static class LatencyMeterHandler extends SimpleChannelUpstreamHandler {
        volatile Channel channel;
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        SynchronousQueue<Long> observedLatency = new SynchronousQueue<Long>();
        ChannelBuffer buffer = ChannelBuffers.directBuffer(ECHO_FRAME_SIZE);

        LatencyMeterHandler() {
        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
                throws Exception {
            channel = e.getChannel();
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
                throws Exception {
            ChannelBuffer m = (ChannelBuffer) e.getMessage();
            observedLatency.put((System.nanoTime() - (m.readLong())));
            if (channel.getParent() != null) {
                buffer.clear();
                buffer.writeLong(System.nanoTime());
                channel.write(buffer);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
                throws Exception {
            if (exception.compareAndSet(null, e.getCause())) {
                e.getChannel().close();
            }
        }
    }
}
