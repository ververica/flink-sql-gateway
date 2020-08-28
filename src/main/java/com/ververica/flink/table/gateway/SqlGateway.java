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

package com.ververica.flink.table.gateway;

import com.ververica.flink.table.gateway.config.Environment;
import com.ververica.flink.table.gateway.context.DefaultContext;
import com.ververica.flink.table.gateway.rest.SqlGatewayEndpoint;
import com.ververica.flink.table.gateway.rest.session.SessionManager;
import com.ververica.flink.table.gateway.utils.SqlGatewayException;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.rest.RestServerEndpointConfiguration;
import org.apache.flink.runtime.util.EnvironmentInformation;
import org.apache.flink.util.JarUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static com.ververica.flink.table.gateway.utils.EnvironmentUtil.readEnvironment;

/**
 * Sql gateway.
 */
public class SqlGateway {
	private static final Logger LOG = LoggerFactory.getLogger(SqlGateway.class);

	private final GatewayOptions options;
	private SqlGatewayEndpoint endpoint;
	private SessionManager sessionManager;

	public SqlGateway(GatewayOptions options) {
		this.options = options;
	}

	private void start() throws Exception {
		final Environment defaultEnv = readEnvironment(options.getDefaultConfig().orElse(null));

		final Integer port = options.getPort().orElse(defaultEnv.getServer().getPort());
		final String address = defaultEnv.getServer().getAddress();
		final Optional<String> bindAddress = defaultEnv.getServer().getBindAddress();

		Configuration configuration = new Configuration();
		configuration.setString(RestOptions.ADDRESS, address);
		bindAddress.ifPresent(s -> configuration.setString(RestOptions.BIND_ADDRESS, s));
		configuration.setString(RestOptions.BIND_PORT, String.valueOf(port));

		final List<URL> dependencies = discoverDependencies(options.getJars(), options.getLibraryDirs());
		final DefaultContext defaultContext = new DefaultContext(defaultEnv, dependencies);
		sessionManager = new SessionManager(defaultContext);

		endpoint = new SqlGatewayEndpoint(
			RestServerEndpointConfiguration.fromConfiguration(configuration),
			sessionManager);
		endpoint.start();
		System.out.println("Rest endpoint started.");

		new CountDownLatch(1).await();
	}

	private static List<URL> discoverDependencies(List<URL> jars, List<URL> libraries) {
		final List<URL> dependencies = new ArrayList<>();
		try {
			// find jar files
			for (URL url : jars) {
				JarUtils.checkJarFile(url);
				dependencies.add(url);
			}

			// find jar files in library directories
			for (URL libUrl : libraries) {
				final File dir = new File(libUrl.toURI());
				if (!dir.isDirectory()) {
					throw new SqlGatewayException("Directory expected: " + dir);
				} else if (!dir.canRead()) {
					throw new SqlGatewayException("Directory cannot be read: " + dir);
				}
				final File[] files = dir.listFiles();
				if (files == null) {
					throw new SqlGatewayException("Directory cannot be read: " + dir);
				}
				for (File f : files) {
					// only consider jars
					if (f.isFile() && f.getAbsolutePath().toLowerCase().endsWith(".jar")) {
						final URL url = f.toURI().toURL();
						JarUtils.checkJarFile(url);
						dependencies.add(url);
					}
				}
			}
		} catch (Exception e) {
			throw new SqlGatewayException("Could not load all required JAR files.", e);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Using the following dependencies: {}", dependencies);
		}

		return dependencies;
	}

	// --------------------------------------------------------------------------------------------

	public static void main(String[] args) {
		checkFlinkVersion();
		final GatewayOptions options = GatewayOptionsParser.parseGatewayOptions(args);
		if (options.isPrintHelp()) {
			GatewayOptionsParser.printHelp();
		} else {
			SqlGateway gateway = new SqlGateway(options);
			try {
				// add shutdown hook
				Runtime.getRuntime().addShutdownHook(new ShutdownThread(gateway));

				// do the actual work
				gateway.start();
			} catch (Throwable t) {
				// make space in terminal
				System.out.println();
				System.out.println();
				LOG.error("Gateway must stop. Unexpected exception. This is a bug. Please consider filing an issue.",
					t);
				throw new SqlGatewayException(
					"Unexpected exception. This is a bug. Please consider filing an issue.", t);
			}
		}
	}

	private static void checkFlinkVersion() {
		String flinkVersion = EnvironmentInformation.getVersion();
		if (!flinkVersion.startsWith("1.11")) {
			LOG.error("Only Flink-1.11 is supported now!");
			throw new SqlGatewayException("Only Flink-1.11 is supported now!");
		} else if (flinkVersion.startsWith("1.11.0")) {
			LOG.error("Flink-1.11.0 is not supported, please use Flink >= 1.11.1!");
			throw new SqlGatewayException("Flink-1.11.0 is not supported, please use Flink >= 1.11.1!");
		}
	}

	// --------------------------------------------------------------------------------------------

	private static class ShutdownThread extends Thread {

		private final SqlGateway gateway;

		public ShutdownThread(SqlGateway gateway) {
			this.gateway = gateway;
		}

		@Override
		public void run() {
			// Shutdown the gateway
			System.out.println("\nShutting down the gateway...");
			LOG.info("Shutting down the gateway...");
			if (gateway.endpoint != null) {
				try {
					gateway.endpoint.closeAsync();
				} catch (Exception e) {
					LOG.error("Failed to shut down the endpoint: " + e.getMessage());
					System.out.println("Failed to shut down the endpoint: " + e.getMessage());
				}
			}
			if (gateway.sessionManager != null) {
				try {
					gateway.sessionManager.close();
				} catch (Exception e) {
					LOG.error("Failed to shut down the session manger: " + e.getMessage());
					System.out.println("Failed to shut down the session manger: " + e.getMessage());
				}
			}
			LOG.info("done.");
			System.out.println("done.");
		}
	}
}
