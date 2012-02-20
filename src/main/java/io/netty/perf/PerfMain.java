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

import io.netty.perf.collection.Histogram;
import io.netty.perf.sctp.SctpLatencyTest;

public class PerfMain {
    public static void main(String[] args) {
        long[] intervals = new long[255];//256 intervals
        final long minLatency = 50000L;
        long intervalUpperBound = minLatency;
        intervals[0] = minLatency;
        for (int i = 1, size = intervals.length - 1; i < size; i++) {
            intervalUpperBound += minLatency;
            intervals[i] = intervalUpperBound;
        }

        intervals[intervals.length - 1] = Long.MAX_VALUE;

        executePerf(new SctpLatencyTest(intervals), 1000000);
    }

    private static void executePerf(NettyLatencyTest perfBench, int count) {
        try {
            perfBench.setup();
            final Histogram observations = perfBench.execute(count);
            System.out.println("*********************************************************");
            System.out.println(observations);
            System.out.println("*********************************************************");
        } finally {
            perfBench.tearDown();
        }
    }
}
