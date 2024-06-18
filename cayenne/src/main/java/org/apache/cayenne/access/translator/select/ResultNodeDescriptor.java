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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.SimpleNodeTreeVisitor;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.parser.ASTAggregateFunctionCall;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.DbAttribute;

/**
 * @since 4.2
 */
class ResultNodeDescriptor {
    private final Node node;
    private final boolean inDataRow;
    private final Property<?> property;

    private boolean isAggregate;
    private CayennePath dataRowKey;
    private DbAttribute dbAttribute;
    private String javaType;

    ResultNodeDescriptor(Node node, boolean inDataRow, Property<?> property, CayennePath dataRowKey) {
        this.node = node;
        this.inDataRow = inDataRow;
        this.property = property;
        this.dataRowKey = dataRowKey;
        this.isAggregate = property != null
                && property.getExpression() instanceof ASTAggregateFunctionCall;
    }

    public void setAggregate(boolean aggregate) {
        isAggregate = aggregate;
    }

    public boolean isAggregate() {
        return isAggregate;
    }

    public boolean isInDataRow() {
        return inDataRow;
    }

    public Property<?> getProperty() {
        return property;
    }

    public Node getNode() {
        return node;
    }

    public String getDataRowKey() {
        if (dataRowKey != null) {
            return dataRowKey.value();
        }
        if (property != null) {
            return property.getAlias();
        }
        if (getDbAttribute() != null) {
            return getDbAttribute().getName();
        }
        return null;
    }

    public void setDataRowKey(CayennePath dataRowKey) {
        this.dataRowKey = dataRowKey;
    }

    public ResultNodeDescriptor setJavaType(String javaType) {
        this.javaType = javaType;
        return this;
    }

    public ResultNodeDescriptor setDbAttribute(DbAttribute dbAttribute) {
        this.dbAttribute = dbAttribute;
        return this;
    }

    public String getJavaType() {
        if (javaType != null) {
            return javaType;
        }
        if (property != null) {
            return property.getType().getCanonicalName();
        }
        if (getDbAttribute() != null) {
            return getDbAttribute().getJavaClass();
        }
        return null;
    }

    public int getJdbcType() {
        if (getDbAttribute() != null) {
            return getDbAttribute().getType();
        }

        if (getProperty() != null) {
            return TypesMapping.getSqlTypeByJava(getProperty().getType());
        }

        return TypesMapping.NOT_DEFINED;
    }

    public DbAttribute getDbAttribute() {
        if (this.dbAttribute != null) {
            return this.dbAttribute;
        }
        DbAttribute[] dbAttribute = {null};
        node.visit(new SimpleNodeTreeVisitor() {
            @Override
            public boolean onNodeStart(Node node) {
                if (node.getType() == NodeType.COLUMN) {
                    dbAttribute[0] = ((ColumnNode) node).getAttribute();
                    return false;
                }
                return true;
            }
        });
        return this.dbAttribute = dbAttribute[0];
    }
}
