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
 
package org.apache.cayenne.dba.firebird;

import java.io.IOException;

import org.apache.cayenne.access.translator.select.QualifierTranslator;
import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.dba.oracle.OracleQualifierTranslator;
import org.apache.cayenne.exp.Expression;

public class FirebirdQualifierTranslator extends QualifierTranslator {
    public FirebirdQualifierTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler);
    }

    @Override
    protected void doAppendPart(Expression rootNode) throws IOException {
        if (rootNode == null) {
            return;
        }
        // IN statements with more than 1500 values are denied in Firebird
        // so we need to split one big statement on few smaller ones
        rootNode = rootNode.transform(new OracleQualifierTranslator.INTrimmer());
        rootNode.traverse(this);
    }
}
