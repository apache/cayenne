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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;

import java.io.IOException;
import java.util.List;

final class ExpHelper {

	private ExpHelper() {
	}

	public static void encodeScalarAsEJBQL(List<Object> parameterAccumulator, Appendable out, Object scalar)
			throws IOException {

		if (null == scalar) {
			out.append("null");
			return;
		}

		if (scalar instanceof Boolean) {
			if ((Boolean) scalar) {
				out.append("true");
			} else {
				out.append("false");
			}
			return;
		}

		if (null != parameterAccumulator) {
			parameterAccumulator.add(scalar);
			out.append('?');
			out.append(Integer.toString(parameterAccumulator.size())); // parameters start at 1
			return;
		}

		if (scalar instanceof Integer || scalar instanceof Long || scalar instanceof Float || scalar instanceof Double) {
			out.append(numericToString((Number)scalar));
			return;
		}

        switch (scalar) {
            case Persistent persistent -> {
                ObjectId id = persistent.getObjectId();
                Object encode = (id != null) ? id : scalar;
                appendAsEscapedString(out, String.valueOf(encode));
                return;
            }
            case Enum<?> e -> {
                out.append("enum:");
                out.append(e.getClass().getName()).append(".").append(e.name());
                return;
            }
            case String ignored -> {
                out.append('\'');
                appendAsEscapedString(out, scalar.toString());
                out.append('\'');
                return;
            }
            default -> {
            }
        }

        throw new IllegalStateException("the scalar type '" + scalar.getClass().getSimpleName()
				+ "' is not supported as a scalar type in EJBQL");
	}

	/**
	 * Utility method that encodes an object that is not an expression node to
	 * String.
	 */
	public static void appendScalarAsString(Appendable out, Object scalar, char quoteChar) throws IOException {
		boolean quote = scalar instanceof String;

		if (quote) {
			out.append(quoteChar);
		}

		// encode only ObjectId for Persistent, ensure that the order of keys is predictable....

		// TODO: should we use UUID here?
        switch (scalar) {
            case Persistent persistent -> {
                ObjectId id = persistent.getObjectId();
                Object encode = (id != null) ? id : scalar;
                appendAsEscapedString(out, String.valueOf(encode));
            }
            case Enum<?> e -> {
                out.append("enum:");
                out.append(e.getClass().getName()).append(".").append(e.name());
            }
            case Number number -> appendAsEscapedString(out, numericToString(number));
            case null, default -> appendAsEscapedString(out, String.valueOf(scalar));
        }

		if (quote) {
			out.append(quoteChar);
		}
	}

	private static String numericToString(Number number) {
		if(number instanceof Long) {
			return number + "L";
		} else if(number instanceof Float) {
			return number + "f";
		}
		return String.valueOf(number);
	}

	/**
	 * Utility method that prints a string to the provided Appendable, escaping special characters.
	 */
	private static void appendAsEscapedString(Appendable out, String source) throws IOException {
		int len = source.length();
		for (int i = 0; i < len; i++) {
			char c = source.charAt(i);

			switch (c) {
			case '\n':
				out.append("\\n");
				continue;
			case '\r':
				out.append("\\r");
				continue;
			case '\t':
				out.append("\\t");
				continue;
			case '\b':
				out.append("\\b");
				continue;
			case '\f':
				out.append("\\f");
				continue;
			case '\\':
				out.append("\\\\");
				continue;
			case '\'':
				out.append("\\'");
				continue;
			case '\"':
				out.append("\\\"");
				continue;
			default:
				out.append(c);
			}
		}
	}
}
