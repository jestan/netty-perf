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

import com.sun.nio.sctp.SctpStandardSocketOptions;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ServerChannel;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.sctp.SctpChannelOption;
import io.netty.channel.sctp.oio.OioSctpChannel;
import io.netty.channel.sctp.oio.OioSctpServerChannel;
import io.netty.handler.codec.sctp.SctpInboundStreamingHandler;
import io.netty.handler.codec.sctp.SctpOutboundStreamingHandler;

import java.util.ArrayList;
import java.util.List;

import static com.sun.nio.sctp.SctpStandardSocketOptions.InitMaxStreams.*;

public class OioSctpLatencyTest extends NettyLatencyTest {

    final static int PROTOCOL_ID = 0;
    final static int MAX_INBOUND_STREAMS = 10;
    final static int MAX_OUTBOUND_STREAMS = 10;

    final static SctpStandardSocketOptions.InitMaxStreams initMaxStreams = create(MAX_INBOUND_STREAMS, MAX_OUTBOUND_STREAMS);

    List<Integer> streamConfig = new ArrayList<Integer>(2);

    public OioSctpLatencyTest(long[] latencyIntervals) {
        super(latencyIntervals);
        streamConfig.add(MAX_INBOUND_STREAMS);
        streamConfig.add(MAX_OUTBOUND_STREAMS);
    }

    @Override
    public ServerBootstrap mkServerBootStrap() {
        ServerBootstrap sb = new ServerBootstrap();

        return sb.group(new OioEventLoopGroup(), new OioEventLoopGroup())
                .childHandler(new ChannelInitializer() {
                    @Override
                    public void initChannel(Channel ch) throws Exception {
                        ch.pipeline().
                                addLast(new SctpInboundStreamingHandler(PROTOCOL_ID, 0, 9)).
                                addLast(new SctpOutboundStreamingHandler(PROTOCOL_ID, 0, 9)).
                                addLast(serverMeter);
                    }
                }).
                option(SctpChannelOption.SCTP_NODELAY, true).
                option(SctpChannelOption.SCTP_INIT_MAXSTREAMS, initMaxStreams);
//                option(SctpChannelOption.SO_RCVBUF, 1024 * 1024 * 1024).
//                option(SctpChannelOption.SO_SNDBUF, 1024 * 1024 * 1024);
    }

    @Override
    public Bootstrap mkClientBootStrap() {
        Bootstrap cb = new Bootstrap();

        return cb.group(new OioEventLoopGroup()).
                handler(new ChannelInitializer() {
                    @Override
                    public void initChannel(Channel ch) throws Exception {
                        ch.pipeline().
                                addLast(new SctpInboundStreamingHandler(PROTOCOL_ID, 0, 9)).
                                addLast(new SctpOutboundStreamingHandler(PROTOCOL_ID, 0, 9)).
                                addLast(clientMeter);
                    }
                }).
                option(SctpChannelOption.SCTP_NODELAY, true).
                option(SctpChannelOption.SCTP_INIT_MAXSTREAMS, initMaxStreams).
                option(SctpChannelOption.SO_RCVBUF, 1024 * 1024 * 1024).
                option(SctpChannelOption.SO_SNDBUF, 1024 * 1024 * 1024);
    }

    @Override
    public Class<? extends Channel> clientChannel() {
        return OioSctpChannel.class;
    }

    @Override
    public Class<? extends ServerChannel> serverChannel() {
        return OioSctpServerChannel.class;
    }

}
