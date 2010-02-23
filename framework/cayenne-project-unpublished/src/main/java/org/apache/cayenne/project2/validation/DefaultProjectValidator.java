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
package org.apache.cayenne.project2.validation;

import org.apache.cayenne.configuration.ConfigurationNode;

public class DefaultProjectValidator implements ProjectValidator {

    /* Validators */
    private DataChannelValidator dataChannelValidator;
    private DataNodeValidator nodeValidator;
    private DataMapValidator mapValidator;
    private ObjEntityValidator objEntityValidator;
    private ObjAttributeValidator objAttrValidator;
    private ObjRelationshipValidator objRelValidator;
    private DbEntityValidator dbEntityValidator;
    private DbAttributeValidator dbAttrValidator;
    private DbRelationshipValidator dbRelValidator;
    private EmbeddableAttributeValidator embeddableAttributeValidator;
    private EmbeddableValidator embeddableValidator;
    private ProcedureValidator procedureValidator;
    private ProcedureParameterValidator procedureParameterValidator;
    private SelectQueryValidator selectQueryValidator;
    private ProcedureQueryValidator procedureQueryValidator;
    private EJBQLQueryValidator ejbqlQueryValidator;
    private SQLTemplateValidator sqlTemplateValidator;

    DefaultProjectValidator() {
        dataChannelValidator = new DataChannelValidator();
        nodeValidator = new DataNodeValidator();
        mapValidator = new DataMapValidator();
        objEntityValidator = new ObjEntityValidator();
        objAttrValidator = new ObjAttributeValidator();
        objRelValidator = new ObjRelationshipValidator();
        dbEntityValidator = new DbEntityValidator();
        dbAttrValidator = new DbAttributeValidator();
        dbRelValidator = new DbRelationshipValidator();
        embeddableAttributeValidator = new EmbeddableAttributeValidator();
        embeddableValidator = new EmbeddableValidator();
        procedureValidator = new ProcedureValidator();
        procedureParameterValidator = new ProcedureParameterValidator();
        selectQueryValidator = new SelectQueryValidator();
        procedureQueryValidator = new ProcedureQueryValidator();
        ejbqlQueryValidator = new EJBQLQueryValidator();
        sqlTemplateValidator = new SQLTemplateValidator();
    }

    public ValidationResults validate(ConfigurationNode node) {
        ValidationResults res = new ValidationResults(node, this);
        return res;
    }

    DataChannelValidator getDataChannelValidator() {
        return dataChannelValidator;
    }

    DataNodeValidator getNodeValidator() {
        return nodeValidator;
    }

    DataMapValidator getMapValidator() {
        return mapValidator;
    }

    ObjEntityValidator getObjEntityValidator() {
        return objEntityValidator;
    }

    ObjAttributeValidator getObjAttrValidator() {
        return objAttrValidator;
    }

    ObjRelationshipValidator getObjRelValidator() {
        return objRelValidator;
    }

    DbEntityValidator getDbEntityValidator() {
        return dbEntityValidator;
    }

    DbAttributeValidator getDbAttrValidator() {
        return dbAttrValidator;
    }

    DbRelationshipValidator getDbRelValidator() {
        return dbRelValidator;
    }

    EmbeddableAttributeValidator getEmbeddableAttributeValidator() {
        return embeddableAttributeValidator;
    }

    EmbeddableValidator getEmbeddableValidator() {
        return embeddableValidator;
    }

    ProcedureValidator getProcedureValidator() {
        return procedureValidator;
    }

    ProcedureParameterValidator getProcedureParameterValidator() {
        return procedureParameterValidator;
    }

    SelectQueryValidator getSelectQueryValidator() {
        return selectQueryValidator;
    }

    ProcedureQueryValidator getProcedureQueryValidator() {
        return procedureQueryValidator;
    }

    EJBQLQueryValidator getEjbqlQueryValidator() {
        return ejbqlQueryValidator;
    }

    SQLTemplateValidator getSqlTemplateValidator() {
        return sqlTemplateValidator;
    }
}
