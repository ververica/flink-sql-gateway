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

package com.ververica.flink.table.gateway.rest.result;

import org.apache.flink.table.api.TableColumn;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.WatermarkSpec;
import org.apache.flink.table.api.constraints.UniqueConstraint;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LocalZonedTimestampType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.TimestampKind;
import org.apache.flink.table.types.logical.TimestampType;
import org.apache.flink.table.types.logical.ZonedTimestampType;
import org.apache.flink.table.types.logical.utils.LogicalTypeParser;
import org.apache.flink.table.types.utils.LogicalTypeDataTypeConverter;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonGenerator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonParseException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonParser;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationContext;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Utility class to convert {@link TableSchema} from/to JSON.
 */
public class TableSchemaUtil {

	private static final String FIELD_NAME_COLUMNS = "columns";
	private static final String FIELD_NAME_FIELD_NAME = "field_name";
	private static final String FIELD_NAME_FIELD_TYPE = "field_type";
	private static final String FIELD_NAME_COMPUTED_COLUMN = "computed_column";
	private static final String FIELD_NAME_PRIMARY_KEY = "primary_key";
	private static final String FIELD_NAME_NAME = "name";
	private static final String FIELD_NAME_WATERMARK_SPECS = "watermark_specs";
	private static final String FIELD_NAME_ROWTIME_ATTRIBUTE = "rowtime_attribute";
	private static final String FIELD_NAME_WATERMARK_EXPR = "watermark_expr";
	private static final String FIELD_NAME_WATERMARK_EXPR_OUTPUT_TYPE = "watermark_expr_output_type";

