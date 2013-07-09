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
import io.netty.channel.ServerChannel;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;

public class OioTcpLatencyTest extends NettyLatencyTest {

    public OioTcpLatencyTest(long[] latencyIntervals) {
        super(latencyIntervals);
    }

    @Override
    public ServerBootstrap mkServerBootStrap() {
        ServerBootstrap sb = new ServerBootstrap();
        return sb.group(new OioEventLoopGroup(), new OioEventLoopGroup()).
                childHandler(new ChannelInitializer() {
                    @Override
                    public void initChannel(Channel ch) throws Exception {
                        ch.pipeline().
                                addLast(new FixedLengthFrameDecoder(ECHO_FRAME_SIZE)).
                                addLast(serverMeter);
                    }
                }).option(ChannelOption.TCP_NODELAY, true);
    }

    @Override
    public Bootstrap mkClientBootStrap() {
        Bootstrap cb = new Bootstrap();
        return cb.group(new OioEventLoopGroup()).
                handler(new ChannelInitializer() {
                    @Override
                    public void initChannel(Channel ch) throws Exception {
                        ch.pipeline().
                                addLast(new FixedLengthFrameDecoder(ECHO_FRAME_SIZE)).
                                addLast(clientMeter);
                    }
                }).option(ChannelOption.TCP_NODELAY, true).
                option(ChannelOption.SO_RCVBUF, 1024 * 1024 * 1024).
                option(ChannelOption.SO_SNDBUF, 1024 * 1024 * 1024);
    }

    @Override
    public Class<? extends Channel> clientChannel() {
        return OioSocketChannel.class;
    }

    @Override
    public Class<? extends ServerChannel> serverChannel() {
        return OioServerSocketChannel.class;
    }
}
