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
package org.apache.cayenne.dba;

import org.apache.cayenne.CayenneRuntimeException;

import java.io.IOException;

/**
 * Encapsulates a database-specific SQL identifier quoting style (the start and end quote characters).
 *
 * @since 4.0
 */
public interface QuotingStrategy {

    /**
     * A shared no-quote instance that appends identifiers verbatim. Used wherever SQL identifier
     * quoting is disabled.
     */
    QuotingStrategy NONE = NoQuoteQuotingStrategy.INSTANCE;

    /**
     * Appends the start-quote delimiter (e.g. {@code "}, {@code [} or {@code `}). A no-op when quoting is off.
     *
     * @since 5.0
     */
    void appendStart(Appendable out);

    /**
     * Appends the end-quote delimiter (e.g. {@code "}, {@code ]} or {@code `}). A no-op when quoting is off.
     *
     * @since 5.0
     */
    void appendEnd(Appendable out);

    /**
     * Appends a fully-qualified name, joining non-null parts with {@code '.'} and wrapping each part
     * in start/end quotes. {@code null} parts are skipped.
     *
     * @since 5.0
     */
    default void appendFQN(Appendable out, String... parts) {
        try {
            boolean first = true;
            for (String part : parts) {
                if (part == null) {
                    continue;
                }
                if (!first) {
                    out.append('.');
                }
                first = false;
                appendStart(out);
                out.append(part);
                appendEnd(out);
            }
        } catch (IOException e) {
            throw new CayenneRuntimeException("Failed to append identifier", e);
        }
    }

    /**
     * Returns a single quoted identifier.
     *
     * @since 5.0
     */
    default String quoted(String identifier) {
        StringBuilder buffer = new StringBuilder();
        appendStart(buffer);
        buffer.append(identifier);
        appendEnd(buffer);
        return buffer.toString();
    }

    /**
     * Returns a quoted fully-qualified name, joining non-null parts with {@code '.'} and wrapping
     * each part in start/end quotes. {@code null} parts are skipped.
     *
     * @since 5.0
     */
    default String quotedFQN(String... parts) {
        StringBuilder buffer = new StringBuilder();
        appendFQN(buffer, parts);
        return buffer.toString();
    }
}
