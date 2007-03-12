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
package org.apache.cayenne.query;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLExpression;

/**
 * A translator of {@link EJBQLExpression} statements into the database SQL.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLTranslator extends EJBQLBaseVisitor {

    private Map aliases;
    private StringBuffer buffer;
    private EJBQLCompiledExpression compiledExpression;

    EJBQLTranslator(EJBQLCompiledExpression compiledExpression) {
        super(false);
        this.compiledExpression = compiledExpression;
    }

    String translate() {
        this.buffer = new StringBuffer();
        compiledExpression.getExpression().visit(this);
        return buffer.length() > 0 ? buffer.toString() : null;
    }

    public boolean visitSelect(EJBQLExpression expression) {
        EJBQLSelectTranslator visitor = new EJBQLSelectTranslator(this);
        expression.visit(visitor);
        return false;
    }

    public boolean visitDelete(EJBQLExpression expression) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public boolean visitUpdate(EJBQLExpression expression) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    StringBuffer getBuffer() {
        return buffer;
    }

    EJBQLCompiledExpression getCompiledExpression() {
        return compiledExpression;
    }

    /**
     * Retrieves a SQL alias for the combination of EJBQL id variable and a table name. If
     * such alias hasn't been used, it is created on the fly.
     */
    String createAlias(String idVariable, String tableName) {
        String key = idVariable + ":" + tableName;

        String alias;

        if (aliases != null) {
            alias = (String) aliases.get(key);
        }
        else {
            aliases = new HashMap();
            alias = null;
        }

        if (alias == null) {
            alias = "t" + aliases.size();
            aliases.put(key, alias);
        }

        return alias;
    }
}
