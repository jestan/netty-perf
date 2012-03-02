package io.netty.perf.tcp;
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
import io.netty.channel.sctp.SctpClientSocketChannelFactory;
import io.netty.channel.sctp.SctpServerSocketChannelFactory;
import io.netty.channel.sctp.codec.SctpFrameDecoder;
import io.netty.channel.sctp.codec.SctpFrameEncoder;
import io.netty.channel.socket.ClientSocketChannelFactory;
import io.netty.channel.socket.ServerSocketChannelFactory;
import io.netty.channel.socket.nio.NioClientSocketChannelFactory;
import io.netty.channel.socket.nio.NioServerSocketChannelFactory;
import io.netty.handler.codec.frame.FixedLengthFrameDecoder;
import io.netty.perf.NettyLatencyTest;
import io.netty.perf.collection.Histogram;
import io.netty.util.internal.ExecutorUtil;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;

public class NioTcpLatencyTest extends NettyLatencyTest {
    public NioTcpLatencyTest(long[] latencyIntervals) {
        super(latencyIntervals);
    }

    @Override
    public void prepareServerBootStrap(ServerBootstrap sb, ChannelHandler sh) {
        sb.getPipeline().addLast("decoder", new FixedLengthFrameDecoder(ECHO_FRAME_SIZE));
        sb.getPipeline().addLast("handler", sh);

        sb.setOption("receiveBufferSizePredictorFactory",
                new AdaptiveReceiveBufferSizePredictorFactory(ECHO_FRAME_SIZE, ECHO_FRAME_SIZE, ECHO_FRAME_SIZE));
        sb.setOption("child.tcpNoDelay", true);

    }

    @Override
    public void prepareClientBootStrap(ClientBootstrap cb, ChannelHandler ch) {
        cb.getPipeline().addLast("decoder", new FixedLengthFrameDecoder(ECHO_FRAME_SIZE));
        cb.getPipeline().addLast("handler", ch);

        cb.setOption("receiveBufferSizePredictorFactory",
                new AdaptiveReceiveBufferSizePredictorFactory(ECHO_FRAME_SIZE, ECHO_FRAME_SIZE, ECHO_FRAME_SIZE));
        cb.setOption("tcpNoDelay", true);
    }

    @Override
    public ChannelFactory newClientChannelFactory(ExecutorService executorService) {
        return new NioClientSocketChannelFactory(executorService, executorService);
    }

    @Override
    public ServerChannelFactory newServerChannelFactory(ExecutorService executorService) {
        return new NioServerSocketChannelFactory(executorService, executorService);
    }
}
