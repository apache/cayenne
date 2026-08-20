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

package org.apache.cayenne.access.sqlbuilder;

/**
 * @since 5.0
 */
public interface SQLAppendable {

    SQLAppendable append(String str);

    /**
     * @deprecated unused; no SQL tree node appends a sub-range. Kept for binary compatibility,
     * now delegating to {@link #append(String)}.
     */
    @Deprecated(since = "5.0", forRemoval = true)
    default SQLAppendable append(CharSequence csq, int start, int end) {
        return append(csq.subSequence(start, end).toString());
    }

    SQLAppendable append(char c);

    SQLAppendable append(int i);

    /**
     * Emits a token separator (a single space) ahead of the next SQL token. SQL tree nodes call this instead of
     * embedding a leading space in their literals, so inter-token spacing is owned by the appendable rather than
     * scattered across nodes. The separator is skipped when {@link #suppressNextTokenSeparator()} was just called.
     */
    SQLAppendable appendTokenSeparator();

    /**
     * Suppresses the separator that the next {@link #appendTokenSeparator()} call would emit. A node calls this right
     * after opening a parenthesis so the group hugs its first token — {@code (NAME)} rather than {@code ( NAME)}.
     */
    SQLAppendable suppressNextTokenSeparator();

    SQLAppendable appendQuoted(String str);

    String getSql();
}
