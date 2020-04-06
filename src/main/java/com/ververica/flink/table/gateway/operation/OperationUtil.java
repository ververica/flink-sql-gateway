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

package com.ververica.flink.table.gateway.operation;

import com.ververica.flink.table.gateway.rest.result.ColumnInfo;
import com.ververica.flink.table.gateway.rest.result.ConstantNames;
import com.ververica.flink.table.gateway.rest.result.ResultKind;
import com.ververica.flink.table.gateway.rest.result.ResultSet;

import org.apache.flink.table.types.logical.VarCharType;
import org.apache.flink.types.Row;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for {@link Operation}.
 */
public class OperationUtil {

	public static final ResultSet OK = ResultSet.builder()
		.resultKind(ResultKind.SUCCESS)
		.columns(ColumnInfo.create(ConstantNames.RESULT, new VarCharType(2)))
		.data(Row.of("OK"))
		.build();

	public static ResultSet singleStringToResultSet(String str, String columnName) {
		return ResultSet.builder()
			.resultKind(ResultKind.SUCCESS_WITH_CONTENT)
			.columns(ColumnInfo.create(columnName, new VarCharType(false, str.length())))
			.data(Row.of(str))
			.build();
	}

	public static ResultSet stringListToResultSet(List<String> strings, String columnName) {
		List<Row> data = new ArrayList<>();
		int maxLength = VarCharType.DEFAULT_LENGTH;

		for (String str : strings) {
			maxLength = Math.max(str.length(), maxLength);
			data.add(Row.of(str));
		}

		return ResultSet.builder()
			.resultKind(ResultKind.SUCCESS_WITH_CONTENT)
			.columns(ColumnInfo.create(columnName, new VarCharType(false, maxLength)))
			.data(data)
			.build();
	}
}
