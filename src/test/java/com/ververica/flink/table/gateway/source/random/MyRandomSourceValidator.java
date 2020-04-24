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

import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.descriptors.ConnectorDescriptorValidator;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.descriptors.Schema;
import org.apache.flink.table.types.DataType;

/**
 * Validator for {@link MyRandomSource}.
 */
public class MyRandomSourceValidator extends ConnectorDescriptorValidator {

	public static final String CONNECTOR_TYPE_VALUE = "my-random";
	public static final String MY_RANDOM_LIMIT = "my-random.limit";

	@Override
	public void validate(DescriptorProperties properties) {
		super.validate(properties);
		properties.validateValue(CONNECTOR_TYPE, CONNECTOR_TYPE_VALUE, false);
		properties.validateInt(MY_RANDOM_LIMIT, false);
		TableSchema schema = properties.getTableSchema(Schema.SCHEMA);
		if (!validateSchema(schema)) {
			throw new ValidationException("Invalid table schema. Table schema must be (a INT, b BIGINT)");
		}
	}

	private boolean validateSchema(TableSchema schema) {
		if (schema.getFieldCount() != 2) {
			return false;
		}

		String[] fieldNames = schema.getFieldNames();
		DataType[] fieldTypes = schema.getFieldDataTypes();
		return "a".equals(fieldNames[0]) && DataTypes.INT().equals(fieldTypes[0]) &&
			"b".equals(fieldNames[1]) && DataTypes.BIGINT().equals(fieldTypes[1]);
	}
}
