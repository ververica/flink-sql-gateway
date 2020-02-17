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

import com.ververica.flink.table.gateway.SessionID;

import org.apache.flink.runtime.rest.messages.ConversionException;
import org.apache.flink.runtime.rest.messages.MessagePathParameter;

/**
 * {@link MessagePathParameter} for session id.
 */
public class SessionIdPathParameter extends MessagePathParameter<String> {

	public static final String KEY = "session_id";

	public SessionIdPathParameter() {
		super(KEY);
	}

	@Override
	protected String convertFromString(String value) throws ConversionException {
		try {
			SessionID.fromHexString(value);
			// TODO return SessionID
			return value;
		} catch (IllegalArgumentException iae) {
			throw new ConversionException("Not a valid session ID: " + value, iae);
		}
	}

	@Override
	protected String convertToString(String value) {
		return value;
	}

	@Override
	public String getDescription() {
		return "A string that identifies a session.";
	}
}
