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

package com.ververica.flink.table.gateway.security;

import org.apache.flink.runtime.security.SecurityContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;

import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;

/**
 * An implementation of SecurityContext for secure cluster.
 */
public class HadoopSecurityContext implements SecurityContext {
	private final String user;

	public HadoopSecurityContext(String user) {
		this.user = user;
	}

	@Override
	public <T> T runSecured(Callable<T> securedCallable) throws Exception {
		UserGroupInformation ugi = UserGroupInformation.getCurrentUser();
		if (StringUtils.isNotEmpty(user)){
			ugi = UserGroupInformation.createProxyUser(user, UserGroupInformation.getCurrentUser());
			return ugi.doAs((PrivilegedExceptionAction<T>) securedCallable::call);
		}
		return ugi.doAs((PrivilegedExceptionAction<T>) securedCallable::call);
	}
}
