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


package org.apache.cayenne.access.trans;

import java.sql.Types;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;

/** 
 * QualifierTranslator that allows translation of qualifiers that perform
 * comparison with CHAR columns. Some databases require trimming the values for
 * this to work.
 * 
 * @author Andrus Adamchik
 */
public class TrimmingQualifierTranslator extends QualifierTranslator {

    protected String trimFunction;

    /**
     * Constructor for TrimmingQualifierTranslator.
     */
    protected TrimmingQualifierTranslator() {
        super();
    }

    /**
     * Constructor for TrimmingQualifierTranslator.
     */
    public TrimmingQualifierTranslator(
        QueryAssembler queryAssembler,
        String trimFunction) {
        super(queryAssembler);
        this.trimFunction = trimFunction;
    }

    /**
     * Adds special handling of CHAR columns.
     */
    protected void processColumn(StringBuffer buf, DbAttribute dbAttr) {
        if (dbAttr.getType() == Types.CHAR) {
            buf.append(trimFunction).append("(");
            super.processColumn(buf, dbAttr);
            buf.append(')');
        } else {
            super.processColumn(buf, dbAttr);
        }
    }

    /**
     * Adds special handling of CHAR columns.
     */
    protected void processColumn(
        StringBuffer buf,
        DbAttribute dbAttr,
        DbRelationship rel) {
        if (dbAttr.getType() == Types.CHAR) {
            buf.append(trimFunction).append("(");
            super.processColumn(buf, dbAttr, rel);
            buf.append(')');
        } else {
            super.processColumn(buf, dbAttr, rel);
        }
    }

    /**
     * Returns the trimFunction.
     * @return String
     */
    public String getTrimFunction() {
        return trimFunction;
    }

    /**
     * Sets the trimFunction.
     * @param trimFunction The trimFunction to set
     */
    public void setTrimFunction(String trimFunction) {
        this.trimFunction = trimFunction;
    }
}
