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

package org.apache.cayenne.access.translator.select;

import java.io.IOException;
import java.sql.Types;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbAttribute;

/**
 * QualifierTranslator that allows translation of qualifiers that perform comparison with
 * CHAR columns. Some databases require trimming the values for this to work.
 * 
 */
public class TrimmingQualifierTranslator extends QualifierTranslator {

    protected String trimFunction;

    /**
     * Constructor for TrimmingQualifierTranslator.
     */
    public TrimmingQualifierTranslator(QueryAssembler queryAssembler, String trimFunction) {
        super(queryAssembler);
        this.trimFunction = trimFunction;
    }

    /**
     * Adds special handling of CHAR columns.
     */
    @Override
    protected void processColumn(DbAttribute dbAttr) throws IOException {
        if (dbAttr.getType() == Types.CHAR) {
            out.append(trimFunction).append("(");
            super.processColumn(dbAttr);
            out.append(')');
        }
        else {
            super.processColumn(dbAttr);
        }
    }
    
    /**
     * Adds special handling of CHAR columns.
     */
    @Override
    protected void processColumnWithQuoteSqlIdentifiers(DbAttribute dbAttr, Expression pathExp) throws IOException {
        
        if (dbAttr.getType() == Types.CHAR) {
            out.append(trimFunction).append("(");
            super.processColumnWithQuoteSqlIdentifiers(dbAttr, pathExp);
            out.append(')');
        }
        else {
            super.processColumnWithQuoteSqlIdentifiers(dbAttr, pathExp);
        }                 
    }

    /**
     * Returns the trimFunction.
     * 
     * @return String
     */
    public String getTrimFunction() {
        return trimFunction;
    }

    /**
     * Sets the trimFunction.
     * 
     * @param trimFunction The trimFunction to set
     */
    public void setTrimFunction(String trimFunction) {
        this.trimFunction = trimFunction;
    }
}
