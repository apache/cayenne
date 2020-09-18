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

package org.apache.cayenne.dba.postgres;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.cayenne.access.sqlbuilder.sqltree.ChildProcessor;
import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LikeNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.TrimmingColumnNode;
import org.apache.cayenne.access.translator.select.TypeAwareSQLTreeProcessor;
import org.apache.cayenne.dba.postgres.sqltree.PositionFunctionNode;
import org.apache.cayenne.dba.postgres.sqltree.PostgresExtractFunctionNode;
import org.apache.cayenne.dba.postgres.sqltree.PostgresLikeNode;
import org.apache.cayenne.dba.postgres.sqltree.PostgresLimitOffsetNode;
import org.apache.cayenne.value.GeoJson;
import org.apache.cayenne.value.Wkt;

/**
 * @since 4.2
 */
public class PostgreSQLTreeProcessor extends TypeAwareSQLTreeProcessor {

    private static final Set<String> EXTRACT_FUNCTION_NAMES = new HashSet<>(Arrays.asList(
        "DAY_OF_MONTH", "DAY", "MONTH", "HOUR", "WEEK", "YEAR", "DAY_OF_WEEK", "DAY_OF_YEAR", "MINUTE", "SECOND"
    ));

    public PostgreSQLTreeProcessor() {
        registerProcessor(NodeType.LIMIT_OFFSET,    (ChildProcessor<LimitOffsetNode>)this::onLimitOffsetNode);
        registerProcessor(NodeType.LIKE,            (ChildProcessor<LikeNode>) this::onLikeNode);
        registerProcessor(NodeType.FUNCTION,        (ChildProcessor<FunctionNode>) this::onFunctionNode);

        registerColumnProcessor(DEFAULT_TYPE, (ChildProcessor<ColumnNode>)(p, c, i)
                -> Optional.of(new TrimmingColumnNode(c)));

        registerColumnProcessor(Wkt.class, (parent, child, i)
                -> Optional.of(wrapInFunction(child, "ST_AsText")));
        registerColumnProcessor(GeoJson.class, (parent, child, i)
                -> Optional.of(wrapInFunction(child, "ST_AsGeoJSON")));

        registerValueProcessor(Wkt.class, (parent, child, i)
                -> Optional.of(wrapInFunction(child, "ST_GeomFromText")));
        registerValueProcessor(GeoJson.class, (parent, child, i)
                -> Optional.of(wrapInFunction(child, "ST_GeomFromGeoJSON")));
    }

    protected Optional<Node> onLimitOffsetNode(Node parent, LimitOffsetNode child, int index) {
        return Optional.of(new PostgresLimitOffsetNode(child));
    }

    protected Optional<Node> onLikeNode(Node parent, LikeNode child, int index) {
        return child.isIgnoreCase()
                ? Optional.of(new PostgresLikeNode(child.isNot(), child.getEscape()))
                : Optional.empty();
    }

    protected Optional<Node> onFunctionNode(Node parent, FunctionNode child, int index) {
        Node replacement = null;
        String functionName = child.getFunctionName();
        if(EXTRACT_FUNCTION_NAMES.contains(functionName)) {
            replacement = new PostgresExtractFunctionNode(functionName);
        } else if("CURRENT_DATE".equals(functionName)
                || "CURRENT_TIME".equals(functionName)
                || "CURRENT_TIMESTAMP".equals(functionName)) {
            replacement = new FunctionNode(functionName, child.getAlias(), false);
        } else if("LOCATE".equals(functionName)) {
            replacement = new PositionFunctionNode(child.getAlias());
        }

        return Optional.ofNullable(replacement);
    }

}
