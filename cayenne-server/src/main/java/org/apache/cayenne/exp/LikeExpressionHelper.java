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
package org.apache.cayenne.exp;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.parser.PatternMatchNode;

/**
 * @since 4.0
 */
class LikeExpressionHelper {

	private static final char WILDCARD_SEQUENCE = '%';
	private static final char WILDCARD_ONE = '_';
	private static final boolean[] ESCAPE_ALPHABET;
	private static final int ESCAPE_ALPHABET_START = '!';

	static {

		ESCAPE_ALPHABET = new boolean[Byte.MAX_VALUE];
		// exclude certain chars, such as unprintable ones, and ?
		for (int i = ESCAPE_ALPHABET_START; i < Byte.MAX_VALUE; i++) {

			if (i != '?' && i != '\"' && i != '\'' && i != WILDCARD_SEQUENCE && i != WILDCARD_ONE) {
				ESCAPE_ALPHABET[i] = true;
			}
		}
	}

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
		Object pattern = exp.getOperand(1);
		if (pattern instanceof String) {
			// find _ or % and then attempt to escape...

			String pString = pattern.toString();

			int len = pString.length();
			for (int i = 0; i < len; i++) {

				char c = pString.charAt(i);
				if (c == WILDCARD_SEQUENCE || c == WILDCARD_ONE) {
					exp.setOperand(1, escapeFrom(exp, pString, i, len));
					break;
				}
			}
		}
	}

	private static String escapeFrom(PatternMatchNode exp, String pattern, int firstWildcard, int len) {

		boolean[] mutableEscapeAlphabet = new boolean[Byte.MAX_VALUE];
		System.arraycopy(ESCAPE_ALPHABET, ESCAPE_ALPHABET_START, mutableEscapeAlphabet, ESCAPE_ALPHABET_START,
				Byte.MAX_VALUE - ESCAPE_ALPHABET_START);

		// can't use chars already in the pattern, so exclude the ones already
		// taken
		for (int i = 0; i < len; i++) {
			char c = pattern.charAt(i);
			if (c < Byte.MAX_VALUE) {
				mutableEscapeAlphabet[c] = false;
			}
		}

		// find the first available char
		char escapeChar = 0;
		for (int i = ESCAPE_ALPHABET_START; i < Byte.MAX_VALUE; i++) {
			if (mutableEscapeAlphabet[i]) {
				escapeChar = (char) i;
				break;
			}
		}

		if (escapeChar == 0) {
			// if we start seeing this this error in the wild, I guess we'll
			// need to extend escape char set beyond ASCII
			throw new CayenneRuntimeException("Could not properly escape pattern: %s", pattern);
		}
		
		exp.setEscapeChar(escapeChar);

		// build escaped pattern
		StringBuilder buffer = new StringBuilder(len + 1);
		buffer.append(pattern.substring(0, firstWildcard));
		buffer.append(escapeChar).append(pattern.charAt(firstWildcard));

		for (int i = firstWildcard + 1; i < len; i++) {

			char c = pattern.charAt(i);
			if (c == WILDCARD_SEQUENCE || c == WILDCARD_ONE) {
				buffer.append(escapeChar);
			}

			buffer.append(c);
		}

		return buffer.toString();
	}

	private static void wrap(PatternMatchNode exp, boolean start, boolean end) {

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
