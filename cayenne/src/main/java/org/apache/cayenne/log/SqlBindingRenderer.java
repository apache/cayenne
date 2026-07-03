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

import org.apache.cayenne.access.jdbc.CSParameter;
import org.apache.cayenne.access.jdbc.PSBatchParameter;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.translator.TranslatedBatch;
import org.apache.cayenne.access.translator.TranslatedProcedure;
import org.apache.cayenne.access.translator.TranslatedSQL;
import org.apache.cayenne.access.translator.TranslatedSelect;
import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.access.types.ExtendedType;

import java.util.Map;

/**
 * Renders the parameter bindings of a {@link TranslatedStatement} in the compact {@code bind:[...]} form used by
 * {@link SqlLogger} and by exception messages. Shared so that logged and thrown SQL look identical.
 */
class SqlBindingRenderer {

    /**
     * Appends the {@code bind:[...]} fragment for the given statement to the buffer, or nothing if the statement has
     * no bindings. Batch bindings with more rows than {@code batchRowThreshold} are truncated to first row,
     * {@code ..<eliddedCount>..}, last row.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void appendBindings(StringBuilder buffer, TranslatedStatement statement, int batchRowThreshold) {
        switch (statement) {
            case TranslatedSelect s -> appendParameters(buffer, s.bindings());
            case TranslatedSQL s -> appendParameters(buffer, s.bindings());
            case TranslatedProcedure p -> appendCallParameters(buffer, p.params());
            case TranslatedBatch b -> appendBatch(buffer, b.bindings(), batchRowThreshold);
        }
    }

    /**
     * Appends the generated keys of a single row as a {@code [name:value,...]} fragment, using the same value
     * formatting as parameter bindings.
     */
    public static void appendGeneratedKeys(StringBuilder buffer, Map<String, ?> keys) {
        buffer.append('[');
        boolean first = true;
        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            if (!first) {
                buffer.append(',');
            }
            first = false;
            appendNamedValue(buffer, entry.getKey(), null, entry.getValue());
        }
        buffer.append(']');
    }

    private static void appendParameters(StringBuilder buffer, PSParameter<?>[] bindings) {
        if (bindings.length == 0) {
            return;
        }
        buffer.append("[bind:[");
        for (int i = 0; i < bindings.length; i++) {
            if (i > 0) {
                buffer.append(",");
            }
            PSParameter<?> b = bindings[i];
            appendNamedValue(buffer, b.attribute() != null ? b.attribute().getName() : null, b.binder(), b.value());
        }
        buffer.append("]]");
    }

    private static void appendCallParameters(StringBuilder buffer, CSParameter<?>[] params) {
        if (params.length == 0) {
            return;
        }
        buffer.append("[bind:[");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                buffer.append(",");
            }
            CSParameter<?> b = params[i];
            appendNamedValue(buffer, b.param() != null ? b.param().getName() : null, b.binder(), b.value());
        }
        buffer.append("]]");
    }

    private static void appendBatch(StringBuilder buffer, PSBatchParameter[] bindings, int batchRowThreshold) {
        if (bindings.length == 0) {
            return;
        }

        int rows = bindings[0].values().length;
        if (rows == 0) {
            return;
        }

        // sanity-check the threshold: <= 0 disables truncation (including a misconfigured negative), while 1 or 2
        // clamp to 2 - the smallest value that yields a head/tail split (one row on each side)
        if (batchRowThreshold <= 0) {
            batchRowThreshold = Integer.MAX_VALUE;
        } else if (batchRowThreshold < 2) {
            batchRowThreshold = 2;
        }

        // rows are delimited by their own [...] brackets, so no extra list bracket is needed: a single-row batch
        // reads as [bind:[...]] and a multi-row batch as [bind:[...][...]] - never a doubled [[...]]
        buffer.append("[bind:");
        if (rows > batchRowThreshold) {
            // show up to batchRowThreshold rows split evenly between head and tail, eliding the middle. For an odd
            // threshold the head gets the extra row, so head + tail == batchRowThreshold and the elided count is
            // exactly rows - batchRowThreshold
            int head = (batchRowThreshold + 1) / 2;
            int tail = batchRowThreshold / 2;
            for (int r = 0; r < head; r++) {
                appendBatchRow(buffer, bindings, r);
            }
            buffer.append("..").append(rows - head - tail).append("..");
            for (int r = rows - tail; r < rows; r++) {
                appendBatchRow(buffer, bindings, r);
            }
        } else {
            for (int r = 0; r < rows; r++) {
                appendBatchRow(buffer, bindings, r);
            }
        }
        buffer.append(']');
    }

    private static void appendBatchRow(StringBuilder buffer, PSBatchParameter[] bindings, int row) {
        buffer.append('[');
        appendBatchRowFields(buffer, bindings, row);
        buffer.append(']');
    }

    private static void appendBatchRowFields(StringBuilder buffer, PSBatchParameter[] bindings, int row) {
        for (int j = 0; j < bindings.length; j++) {
            if (j > 0) {
                buffer.append(",");
            }
            PSBatchParameter b = bindings[j];
            appendNamedValue(buffer, b.attribute() != null ? b.attribute().getName() : null, null, b.getValue(row));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void appendNamedValue(StringBuilder buffer, String name, ExtendedType binder, Object value) {
        if (name != null) {
            buffer.append(name).append(':');
        }
        buffer.append(formatValue(binder, value));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String formatValue(ExtendedType binder, Object value) {
        if (binder != null) {
            return binder.toString(value);
        }
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "'" + value + "'";
    }
}
