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

import java.util.Iterator;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.validation.SimpleValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

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

    public ValidationResult validate(ConfigurationNode node) {
        ValidationVisitor vis = new ValidationVisitor(this);
        return node.acceptVisitor(vis);
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

class ValidationVisitor implements ConfigurationNodeVisitor<ValidationResult> {

    private ValidationResult validationResults = new ValidationResult();

    private DefaultProjectValidator defaultProjectValidator;

    ValidationVisitor(DefaultProjectValidator defaultProjectValidator) {
        this.defaultProjectValidator = defaultProjectValidator;
    }

    public ValidationResult visitDataChannelDescriptor(
            DataChannelDescriptor channelDescriptor) {
        defaultProjectValidator.getDataChannelValidator().validate(
                channelDescriptor,
                this);
        Iterator<DataNodeDescriptor> it = channelDescriptor
                .getNodeDescriptors()
                .iterator();
        while (it.hasNext()) {
            DataNodeDescriptor node = it.next();
            visitDataNodeDescriptor(node);
        }

        Iterator<DataMap> itMap = channelDescriptor.getDataMaps().iterator();
        while (itMap.hasNext()) {
            DataMap map = itMap.next();
            visitDataMap(map);
        }
        return validationResults;
    }

    public ValidationResult visitDataMap(DataMap dataMap) {
        defaultProjectValidator.getMapValidator().validate(dataMap, this);
        Iterator<Embeddable> itEmb = dataMap.getEmbeddables().iterator();
        while (itEmb.hasNext()) {
            Embeddable emb = itEmb.next();
            visitEmbeddable(emb);
        }

        Iterator<ObjEntity> itObjEnt = dataMap.getObjEntities().iterator();
        while (itObjEnt.hasNext()) {
            ObjEntity ent = itObjEnt.next();
            visitObjEntity(ent);
        }

        Iterator<DbEntity> itDbEnt = dataMap.getDbEntities().iterator();
        while (itDbEnt.hasNext()) {
            DbEntity ent = itDbEnt.next();
            visitDbEntity(ent);
        }

        Iterator<Procedure> itProc = dataMap.getProcedures().iterator();
        while (itProc.hasNext()) {
            Procedure proc = itProc.next();
            visitProcedure(proc);
        }

        Iterator<Query> itQuer = dataMap.getQueries().iterator();
        while (itQuer.hasNext()) {
            Query q = itQuer.next();
            visitQuery(q);
        }

        return validationResults;
    }

    public ValidationResult visitDataNodeDescriptor(DataNodeDescriptor nodeDescriptor) {
        defaultProjectValidator.getNodeValidator().validate(nodeDescriptor, this);
        return validationResults;
    }

    public ValidationResult visitDbAttribute(DbAttribute attribute) {
        defaultProjectValidator.getDbAttrValidator().validate(attribute, this);
        return validationResults;
    }

    public ValidationResult visitDbEntity(DbEntity entity) {
        defaultProjectValidator.getDbEntityValidator().validate(entity, this);

        Iterator<DbAttribute> itAttr = entity.getAttributes().iterator();
        while (itAttr.hasNext()) {
            DbAttribute attr = itAttr.next();
            visitDbAttribute(attr);
        }

        Iterator<DbRelationship> itRel = entity.getRelationships().iterator();
        while (itRel.hasNext()) {
            DbRelationship rel = itRel.next();
            visitDbRelationship(rel);
        }
        return validationResults;
    }

    public ValidationResult visitDbRelationship(DbRelationship relationship) {
        defaultProjectValidator.getDbRelValidator().validate(relationship, this);
        return validationResults;
    }

    public ValidationResult visitEmbeddable(Embeddable embeddable) {
        defaultProjectValidator.getEmbeddableValidator().validate(embeddable, this);
        Iterator<EmbeddableAttribute> it = embeddable.getAttributes().iterator();
        while (it.hasNext()) {
            EmbeddableAttribute attr = it.next();
            visitEmbeddableAttribute(attr);
        }
        return validationResults;
    }

    public ValidationResult visitEmbeddableAttribute(EmbeddableAttribute attribute) {
        defaultProjectValidator.getEmbeddableAttributeValidator().validate(
                attribute,
                this);
        return validationResults;
    }

    public ValidationResult visitObjAttribute(ObjAttribute attribute) {
        defaultProjectValidator.getObjAttrValidator().validate(attribute, this);
        return validationResults;
    }

    public ValidationResult visitObjEntity(ObjEntity entity) {
        defaultProjectValidator.getObjEntityValidator().validate(entity, this);

        Iterator<ObjAttribute> itAttr = entity.getAttributes().iterator();
        while (itAttr.hasNext()) {
            ObjAttribute attr = itAttr.next();
            visitObjAttribute(attr);
        }

        Iterator<ObjRelationship> itRel = entity.getRelationships().iterator();
        while (itRel.hasNext()) {
            ObjRelationship rel = itRel.next();
            visitObjRelationship(rel);
        }
        return validationResults;
    }

    public ValidationResult visitObjRelationship(ObjRelationship relationship) {
        defaultProjectValidator.getObjRelValidator().validate(relationship, this);
        return validationResults;
    }

    public ValidationResult visitProcedure(Procedure procedure) {
        defaultProjectValidator.getProcedureValidator().validate(procedure, this);
        ProcedureParameter parameter = procedure.getResultParam();
        if (parameter != null) {
            visitProcedureParameter(parameter);
        }

        Iterator<ProcedureParameter> itPrOut = procedure
                .getCallOutParameters()
                .iterator();
        while (itPrOut.hasNext()) {
            ProcedureParameter procPar = itPrOut.next();
            visitProcedureParameter(procPar);
        }

        Iterator<ProcedureParameter> itPr = procedure.getCallParameters().iterator();
        while (itPr.hasNext()) {
            ProcedureParameter procPar = itPr.next();
            visitProcedureParameter(procPar);
        }

        return validationResults;
    }

    public ValidationResult visitProcedureParameter(ProcedureParameter parameter) {
        defaultProjectValidator
                .getProcedureParameterValidator()
                .validate(parameter, this);
        return validationResults;
    }

    public ValidationResult visitQuery(Query query) {
        if (query instanceof SelectQuery) {
            defaultProjectValidator.getSelectQueryValidator().validate(query, this);
        }
        else if (query instanceof SQLTemplate) {
            defaultProjectValidator.getSqlTemplateValidator().validate(query, this);
        }
        else if (query instanceof ProcedureQuery) {
            defaultProjectValidator.getProcedureQueryValidator().validate(query, this);
        }
        else if (query instanceof EJBQLQuery) {
            defaultProjectValidator.getEjbqlQueryValidator().validate(query, this);
        }
        else {
            // ignore unknown nodes
            return null;
        }
        return validationResults;
    }

    /**
     * Registers validation result. Increases internally stored max severity if
     * <code>result</code> parameter has a higher severity then the current value. Leaves
     * current value unchanged otherwise.
     */
    public void registerValidated(String message, Object object) {

        SimpleValidationFailure result = new SimpleValidationFailure(object, message);
        validationResults.addFailure(result);
    }

    public void registerError(String message, Object object) {
        registerValidated(message, object);
    }

    public void registerWarning(String message, Object object) {
        registerValidated(message, object);
    }
}