	public static String writeTableSchemaToJson(TableSchema schema) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule("SimpleModule");
		module.addSerializer(new TableSchemaJsonSerializer());
		objectMapper.registerModule(module);
		return objectMapper.writeValueAsString(schema);
	}

	public static TableSchema readTableSchemaFromJson(String schema) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule("SimpleModule");
		module.addDeserializer(TableSchema.class, new TableSchemaJsonDeserializer());
		objectMapper.registerModule(module);
		return objectMapper.readValue(schema, TableSchema.class);
	}

	static class TableSchemaJsonSerializer extends StdSerializer<TableSchema> {

		TableSchemaJsonSerializer() {
			super(TableSchema.class);
		}

		@Override
		public void serialize(
			TableSchema tableSchema,
			JsonGenerator jsonGenerator,
			SerializerProvider serializerProvider) throws IOException {
			jsonGenerator.writeStartObject();

			jsonGenerator.writeFieldName(FIELD_NAME_COLUMNS);
			jsonGenerator.writeStartArray();
			for (TableColumn column : tableSchema.getTableColumns()) {
				jsonGenerator.writeStartObject();
				jsonGenerator.writeStringField(FIELD_NAME_FIELD_NAME, column.getName());
				serializerProvider.defaultSerializeField(
					FIELD_NAME_FIELD_TYPE, column.getType().getLogicalType().toString(), jsonGenerator);
				if (column.getExpr().isPresent()) {
					jsonGenerator.writeStringField(FIELD_NAME_COMPUTED_COLUMN, column.getExpr().get());
				}
				jsonGenerator.writeEndObject();
			}
			jsonGenerator.writeEndArray();

			if (tableSchema.getPrimaryKey().isPresent()) {
				jsonGenerator.writeStartObject();
				jsonGenerator.writeFieldName(FIELD_NAME_PRIMARY_KEY);
				UniqueConstraint primaryKey = tableSchema.getPrimaryKey().get();
				serializerProvider.defaultSerializeField(FIELD_NAME_NAME, primaryKey.getName(), jsonGenerator);
				serializerProvider.defaultSerializeField(FIELD_NAME_COLUMNS, primaryKey.getColumns(), jsonGenerator);
				jsonGenerator.writeEndObject();
			}

			if (!tableSchema.getWatermarkSpecs().isEmpty()) {
				jsonGenerator.writeFieldName(FIELD_NAME_WATERMARK_SPECS);
				jsonGenerator.writeStartArray();
				for (WatermarkSpec spec : tableSchema.getWatermarkSpecs()) {
					jsonGenerator.writeStartObject();
					jsonGenerator.writeStringField(FIELD_NAME_ROWTIME_ATTRIBUTE, spec.getRowtimeAttribute());
					jsonGenerator.writeStringField(FIELD_NAME_WATERMARK_EXPR, spec.getWatermarkExpr());
					serializerProvider.defaultSerializeField(
						FIELD_NAME_WATERMARK_EXPR_OUTPUT_TYPE,
						spec.getWatermarkExprOutputType().getLogicalType().toString(),
						jsonGenerator);
					jsonGenerator.writeEndObject();
				}
				jsonGenerator.writeEndArray();
			}

			jsonGenerator.writeEndObject();
		}
	}

	static class TableSchemaJsonDeserializer extends StdDeserializer<TableSchema> {

		TableSchemaJsonDeserializer() {
			super(TableSchema.class);
		}

		@Override
		public TableSchema deserialize(
			JsonParser jsonParser,
			DeserializationContext ctx) throws IOException, JsonProcessingException {
			JsonNode node = jsonParser.getCodec().readTree(jsonParser);

			TableSchema.Builder builder = TableSchema.builder();

			JsonNode columnsNode = node.get(FIELD_NAME_COLUMNS);
			if (columnsNode != null) {
				for (JsonNode columnNode : columnsNode) {
					String name = columnNode.get(FIELD_NAME_FIELD_NAME).textValue();
					DataType dataType = createDataType(columnNode.get(FIELD_NAME_FIELD_TYPE), ctx);
					JsonNode computedColumnNode = columnNode.get(FIELD_NAME_COMPUTED_COLUMN);
					if (computedColumnNode != null) {
						String computedColumn = computedColumnNode.textValue();
						builder.field(name, dataType, computedColumn);
					} else {
						builder.field(name, dataType);
					}
				}
			} else {
				throw new JsonParseException(jsonParser, "Field columns must be provided");
			}

			JsonNode watermarkSpecsNode = node.get(FIELD_NAME_WATERMARK_SPECS);
			if (watermarkSpecsNode != null) {
				for (JsonNode wkSpecNode : watermarkSpecsNode) {
					String rowtimeAttribute = wkSpecNode.get(FIELD_NAME_ROWTIME_ATTRIBUTE).textValue();
					String watermarkExpr = wkSpecNode.get(FIELD_NAME_WATERMARK_EXPR).textValue();
					DataType watermarkExprOutputType =
						createDataType(wkSpecNode.get(FIELD_NAME_WATERMARK_EXPR_OUTPUT_TYPE), ctx);
					builder.watermark(rowtimeAttribute, watermarkExpr, watermarkExprOutputType);
				}
			}

			JsonNode primaryKeyNode = node.get(FIELD_NAME_PRIMARY_KEY);
			if (primaryKeyNode != null) {
				String pkName = primaryKeyNode.get(FIELD_NAME_NAME).textValue();
				JsonNode pkColumnsNode = primaryKeyNode.get(FIELD_NAME_COLUMNS);
				JsonParser pkColumnsParser = pkColumnsNode.traverse();
				pkColumnsParser.nextToken();
				String[] pkColumns = ctx.readValue(pkColumnsParser, String[].class);
				builder.primaryKey(pkName, pkColumns);
			}

			return builder.build();
		}

		private DataType createDataType(JsonNode jsonNode, DeserializationContext ctx) throws IOException {
			JsonParser typeParser = jsonNode.traverse();
			typeParser.nextToken();
			String type = ctx.readValue(typeParser, String.class);
			LogicalType logicalType;
			// TODO remove this after https://issues.apache.org/jira/browse/FLINK-16110 is fixed
			if (type.startsWith("TIMESTAMP")) {
				logicalType = parserTimestampType(type);
			} else {
				// TODO use user classloader
				logicalType = LogicalTypeParser.parse(type);
			}

			return LogicalTypeDataTypeConverter.toDataType(logicalType);
		}
	}

	/**
	 * Parse type looks like "TIMESTAMP(3) *ROWTIME*" and "TIMESTAMP(3) *PROCTIME*".
	 */
	private static LogicalType parserTimestampType(String type) {
		String newType;
		TimestampKind kind;
		if (type.contains("*ROWTIME*")) {
			newType = type.replace("*ROWTIME*", "");
			kind = TimestampKind.ROWTIME;
		} else if (type.contains("*PROCTIME*")) {
			newType = type.replace("*PROCTIME*", "");
			kind = TimestampKind.PROCTIME;
		} else {
			// TODO use user classloader
			return LogicalTypeParser.parse(type);
		}

		LogicalType t = LogicalTypeParser.parse(newType);
		if (t instanceof TimestampType) {
			return new TimestampType(t.isNullable(), kind, ((TimestampType) t).getPrecision());
		} else if (t instanceof LocalZonedTimestampType) {
			return new LocalZonedTimestampType(t.isNullable(), kind, ((LocalZonedTimestampType) t).getPrecision());
		} else if (t instanceof ZonedTimestampType) {
			return new ZonedTimestampType(t.isNullable(), kind, ((ZonedTimestampType) t).getPrecision());
		} else {
			throw new UnsupportedOperationException("Unsupported Timestamp type: " + type);
		}
	}

}
