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

package com.ververica.flink.table.gateway.rest.message;

import org.apache.flink.runtime.rest.messages.ResponseBody;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link ResponseBody} for getting info.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetInfoResponseBody implements ResponseBody {

	private static final String FIELD_NAME_PRODUCT_NAME = "product_name";
	private static final String FIELD_NAME_FLINK_VERSION = "version";

	@JsonProperty(FIELD_NAME_PRODUCT_NAME)
	private final String productName;

	@JsonProperty(FIELD_NAME_FLINK_VERSION)
	private final String flinkVersion;

	public GetInfoResponseBody(
		@JsonProperty(FIELD_NAME_PRODUCT_NAME) String productName,
		@JsonProperty(FIELD_NAME_FLINK_VERSION) String flinkVersion) {
		this.productName = productName;
		this.flinkVersion = flinkVersion;
	}

	public String getProductName() {
		return productName;
	}

	public String getFlinkVersion() {
		return flinkVersion;
	}
}
