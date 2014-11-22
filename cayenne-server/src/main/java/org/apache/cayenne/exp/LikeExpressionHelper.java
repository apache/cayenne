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
package org.apache.cayenne.exp;

import org.apache.cayenne.exp.parser.PatternMatchNode;

/**
 * @since 4.0
 */
class LikeExpressionHelper {

	// presumably only "?" can't be an escape char
	private static final char[] ESCAPE_ALPHABET = new char[] { '\\', '|', '/', ' ' };

	private static final String WILDCARD_SEQUENCE = "%";
	private static final String WILDCARD_ONE = "_";

	static void toContains(PatternMatchNode exp) {
		escape(exp);
		wrap(exp, true, true);
	}

	static void toStartsWith(PatternMatchNode exp) {
		escape(exp);
		wrap(exp, false, true);
	}

	static void toEndsWith(PatternMatchNode exp) {
		escape(exp);
		wrap(exp, true, false);
	}

	static void escape(PatternMatchNode exp) {

	}

	static void wrap(PatternMatchNode exp, boolean start, boolean end) {

		Object pattern = exp.getOperand(1);
		if (pattern instanceof String) {

			StringBuilder buffer = new StringBuilder();
			if (start) {
				buffer.append(WILDCARD_SEQUENCE);
			}

			buffer.append(pattern);

			if (end) {
				buffer.append(WILDCARD_SEQUENCE);
			}

			exp.setOperand(1, buffer.toString());
		}
	}

}
