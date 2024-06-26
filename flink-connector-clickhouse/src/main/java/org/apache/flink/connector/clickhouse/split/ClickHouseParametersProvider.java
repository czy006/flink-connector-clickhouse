/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.connector.clickhouse.split;

import java.io.Serializable;

/** Clickhouse parameters provider. */
public abstract class ClickHouseParametersProvider {

    protected Serializable[][] parameterValues;
    protected Serializable[][] shardIdValues;
    protected int batchNum;

    /** Returns the necessary parameters array to use for query in parallel a table. */
    public Serializable[][] getParameterValues() {
        return parameterValues;
    }

    /** Returns the shard ids that the parameter values act on. */
    public Serializable[][] getShardIdValues() {
        return shardIdValues;
    }

    public abstract String getParameterClause();

    public abstract ClickHouseParametersProvider ofBatchNum(Integer batchNum);

    public abstract ClickHouseParametersProvider calculate();

    // -------------------------- Methods for local tables --------------------------

    protected int[] allocateShards(int minBatchSize, int minBatchNum, int length) {
        int[] shards = new int[length];
        for (int i = 0; i < length; i++) {
            if (i + 1 <= minBatchNum) {
                shards[i] = minBatchSize;
            } else {
                shards[i] = minBatchSize + 1;
            }
        }
        return shards;
    }

    protected Integer[] subShardIds(int start, int idNum, int[] shardIds) {
        Integer[] subIds = new Integer[idNum];
        for (int i = 0; i < subIds.length; i++) {
            subIds[i] = shardIds[start + i];
        }
        return subIds;
    }

    /** Builder. */
    public static class Builder {

        private Long minVal;

        private Long maxVal;

        private Integer batchNum;

        private int[] shardIds;

        private boolean useLocal;

        public Builder setMinVal(Long minVal) {
            this.minVal = minVal;
            return this;
        }

        public Builder setMaxVal(Long maxVal) {
            this.maxVal = maxVal;
            return this;
        }

        public Builder setBatchNum(Integer batchNum) {
            this.batchNum = batchNum;
            return this;
        }

        public Builder setShardIds(int[] shardIds) {
            this.shardIds = shardIds;
            return this;
        }

        public Builder setUseLocal(boolean useLocal) {
            this.useLocal = useLocal;
            return this;
        }

        public ClickHouseParametersProvider build() {
            ClickHouseParametersProvider parametersProvider;
            if (minVal == null || maxVal == null) {
                parametersProvider =
                        useLocal && shardIds != null
                                ? new ClickHouseShardTableParametersProvider(shardIds)
                                : new ClickHouseNonParametersProvider();
            } else {
                parametersProvider =
                        useLocal && shardIds != null
                                ? new ClickHouseShardBetweenParametersProvider(
                                        minVal, maxVal, shardIds)
                                : new ClickHouseBatchBetweenParametersProvider(minVal, maxVal);
            }

            return parametersProvider.ofBatchNum(batchNum).calculate();
        }
    }
}
