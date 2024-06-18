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

package org.apache.cayenne.dba.mysql;

import java.util.Optional;

import org.apache.cayenne.access.sqlbuilder.sqltree.ChildProcessor;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LikeNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.translator.select.TypeAwareSQLTreeProcessor;
import org.apache.cayenne.dba.mysql.sqltree.MysqlLikeNode;
import org.apache.cayenne.dba.mysql.sqltree.MysqlLimitOffsetNode;
import org.apache.cayenne.value.GeoJson;
import org.apache.cayenne.value.Wkt;

/**
 * @since 4.2
 */
public class MySQLTreeProcessor extends TypeAwareSQLTreeProcessor {

    private static final MySQLTreeProcessor INSTANCE_CI = new MySQLTreeProcessor(true);
    private static final MySQLTreeProcessor INSTANCE_CS = new MySQLTreeProcessor(false);

    public static MySQLTreeProcessor getInstance(boolean caseInsensitiveCollations) {
        return caseInsensitiveCollations ? INSTANCE_CI : INSTANCE_CS;
    }

    protected MySQLTreeProcessor(boolean ciCollations) {
        if(ciCollations) {
            // For case insensitive collations we need to use `LIKE BINARY` operator to keep strict `LIKE` semantics
            registerProcessor(NodeType.LIKE, (ChildProcessor<LikeNode>) this::onLikeNode);
        }
        registerProcessor(NodeType.LIMIT_OFFSET, (ChildProcessor<LimitOffsetNode>) this::onLimitOffsetNode);
        registerProcessor(NodeType.FUNCTION, (ChildProcessor<FunctionNode>) this::onFunctionNode);

        registerColumnProcessor(Wkt.class, (parent, child, i)
                -> Optional.of(wrapInFunction(child, "ST_AsText")));
        registerColumnProcessor(GeoJson.class, (parent, child, i)
                -> Optional.of(wrapInFunction(child, "ST_AsGeoJSON")));

        registerValueProcessor(Wkt.class, (parent, child, i)
                -> Optional.of(wrapInFunction(child, "ST_GeomFromText")));
        registerValueProcessor(GeoJson.class, (parent, child, i)
                -> Optional.of(wrapInFunction(child, "ST_GeomFromGeoJSON")));
    }

    protected Optional<Node> onLikeNode(Node parent, LikeNode child, int index) {
        if(!child.isIgnoreCase()) {
            return Optional.of(new MysqlLikeNode(child.isNot(), child.getEscape()));
        }
        return Optional.empty();
    }

    protected Optional<Node> onLimitOffsetNode(Node parent, LimitOffsetNode child, int index) {
        return Optional.of(new MysqlLimitOffsetNode(child.getLimit(), child.getOffset()));
    }

    protected Optional<Node> onFunctionNode(Node parent, FunctionNode child, int index) {
        String functionName = child.getFunctionName();
        if("DAY_OF_MONTH".equals(functionName)
                || "DAY_OF_WEEK".equals(functionName)
                || "DAY_OF_YEAR".equals(functionName)) {
            return Optional.of(new FunctionNode(functionName.replace("_", ""), child.getAlias(), true));
        }
        return Optional.empty();
    }

}
