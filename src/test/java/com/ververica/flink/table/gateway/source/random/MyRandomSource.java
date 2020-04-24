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
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.sources.StreamTableSource;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.Row;

import java.util.Random;

import static org.apache.flink.table.types.utils.TypeConversions.fromDataTypeToLegacyInfo;

/**
 * A user defined source which generates random data for testing purpose.
 * It's schema must be ( a INT, b BIGINT ).
 */
public class MyRandomSource implements StreamTableSource<Row> {

	private final TableSchema schema;

	private final MyRandomSourceFunction sourceFunction;

	public MyRandomSource(int limit) {
		this.schema = TableSchema.builder()
			.field("a", DataTypes.INT())
			.field("b", DataTypes.BIGINT())
			.build();

		this.sourceFunction = new MyRandomSourceFunction(limit);
	}

	@Override
	public boolean isBounded() {
		return true;
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

	/**
	 * Source function of {@link MyRandomSource}.
	 */
	public static class MyRandomSourceFunction implements SourceFunction<Row> {

		private final int limit;
		private boolean running;

		private Random random;

		public MyRandomSourceFunction(int limit) {
			this.limit = limit;
			this.running = false;

			this.random = new Random();
		}

		@Override
		public void run(SourceContext<Row> ctx) throws Exception {
			int collected = 0;
			while (running && collected < limit) {
				ctx.collect(Row.of(random.nextInt(), random.nextLong()));
			}
		}

		@Override
		public void cancel() {
			running = false;
		}
	}
}
