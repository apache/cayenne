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

package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class ProcedureHandler extends NamespaceAwareNestedTagHandler {

    private static final String PROCEDURE_TAG = "procedure";
    private static final String PROCEDURE_PARAMETER_TAG = "procedure-parameter";

    private DataMap map;

    private Procedure procedure;

    public ProcedureHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler);
        this.map = map;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case PROCEDURE_TAG:
                addProcedure(attributes);
                return true;

            case PROCEDURE_PARAMETER_TAG:
                addProcedureParameter(attributes);
                return true;
        }

        return false;
    }

    private void addProcedure(Attributes attributes) throws SAXException{
        String name = attributes.getValue("name");
        String returningValue = attributes.getValue("returningValue");
        if (null == name) {
            throw new SAXException("ProcedureHandler::addProcedure() - no procedure name.");
        }

        procedure = new Procedure(name);
        procedure.setReturningValue(returningValue != null && returningValue.equalsIgnoreCase(DataMapHandler.TRUE));
        procedure.setSchema(attributes.getValue("schema"));
        procedure.setCatalog(attributes.getValue("catalog"));
        map.addProcedure(procedure);
    }

    private void addProcedureParameter(Attributes attributes) throws SAXException {

        String name = attributes.getValue("name");
        if (name == null) {
            throw new SAXException("ProcedureHandler::addProcedureParameter() - no procedure parameter name.");
        }

        ProcedureParameter parameter = new ProcedureParameter(name);

        String type = attributes.getValue("type");
        if (type != null) {
            parameter.setType(TypesMapping.getSqlTypeByName(type));
        }

        String length = attributes.getValue("length");
        if (length != null) {
            parameter.setMaxLength(Integer.parseInt(length));
        }

        String precision = attributes.getValue("precision");
        if (precision != null) {
            parameter.setPrecision(Integer.parseInt(precision));
        }

        String direction = attributes.getValue("direction");
        if ("in".equals(direction)) {
            parameter.setDirection(ProcedureParameter.IN_PARAMETER);
        } else if ("out".equals(direction)) {
            parameter.setDirection(ProcedureParameter.OUT_PARAMETER);
        } else if ("in_out".equals(direction)) {
            parameter.setDirection(ProcedureParameter.IN_OUT_PARAMETER);
        }

        procedure.addCallParameter(parameter);
    }

    public Procedure getProcedure() {
        return procedure;
    }
}
