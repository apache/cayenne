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

/**
 * @since 3.0
 */
class MavenLogger implements org.apache.commons.logging.Log {

	private org.apache.maven.plugin.logging.Log logger;

	public MavenLogger(AbstractMojo parent) {
		this.logger = parent.getLog();
	}

	public void debug(Object message, Throwable th) {
		logger.debug(String.valueOf(message), th);
	}

	public void debug(Object message) {
		logger.debug(String.valueOf(message));
	}

	public void error(Object message, Throwable th) {
		logger.error(String.valueOf(message), th);
	}

	public void error(Object message) {
		logger.error(String.valueOf(message));
	}

	public void fatal(Object message, Throwable th) {
		logger.error(String.valueOf(message), th);
	}

	public void fatal(Object message) {
		logger.error(String.valueOf(message));
	}

	public void info(Object message, Throwable th) {
		logger.info(String.valueOf(message), th);
	}

	public void info(Object message) {
		logger.info(String.valueOf(message));
	}

	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	public boolean isFatalEnabled() {
		return logger.isErrorEnabled();
	}

	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	public boolean isTraceEnabled() {
		return logger.isDebugEnabled();
	}

	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	public void trace(Object message, Throwable th) {
		logger.debug(String.valueOf(message), th);
	}

	public void trace(Object message) {
		logger.debug(String.valueOf(message));
	}

	public void warn(Object message, Throwable th) {
		logger.warn(String.valueOf(message), th);
	}

	public void warn(Object message) {
		logger.warn(String.valueOf(message));
	}
}
