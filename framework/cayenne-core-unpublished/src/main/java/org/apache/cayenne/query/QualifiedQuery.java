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

import org.apache.cayenne.exp.Expression;

/**
 * An abstract superclass of queries with Expression qualifiers.
 * 
 */
public abstract class QualifiedQuery extends AbstractQuery {

    protected Expression qualifier;

    /**
     * Sets new query qualifier.
     */
    public void setQualifier(Expression qualifier) {
        this.qualifier = qualifier;
    }

    /**
     * Returns query qualifier.
     */
    public Expression getQualifier() {
        return qualifier;
    }

    /**
     * Adds specified qualifier to the existing qualifier joining it using "AND".
     */
    public void andQualifier(Expression e) {
        qualifier = (qualifier != null) ? qualifier.andExp(e) : e;
    }

    /**
     * Adds specified qualifier to the existing qualifier joining it using "OR".
     */
    public void orQualifier(Expression e) {
        qualifier = (qualifier != null) ? qualifier.orExp(e) : e;
    }
}
