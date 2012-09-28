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
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSctpChannel;
import io.netty.channel.socket.nio.NioSctpServerChannel;
import io.netty.handler.codec.sctp.SctpInboundByteStreamHandler;
import io.netty.handler.codec.sctp.SctpMessageCompletionHandler;
import io.netty.handler.codec.sctp.SctpOutboundByteStreamHandler;
import io.netty.perf.NettyLatencyTest;

public class SctpLatencyTest extends NettyLatencyTest {

    public SctpLatencyTest(long[] latencyIntervals) {
        super(latencyIntervals);
    }

    @Override
    public ServerBootstrap mkServerBootStrap() {
        ServerBootstrap sb = new ServerBootstrap();

        return sb.group(new NioEventLoopGroup(), new NioEventLoopGroup())
                .childHandler(new ChannelInitializer() {
                    @Override
                    public void initChannel(Channel ch) throws Exception {
                        ch.pipeline().
//                                addLast(new SctpMessageCompletionHandler()).
                                addLast(new SctpInboundByteStreamHandler(0, 0)).
                                addLast(new SctpOutboundByteStreamHandler(0, 0)).
                                addLast(serverMeter);
                    }
                }).option(ChannelOption.SCTP_NODELAY, true);
    }

    @Override
    public Bootstrap mkClientBootStrap() {
        Bootstrap cb = new Bootstrap();

        return cb.group(new NioEventLoopGroup()).
                handler(new ChannelInitializer() {
                    @Override
                    public void initChannel(Channel ch) throws Exception {
                        ch.pipeline().
//                                addLast(new SctpMessageCompletionHandler()).
                                addLast(new SctpInboundByteStreamHandler(0, 0)).
                                addLast(new SctpOutboundByteStreamHandler(0, 0)).
                                addLast(clientMeter);
                    }
                }).option(ChannelOption.SCTP_NODELAY, true);
    }

    @Override
    public Class<? extends Channel> clientChannel() {
        return NioSctpChannel.class;
    }

    @Override
    public Class<? extends Channel> serverChannel() {
        return NioSctpServerChannel.class;
    }

}
