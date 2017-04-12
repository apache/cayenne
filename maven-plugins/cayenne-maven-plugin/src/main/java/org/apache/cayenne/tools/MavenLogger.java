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

	private Log logger;

	public MavenLogger(AbstractMojo parent) {
		this.logger = parent.getLog();
	}

	@Override
	public void debug(String s) {
		logger.debug(s);
	}

	@Override
	public void debug(String s, Object o) {
		logger.debug(s);
	}

	@Override
	public void debug(String s, Object o, Object o1) {
		logger.debug(s);
	}

	@Override
	public void debug(String s, Object... objects) {
		logger.debug(s);
	}

	@Override
	public void debug(String s, Throwable throwable) {
		logger.debug(s, throwable);
	}

	@Override
	public boolean isDebugEnabled(Marker marker) {
		return logger.isDebugEnabled();
	}

	@Override
	public void debug(Marker marker, String s) {
		logger.debug(s);
	}

	@Override
	public void debug(Marker marker, String s, Object o) {
		logger.debug(s);
	}

	@Override
	public void debug(Marker marker, String s, Object o, Object o1) {
		logger.debug(s);
	}

	@Override
	public void debug(Marker marker, String s, Object... objects) {
		logger.debug(s);
	}

	@Override
	public void debug(Marker marker, String s, Throwable throwable) {
		logger.debug(s, throwable);
	}

	@Override
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	@Override
	public void error(String s) {
		logger.error(s);
	}

	@Override
	public void error(String s, Object o) {
		logger.error(s);
	}

	@Override
	public void error(String s, Object o, Object o1) {
		logger.error(s);
	}

	@Override
	public void error(String s, Object... objects) {
		logger.error(s);
	}

	@Override
	public void error(String s, Throwable throwable) {
		logger.error(s, throwable);
	}

	@Override
	public boolean isErrorEnabled(Marker marker) {
		return logger.isErrorEnabled();
	}

	@Override
	public void error(Marker marker, String s) {
		logger.error(s);
	}

	@Override
	public void error(Marker marker, String s, Object o) {
		logger.error(s);
	}

	@Override
	public void error(Marker marker, String s, Object o, Object o1) {
		logger.error(s);
	}

	@Override
	public void error(Marker marker, String s, Object... objects) {
		logger.error(s);
	}

	@Override
	public void error(Marker marker, String s, Throwable throwable) {
		logger.error(s, throwable);
	}

	@Override
	public void info(String s) {
		logger.info(s);
	}

	@Override
	public void info(String s, Object o) {
		logger.info(s);
	}

	@Override
	public void info(String s, Object o, Object o1) {
		logger.info(s);
	}

	@Override
	public void info(String s, Object... objects) {
		logger.info(s);
	}

	@Override
	public void info(String s, Throwable throwable) {
		logger.info(s, throwable);
	}

	@Override
	public boolean isInfoEnabled(Marker marker) {
		return isInfoEnabled();
	}

	@Override
	public void info(Marker marker, String s) {
		logger.info(s);
	}

	@Override
	public void info(Marker marker, String s, Object o) {
		logger.info(s);
	}

	@Override
	public void info(Marker marker, String s, Object o, Object o1) {
		logger.info(s);
	}

	@Override
	public void info(Marker marker, String s, Object... objects) {
		logger.info(s);
	}

	@Override
	public void info(Marker marker, String s, Throwable throwable) {
		logger.info(s, throwable);
	}

	@Override
	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	@Override
	public String getName() {
		return this.getName();
	}

	@Override
	public boolean isTraceEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	public void trace(String s) {
		logger.debug(s);
	}

	@Override
	public void trace(String s, Object o) {
		logger.debug(s);
	}

	@Override
	public void trace(String s, Object o, Object o1) {
		logger.debug(s);
	}

	@Override
	public void trace(String s, Object... objects) {
		logger.debug(s);
	}

	@Override
	public void trace(String s, Throwable throwable) {
		logger.debug(s, throwable);
	}

	@Override
	public boolean isTraceEnabled(Marker marker) {
		return logger.isDebugEnabled();
	}

	@Override
	public void trace(Marker marker, String s) {
		logger.debug(s);
	}

	@Override
	public void trace(Marker marker, String s, Object o) {
		logger.debug(s);
	}

	@Override
	public void trace(Marker marker, String s, Object o, Object o1) {
		logger.debug(s);
	}

	@Override
	public void trace(Marker marker, String s, Object... objects) {
		logger.debug(s);
	}

	@Override
	public void trace(Marker marker, String s, Throwable throwable) {
		logger.debug(s, throwable);
	}

	@Override
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	public void warn(String s) {
		logger.warn(s);
	}

	@Override
	public void warn(String s, Object o) {
		logger.warn(s);
	}

	@Override
	public void warn(String s, Object... objects) {
		logger.warn(s);
	}

	@Override
	public void warn(String s, Object o, Object o1) {
		logger.warn(s);
	}

	@Override
	public void warn(String s, Throwable throwable) {
		logger.warn(s, throwable);
	}

	@Override
	public boolean isWarnEnabled(Marker marker) {
		return logger.isWarnEnabled();
	}

	@Override
	public void warn(Marker marker, String s) {
		logger.warn(s);
	}

	@Override
	public void warn(Marker marker, String s, Object o) {
		logger.warn(s);
	}

	@Override
	public void warn(Marker marker, String s, Object o, Object o1) {
		logger.warn(s);
	}

	@Override
	public void warn(Marker marker, String s, Object... objects) {
		logger.warn(s);
	}

	@Override
	public void warn(Marker marker, String s, Throwable throwable) {
		logger.warn(s, throwable);
	}

	@Override
	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}
}
