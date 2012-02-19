package io.netty.perf.tcp;

import io.netty.perf.NettyLatencyTest;
import io.netty.perf.collection.Histogram;

public class TcpLatencyTest implements NettyLatencyTest {
    private final long[] latencyIntervals;

    public TcpLatencyTest(long[] latencyIntervals) {
        this.latencyIntervals = latencyIntervals;
    }


    public void setup() {
    }

    public Histogram execute(int count) {
        Histogram currentHistogram = new Histogram(latencyIntervals);
        //TODO: implement this
        return currentHistogram;
    }

    public void tearDown() {
    }
}
