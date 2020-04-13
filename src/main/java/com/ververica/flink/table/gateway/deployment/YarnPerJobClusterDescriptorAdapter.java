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

package com.ververica.flink.table.gateway.deployment;

import com.ververica.flink.table.gateway.context.ExecutionContext;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.client.deployment.ClusterClientFactory;
import org.apache.flink.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle job actions based on yarn-per-job mode.
 */
public class YarnPerJobClusterDescriptorAdapter<ClusterID> extends ClusterDescriptorAdapter<ClusterID>{
    private static final Logger LOG = LoggerFactory.getLogger(YarnPerJobClusterDescriptorAdapter.class);

    public YarnPerJobClusterDescriptorAdapter(ExecutionContext<ClusterID> executionContext, String sessionId) {
        super(executionContext, sessionId);
    }

    @Override
    public boolean isGloballyTerminalState(){
        boolean isGloballyTerminalState;
        try {
            JobStatus jobStatus = getJobStatus();
            isGloballyTerminalState = jobStatus.isGloballyTerminalState();
        } catch (Exception e) {
            if (containsYarnApplicationNotFoundException(e)) {
                isGloballyTerminalState = true;
            } else {
                throw e;
            }
        }

        return isGloballyTerminalState;
    }

    @Override
    public void setClusterID(Configuration configuration) {
        ClusterClientFactory<ClusterID> clusterClientFactory = executionContext.getClusterClientFactory();
        clusterID = clusterClientFactory.getClusterId(configuration);
        if (LOG.isDebugEnabled()) {
            LOG.debug("The clusterID is {} for job {}", clusterID, jobId);
        }
    }

    /**
     * We do't want to add hadoop dependency
     */
    private boolean containsYarnApplicationNotFoundException(Throwable e) {
        do {
            String exceptionClass = e.getClass().getCanonicalName();
            if (exceptionClass.equals("org.apache.hadoop.yarn.exceptions.ApplicationNotFoundException")) {
                return true;
            }
            e = e.getCause();
        } while (e != null);
        return false;
    }
}
