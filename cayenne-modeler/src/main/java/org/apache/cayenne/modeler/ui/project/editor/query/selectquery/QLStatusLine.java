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
package org.apache.cayenne.modeler.ui.project.editor.query.selectquery;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cayenne.modeler.toolkit.text.QueryStatusLine;
import org.apache.cayenne.projecttools.SelectQueryInfo;

/**
 * A line under the query editor that shows what the query returns and its parameters, or the first problem of the
 * query.
 */
class QLStatusLine extends QueryStatusLine {

    /**
     * Shows the state of the query after it was checked.
     *
     * @param info     the parsed query, or null if there is no query
     * @param problems the problems found in the query, empty if there are none
     */
    void update(SelectQueryInfo info, List<String> problems) {
        if (info == null) {
            clear();
        } else if (!problems.isEmpty()) {
            showErrors(problems);
        } else {
            Set<String> parameters = info.parameterNames();
            String parametersText = parameters.isEmpty()
                    ? "none"
                    : parameters.stream().map(p -> "$" + p).collect(Collectors.joining(", "));
            showOk("Returns: " + resultType(info) + "    Parameters: " + parametersText);
        }
    }

    /**
     * Returns a description of the objects that a valid query returns.
     */
    static String resultType(SelectQueryInfo info) {
        return switch (info.resultKind()) {
            case ENTITY -> info.root().getName();
            case DATA_ROW -> "DataRow (" + info.root().getName() + ")";
            case SCALAR -> "Object (1 column)";
            case ARRAY -> "Object[] (" + info.columnCount() + " columns)";
            case null -> String.valueOf(info.rootName());
        };
    }
}
