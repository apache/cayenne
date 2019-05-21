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
package org.apache.cayenne.test.jdbc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Parses SQL from a URL source. Expectations for the URL contents:
 * <ul>
 * <li>It has to be UTF-8 encoded.
 * <li>All lines starting with "-- " are treated as comments
 * <li>If a statement separator is supplied, it must be at the end of the line
 * or on its own line.
 * <li>If no separator is supplied, then the entire content body sans comments
 * is treated as a single statement.
 * </ul>
 */
public class SQLReader {

	public static Collection<String> statements(URL sqlSource) throws Exception {
		return statements(sqlSource, null);
	}

	public static Collection<String> statements(URL sqlSource, String separator) throws Exception {

		Collection<String> statements = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(sqlSource.openStream(), "UTF-8"));) {

			String line;
			StringBuilder statement = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				if (appendLine(statement, line, separator)) {
					statements.add(statement.toString());
					statement = new StringBuilder();
				}
			}

			if (statement.length() > 0) {
				statements.add(statement.toString());
			}
		}

		return statements;
	}

	private static boolean appendLine(StringBuilder statement, String line, String separator) {
		if (line.startsWith("-- ")) {
			return false;
		}

		boolean endOfLine = false;

		line = line.trim();
		if (separator != null && line.endsWith(separator)) {
			line = line.substring(0, line.length() - separator.length());
			endOfLine = true;
		}

		if (line.length() > 0) {
			statement.append('\n').append(line);
		}

		return endOfLine;
	}
}
