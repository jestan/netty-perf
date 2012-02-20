package io.netty.perf.sctp;

import io.netty.bootstrap.ClientBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ChannelBuffer;
import io.netty.buffer.ChannelBuffers;
import io.netty.channel.*;
import io.netty.channel.sctp.SctpClientSocketChannelFactory;
import io.netty.channel.sctp.SctpFrame;
import io.netty.channel.sctp.SctpServerSocketChannelFactory;
import io.netty.channel.sctp.codec.SctpFrameDecoder;
import io.netty.channel.sctp.codec.SctpFrameEncoder;
import io.netty.handler.codec.frame.FixedLengthFrameDecoder;
import io.netty.perf.NettyLatencyTest;
import io.netty.perf.collection.Histogram;
import io.netty.util.internal.ExecutorUtil;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;

public class SctpLatencyTest implements NettyLatencyTest {
    private final long[] latencyIntervals;

    private ExecutorService executor;

    private LatencyMeterHandler ch;
    private LatencyMeterHandler sh;


    public SctpLatencyTest(long[] latencyIntervals) {
        this.latencyIntervals = latencyIntervals;
    }

    public void setup() {
        executor = Executors.newCachedThreadPool();
        ServerBootstrap sb = new ServerBootstrap(new SctpServerSocketChannelFactory(executor, executor));
        ClientBootstrap cb = new ClientBootstrap(new SctpClientSocketChannelFactory(executor, executor));

        sh = new LatencyMeterHandler();
        ch = new LatencyMeterHandler();

        sb.getPipeline().addLast("sctp-decoder", new SctpFrameDecoder());
        sb.getPipeline().addLast("sctp-encoder", new SctpFrameEncoder());
        sb.getPipeline().addLast("decoder", new FixedLengthFrameDecoder(8));
        sb.getPipeline().addLast("handler", sh);

        sb.setOption("receiveBufferSizePredictorFactory",
                new AdaptiveReceiveBufferSizePredictorFactory(8, 8, 8));
        sb.setOption("child.sctpNoDelay", true);



        cb.getPipeline().addLast("sctp-decoder", new SctpFrameDecoder());
        cb.getPipeline().addLast("sctp-encoder", new SctpFrameEncoder());
        cb.getPipeline().addLast("decoder", new FixedLengthFrameDecoder(8));
        cb.getPipeline().addLast("handler", ch);

        cb.setOption("receiveBufferSizePredictorFactory",
                new AdaptiveReceiveBufferSizePredictorFactory(8, 8, 8));
        cb.setOption("sctpNoDelay", true);

        Channel sc = sb.bind(new InetSocketAddress("127.0.0.1", 0));
        int port = ((InetSocketAddress) sc.getLocalAddress()).getPort();

        ChannelFuture ccf = cb.connect(new InetSocketAddress("127.0.0.1", port));
        ccf.awaitUninterruptibly();
    }

    public Histogram execute(int count) {

        Histogram currentHistogram = new Histogram(latencyIntervals);
        ChannelBuffer buffer = ChannelBuffers.directBuffer(8);
        for (int i = 0; i < count; i++) {
            buffer.clear();
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

    public void tearDown() {
        ch.channel.close().awaitUninterruptibly();
        sh.channel.close().awaitUninterruptibly();
        ExecutorUtil.terminate(executor);
    }

    private static class LatencyMeterHandler extends SimpleChannelUpstreamHandler {
        volatile Channel channel;
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        SynchronousQueue<Long> observedLatency = new SynchronousQueue<Long>();
        ChannelBuffer buffer = ChannelBuffers.directBuffer(8);

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
