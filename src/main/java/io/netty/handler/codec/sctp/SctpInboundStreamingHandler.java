/*
 * Copyright 2012 The Netty Project
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

package io.netty.handler.codec.sctp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.sctp.SctpMessage;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * A ChannelHandler which decodes {@link io.netty.channel.sctp.SctpMessage}s to {@link io.netty.buffer.ByteBuf}s multiple SCTP Streams.
 */
public class SctpInboundStreamingHandler extends MessageToMessageDecoder<SctpMessage> {
    private final int protocolIdentifier;
    private final int minStream;
    private final int maxStream;

    /**Accepts {@link io.netty.channel.sctp.SctpMessage}s from range of streams and a specified application protocol.
     * @param protocolIdentifier supported application protocol.
     * @param minStream minimum stream number of the protocol,
     * @param maxStream maximum stream number of the protocol,should be <= to max no streams of the association.
     */
    public SctpInboundStreamingHandler(int protocolIdentifier, int minStream, int maxStream) {
        if (minStream > maxStream) {
            throw new IllegalArgumentException("minStream should be <= maxStream");
        }
        this.protocolIdentifier = protocolIdentifier;
        this.minStream = minStream;
        this.maxStream = maxStream;
    }

    @Override
    public final boolean acceptInboundMessage(Object msg) throws Exception {
        if (super.acceptInboundMessage(msg)) {
            return acceptInboundMessage((SctpMessage) msg);
        }
        return false;
    }

    protected boolean acceptInboundMessage(SctpMessage msg) {
        return msg.protocolIdentifier() == protocolIdentifier &&
                msg.streamIdentifier() >= minStream && msg.streamIdentifier() <= maxStream ;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, SctpMessage msg, List<Object> out) throws Exception {
        if (!msg.isComplete()) {
            throw new CodecException(String.format("Received un-complete SctpMessage, please add %s in the " +
                    "pipeline before this handler", SctpMessageCompletionHandler.class.getSimpleName()));
        }
        out.add(msg.content().retain());
    }
}
