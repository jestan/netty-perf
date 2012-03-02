package io.netty.perf.tcp;

import io.netty.bootstrap.ClientBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveReceiveBufferSizePredictorFactory;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ServerChannelFactory;
import io.netty.channel.socket.oio.OioClientSocketChannelFactory;
import io.netty.channel.socket.oio.OioServerSocketChannelFactory;
import io.netty.handler.codec.frame.FixedLengthFrameDecoder;
import io.netty.perf.NettyLatencyTest;

import java.util.concurrent.ExecutorService;

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
public class OioTcpLatencyTest extends NettyLatencyTest {

    public OioTcpLatencyTest(long[] latencyIntervals) {
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
        return new OioClientSocketChannelFactory(executorService);
    }

    @Override
    public ServerChannelFactory newServerChannelFactory(ExecutorService executorService) {
        return new OioServerSocketChannelFactory(executorService, executorService);
    }
}
