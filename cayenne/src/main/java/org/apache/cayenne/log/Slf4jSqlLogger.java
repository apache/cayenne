/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.log;

import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * A {@link SqlLogger} that emits compact, single-line messages through slf4j-api under the fixed logger name
 * {@code cayenne-sql}. Statement lines are logged at INFO; transaction boundaries at DEBUG.
 *
 * @since 5.0
 */
public class Slf4jSqlLogger implements SqlLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("cayenne-sql");

    protected final int batchRowThreshold;

    public Slf4jSqlLogger(@Inject RuntimeProperties runtimeProperties) {
        this.batchRowThreshold = runtimeProperties.getInt(Constants.JDBC_LOG_BATCH_ROW_THRESHOLD_PROPERTY, 20);
    }

    @Override
    public boolean isEnabled() {
        return LOGGER.isInfoEnabled();
    }

    @Override
    public void logSelect(TranslatedStatement statement, int rowCount) {
        logStatement(statement, "selected:", rowCount);
    }

    @Override
    public void logUpdate(TranslatedStatement statement, int rowCount, List<? extends Map<String, ?>> generatedKeys) {
        if (LOGGER.isInfoEnabled()) {
            StringBuilder buffer = new StringBuilder(buildStatementLine(statement, "updated:", rowCount));
            if (generatedKeys != null && !generatedKeys.isEmpty()) {
                buffer.append(" [generated:");
                for (Map<String, ?> keys : generatedKeys) {
                    SqlBindingRenderer.appendGeneratedKeys(buffer, keys);
                }
                buffer.append(']');
            }
            LOGGER.info(buffer.toString());
        }
    }

    protected void logStatement(TranslatedStatement statement, String resultLabel, int rowCount) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(buildStatementLine(statement, resultLabel, rowCount));
        }
    }

    protected String buildStatementLine(TranslatedStatement statement, String resultLabel, int rowCount) {
        StringBuilder buffer = new StringBuilder(statement.sql()).append(' ');
        SqlBindingRenderer.appendBindings(buffer, statement, batchRowThreshold);
        if (buffer.charAt(buffer.length() - 1) != ' ') {
            buffer.append(' ');
        }
        return buffer.append('[').append(resultLabel).append(rowCount).append(']').toString();
    }

    @Override
    public void logAlsoSelect(int rowCount) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("also [selected:{}]", rowCount);
        }
    }

    @Override
    public void logAlsoUpdate(int rowCount) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("also [updated:{}]", rowCount);
        }
    }

    @Override
    public void logTransactionStart() {
        LOGGER.debug("tx started");
    }

    @Override
    public void logTransactionCommit() {
        LOGGER.debug("tx committed");
    }

    @Override
    public void logTransactionRollback() {
        LOGGER.debug("tx rolled back");
    }

    @Override
    public void logMessage(String message) {
        if (message != null) {
            LOGGER.info(message);
        }
    }
}
