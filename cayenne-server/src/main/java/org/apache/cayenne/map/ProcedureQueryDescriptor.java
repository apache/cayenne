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
package org.apache.cayenne.map;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.util.XMLEncoder;

import java.util.Map;

/**
 * @since 4.0
 */
public class ProcedureQueryDescriptor extends QueryDescriptor {

    protected String resultEntityName;

    public ProcedureQueryDescriptor() {
        super(PROCEDURE_QUERY);
    }

    /**
     * Returns result entity name.
     */
    public String getResultEntityName() {
        return resultEntityName;
    }

    /**
     * Sets result entity name.
     */
    public void setResultEntityName(String resultEntityName) {
        this.resultEntityName = resultEntityName;
    }

    @Override
    public ProcedureQuery buildQuery() {
        ProcedureQuery procedureQuery = new ProcedureQuery();

        if (root != null) {
            procedureQuery.setRoot(root);
        }

        procedureQuery.setResultEntityName(this.getResultEntityName());
        procedureQuery.initWithProperties(this.getProperties());

        return procedureQuery;
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("query")
                .attribute("name", getName())
                .attribute("type", getType())
                .attribute("root", QueryDescriptor.PROCEDURE_ROOT);

        String rootString = null;
        if (root instanceof String) {
            rootString = root.toString();
        } else if (root instanceof Procedure) {
            rootString = ((Procedure) root).getName();
        }

        encoder.attribute("root-name", rootString)
                .attribute("result-entity", resultEntityName);

        // print properties
        encodeProperties(encoder);

        delegate.visitQuery(this);
        encoder.end();
    }
}
