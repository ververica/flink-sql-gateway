/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.flink.table.gateway.source.random;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.sources.StreamTableSource;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.Row;

import static org.apache.flink.table.types.utils.TypeConversions.fromDataTypeToLegacyInfo;

/**
 * A user defined source which generates random data for testing purpose.
 */
public class RandomSource implements StreamTableSource<Row> {

	private final TableSchema schema;
	private final long limit;

	private final RandomSourceFunction sourceFunction;

	public RandomSource(TableSchema schema, long limit) {
		this.schema = schema;
		this.limit = limit;

		this.sourceFunction = new RandomSourceFunction(schema, limit);
	}

	@Override
	public boolean isBounded() {
		return limit > 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DataStream<Row> getDataStream(StreamExecutionEnvironment streamExecutionEnvironment) {
		TypeInformation<Row> typeInfo = (TypeInformation<Row>) fromDataTypeToLegacyInfo(getProducedDataType());
		return streamExecutionEnvironment.addSource(sourceFunction, typeInfo);
	}

	@Override
	public DataType getProducedDataType() {
		return schema.toRowDataType();
	}

	@Override
	public TableSchema getTableSchema() {
		return schema;
	}
}
