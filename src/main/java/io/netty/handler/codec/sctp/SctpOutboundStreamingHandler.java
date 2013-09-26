/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.sctp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.sctp.SctpMessage;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A ChannelHandler which encodes {@link io.netty.buffer.ByteBuf}s to {@link io.netty.channel.sctp.SctpMessage}s and send them over multiple SCTP Streams.
 */
public class SctpOutboundStreamingHandler extends MessageToMessageEncoder<ByteBuf> {
    private final int protocolIdentifier;
    private final int minStream;
    private final int maxStream;

    private final int divider;

    private final AtomicLong streamCounter;

    /**
     * Encode {@link io.netty.buffer.ByteBuf}s to {@link io.netty.channel.sctp.SctpMessage}s and send them over a multiple SCTP streams. Stream number is
     * selected in a round robin fashion from given range.
     *
     * @param protocolIdentifier supported application protocol.
     * @param minStream          minimum stream number of the protocol,
     * @param maxStream          maximum stream number of the protocol,should be <= to max no streams
     *                           of the association.
     */
    public SctpOutboundStreamingHandler(int protocolIdentifier, int minStream, int maxStream) {
        if (minStream > maxStream) {
            throw new IllegalArgumentException("minStream should be <= maxStream");
        }
        this.protocolIdentifier = protocolIdentifier;
        this.minStream = minStream;
        this.maxStream = maxStream;
        this.streamCounter = new AtomicLong(minStream);

        this.divider = maxStream + 1;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        out.add(new SctpMessage(protocolIdentifier, (int) selectStreamByRoundRobin(), msg.retain()));
    }

    private long selectStreamByRoundRobin() {
        return minStream + (streamCounter.getAndIncrement() % divider);
    }
}
