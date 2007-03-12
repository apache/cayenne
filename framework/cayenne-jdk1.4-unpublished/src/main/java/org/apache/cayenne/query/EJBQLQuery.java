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

import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.map.EntityResolver;

/**
 * An EJBQL query representation in Cayenne.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class EJBQLQuery extends IndirectQuery {

    protected String ejbqlStatement;

    protected transient EJBQLCompiledExpression expression;

    public EJBQLQuery(String ejbqlStatement) {
        this.ejbqlStatement = ejbqlStatement;
    }

    /**
     * Compiles EJBQL into a SQLTemplate query and returns this query.
     */
    protected Query createReplacementQuery(EntityResolver resolver) {
        EJBQLCompiledExpression expression = getExpression(resolver);

        EJBQLTranslator translator = new EJBQLTranslator(expression);
        String sql = translator.translate();

        return new SQLTemplate(expression.getRootDescriptor().getObjectClass(), sql);
    }

    /**
     * Returns an unparsed EJB QL statement used to initialize this query.
     */
    public String getEjbqlStatement() {
        return ejbqlStatement;
    }

    /**
     * Returns lazily initialized EJBQLCompiledExpression for this query EJBQL.
     */
    EJBQLCompiledExpression getExpression(EntityResolver resolver) throws EJBQLException {
        if (expression == null) {
            this.expression = EJBQLParserFactory.getParser().compile(
                    ejbqlStatement,
                    resolver);
        }

        return expression;
    }
}
