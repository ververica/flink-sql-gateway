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

package com.ververica.flink.table.gateway.context;

import com.ververica.flink.table.gateway.SqlGatewayException;
import com.ververica.flink.table.gateway.config.Environment;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.client.cli.CliFrontend;
import org.apache.flink.client.cli.CliFrontendParser;
import org.apache.flink.client.cli.CustomCommandLine;
import org.apache.flink.client.deployment.ClusterClientServiceLoader;
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.plugin.PluginUtils;

import org.apache.commons.cli.Options;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Context describing default environment, dependencies, flink config, etc.
 */
public class DefaultContext {
	private final Environment defaultEnv;
	private final List<URL> dependencies;
	private final Configuration flinkConfig;
	private final List<CustomCommandLine> commandLines;
	private final Options commandLineOptions;
	private final ClusterClientServiceLoader clusterClientServiceLoader;

	public DefaultContext(Environment defaultEnv, List<URL> dependencies) {
		this.defaultEnv = defaultEnv;
		this.dependencies = dependencies;

		// discover configuration
		final String flinkConfigDir;
		try {
			// find the configuration directory
			flinkConfigDir = CliFrontend.getConfigurationDirectoryFromEnv();

			// load the global configuration
			this.flinkConfig = GlobalConfiguration.loadConfiguration(flinkConfigDir);

			// initialize default file system
			FileSystem.initialize(flinkConfig, PluginUtils.createPluginManagerFromRootFolder(flinkConfig));

			// load command lines for deployment
			this.commandLines = CliFrontend.loadCustomCommandLines(flinkConfig, flinkConfigDir);
			this.commandLineOptions = collectCommandLineOptions(commandLines);
		} catch (Exception e) {
			throw new SqlGatewayException("Could not load Flink configuration.", e);
		}
		clusterClientServiceLoader = new DefaultClusterClientServiceLoader();
	}

	/**
	 * Constructor for testing purposes.
	 */
	@VisibleForTesting
	public DefaultContext(
			Environment defaultEnv,
			List<URL> dependencies,
			Configuration flinkConfig,
			CustomCommandLine commandLine,
			ClusterClientServiceLoader clusterClientServiceLoader) {
		this.defaultEnv = defaultEnv;
		this.dependencies = dependencies;
		this.flinkConfig = flinkConfig;
		this.commandLines = Collections.singletonList(commandLine);
		this.commandLineOptions = collectCommandLineOptions(commandLines);
		this.clusterClientServiceLoader = Objects.requireNonNull(clusterClientServiceLoader);
	}

	public Configuration getFlinkConfig() {
		return flinkConfig;
	}

	public Environment getDefaultEnv() {
		return defaultEnv;
	}

	public List<URL> getDependencies() {
		return dependencies;
	}

	public List<CustomCommandLine> getCommandLines() {
		return commandLines;
	}

	public Options getCommandLineOptions() {
		return commandLineOptions;
	}

	public ClusterClientServiceLoader getClusterClientServiceLoader() {
		return clusterClientServiceLoader;
	}

	private Options collectCommandLineOptions(List<CustomCommandLine> commandLines) {
		final Options customOptions = new Options();
		for (CustomCommandLine customCommandLine : commandLines) {
			customCommandLine.addGeneralOptions(customOptions);
			customCommandLine.addRunOptions(customOptions);
		}
		return CliFrontendParser.mergeOptions(
			CliFrontendParser.getRunCommandOptions(),
			customOptions);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DefaultContext)) {
			return false;
		}
		DefaultContext context = (DefaultContext) o;
		return Objects.equals(defaultEnv, context.defaultEnv) &&
			Objects.equals(dependencies, context.dependencies) &&
			Objects.equals(flinkConfig, context.flinkConfig) &&
			Objects.equals(commandLines, context.commandLines) &&
			Objects.equals(commandLineOptions, context.commandLineOptions) &&
			Objects.equals(clusterClientServiceLoader, context.clusterClientServiceLoader);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			defaultEnv, dependencies, flinkConfig, commandLines, commandLineOptions, clusterClientServiceLoader);
	}
}
