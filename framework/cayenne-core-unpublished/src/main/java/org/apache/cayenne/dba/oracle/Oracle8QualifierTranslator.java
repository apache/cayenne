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
package org.apache.cayenne.dba.oracle;

import java.io.IOException;

import org.apache.cayenne.access.trans.QueryAssembler;

/**
 * Extends the TrimmingQualifierTranslator that Cayenne normally uses for Oracle.
 * Overrides doAppendPart() to wrap the qualifierBuffer in parentheses if it contains an
 * "OR" expression. This avoids a bug that can happen on Oracle8 if the query also
 * contains a join.
 * 
 * @since 3.0
 */
class Oracle8QualifierTranslator extends OracleQualifierTranslator {

    public Oracle8QualifierTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler);
    }

    @Override
    protected void doAppendPart() throws IOException {
        super.doAppendPart();

        if (out instanceof StringBuilder) {
            StringBuilder buffer = (StringBuilder) out;
            if (buffer.indexOf(" OR ") != -1) {
                buffer.insert(0, '(');
                buffer.append(')');
            }
        }
    }
}
