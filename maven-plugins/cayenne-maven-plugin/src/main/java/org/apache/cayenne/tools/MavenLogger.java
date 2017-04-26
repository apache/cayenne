/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.tools;

import org.apache.maven.plugin.AbstractMojo;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.apache.maven.plugin.logging.Log;

/**
 * @since 3.0
 */
class MavenLogger implements Logger {

	private static final String LOGGER_NAME = "MavenLogger";

	private Log logger;

	public MavenLogger(AbstractMojo parent) {
		this.logger = parent.getLog();
	}

	@Override
	public void debug(String message) {
		logger.debug(message);
	}

	@Override
	public void debug(String message, Object object) {
		logger.debug(message);
	}

	@Override
	public void debug(String message, Object object, Object secondObject) {
		logger.debug(message);
	}

	@Override
	public void debug(String message, Object... objects) {
		logger.debug(message);
	}

	@Override
	public void debug(String message, Throwable throwable) {
		logger.debug(message, throwable);
	}

	@Override
	public boolean isDebugEnabled(Marker marker) {
		return logger.isDebugEnabled();
	}

	@Override
	public void debug(Marker marker, String message) {
		logger.debug(message);
	}

	@Override
	public void debug(Marker marker, String message, Object object) {
		logger.debug(message);
	}

	@Override
	public void debug(Marker marker, String message, Object object, Object secondObject) {
		logger.debug(message);
	}

	@Override
	public void debug(Marker marker, String message, Object... objects) {
		logger.debug(message);
	}

	@Override
	public void debug(Marker marker, String message, Throwable throwable) {
		logger.debug(message, throwable);
	}

	@Override
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	@Override
	public void error(String message) {
		logger.error(message);
	}

	@Override
	public void error(String message, Object object) {
		logger.error(message);
	}

	@Override
	public void error(String message, Object object, Object secondObject) {
		logger.error(message);
	}

	@Override
	public void error(String message, Object... objects) {
		logger.error(message);
	}

	@Override
	public void error(String message, Throwable throwable) {
		logger.error(message, throwable);
	}

	@Override
	public boolean isErrorEnabled(Marker marker) {
		return logger.isErrorEnabled();
	}

	@Override
	public void error(Marker marker, String message) {
		logger.error(message);
	}

	@Override
	public void error(Marker marker, String message, Object object) {
		logger.error(message);
	}

	@Override
	public void error(Marker marker, String message, Object object, Object secondObject) {
		logger.error(message);
	}

	@Override
	public void error(Marker marker, String message, Object... objects) {
		logger.error(message);
	}

	@Override
	public void error(Marker marker, String message, Throwable throwable) {
		logger.error(message, throwable);
	}

	@Override
	public void info(String message) {
		logger.info(message);
	}

	@Override
	public void info(String message, Object object) {
		logger.info(message);
	}

	@Override
	public void info(String message, Object object, Object secondObject) {
		logger.info(message);
	}

	@Override
	public void info(String message, Object... objects) {
		logger.info(message);
	}

	@Override
	public void info(String message, Throwable throwable) {
		logger.info(message, throwable);
	}

	@Override
	public boolean isInfoEnabled(Marker marker) {
		return isInfoEnabled();
	}

	@Override
	public void info(Marker marker, String message) {
		logger.info(message);
	}

	@Override
	public void info(Marker marker, String message, Object object) {
		logger.info(message);
	}

	@Override
	public void info(Marker marker, String message, Object object, Object secondObject) {
		logger.info(message);
	}

	@Override
	public void info(Marker marker, String message, Object... objects) {
		logger.info(message);
	}

	@Override
	public void info(Marker marker, String message, Throwable throwable) {
		logger.info(message, throwable);
	}

	@Override
	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	@Override
	public String getName() {
		return LOGGER_NAME;
	}

	@Override
	public boolean isTraceEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	public void trace(String message) {
		logger.debug(message);
	}

	@Override
	public void trace(String message, Object object) {
		logger.debug(message);
	}

	@Override
	public void trace(String message, Object object, Object secondObject) {
		logger.debug(message);
	}

	@Override
	public void trace(String message, Object... objects) {
		logger.debug(message);
	}

	@Override
	public void trace(String message, Throwable throwable) {
		logger.debug(message, throwable);
	}

	@Override
	public boolean isTraceEnabled(Marker marker) {
		return logger.isDebugEnabled();
	}

	@Override
	public void trace(Marker marker, String message) {
		logger.debug(message);
	}

	@Override
	public void trace(Marker marker, String message, Object object) {
		logger.debug(message);
	}

	@Override
	public void trace(Marker marker, String message, Object object, Object secondObject) {
		logger.debug(message);
	}

	@Override
	public void trace(Marker marker, String message, Object... objects) {
		logger.debug(message);
	}

	@Override
	public void trace(Marker marker, String message, Throwable throwable) {
		logger.debug(message, throwable);
	}

	@Override
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	public void warn(String message) {
		logger.warn(message);
	}

	@Override
	public void warn(String message, Object object) {
		logger.warn(message);
	}

	@Override
	public void warn(String message, Object... objects) {
		logger.warn(message);
	}

	@Override
	public void warn(String message, Object object, Object secondObject) {
		logger.warn(message);
	}

	@Override
	public void warn(String message, Throwable throwable) {
		logger.warn(message, throwable);
	}

	@Override
	public boolean isWarnEnabled(Marker marker) {
		return logger.isWarnEnabled();
	}

	@Override
	public void warn(Marker marker, String message) {
		logger.warn(message);
	}

	@Override
	public void warn(Marker marker, String message, Object object) {
		logger.warn(message);
	}

	@Override
	public void warn(Marker marker, String message, Object object, Object secondObject) {
		logger.warn(message);
	}

	@Override
	public void warn(Marker marker, String message, Object... objects) {
		logger.warn(message);
	}

	@Override
	public void warn(Marker marker, String message, Throwable throwable) {
		logger.warn(message, throwable);
	}

	@Override
	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}
}
