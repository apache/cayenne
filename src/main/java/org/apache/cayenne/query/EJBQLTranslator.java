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

import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;

/**
 * A translator of {@link EJBQLExpression} into the database SQL.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLTranslator implements EJBQLExpressionVisitor {

    private StringBuffer buffer;

    EJBQLTranslator() {
        this.buffer = new StringBuffer();
    }

    String getSql() {
        return buffer.length() > 0 ? buffer.toString() : null;
    }

    public boolean visitSelect(EJBQLExpression expression) {
        buffer.append("SELECT");
        return true;
    }

    public boolean visitSelectExpression(EJBQLExpression expression) {
        return true;
    }

    public boolean visitIdentificationVariable(EJBQLExpression expression) {
        // if we are within the select expression, this is a name of the objentity.
        return true;
    }

    public boolean visitFrom(EJBQLExpression expression) {
        buffer.append(" FROM");
        return true;
    }
}
