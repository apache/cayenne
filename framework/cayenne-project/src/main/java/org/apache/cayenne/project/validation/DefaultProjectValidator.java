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
package org.apache.cayenne.project.validation;

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
import org.apache.cayenne.validation.ValidationResult;

/**
 * @since 3.1
 */
public class DefaultProjectValidator implements ProjectValidator {

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
        return node.acceptVisitor(new ValidationVisitor());
    }

    class ValidationVisitor implements ConfigurationNodeVisitor<ValidationResult> {

        private ValidationResult validationResult;

        ValidationVisitor() {
            validationResult = new ValidationResult();
        }

        public ValidationResult visitDataChannelDescriptor(
                DataChannelDescriptor channelDescriptor) {

            dataChannelValidator.validate(channelDescriptor, validationResult);

            for (DataNodeDescriptor node : channelDescriptor.getNodeDescriptors()) {
                visitDataNodeDescriptor(node);
            }

            for (DataMap map : channelDescriptor.getDataMaps()) {
                visitDataMap(map);
            }

            return validationResult;
        }

        public ValidationResult visitDataMap(DataMap dataMap) {
            mapValidator.validate(dataMap, validationResult);
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

            return validationResult;
        }

        public ValidationResult visitDataNodeDescriptor(DataNodeDescriptor nodeDescriptor) {
            nodeValidator.validate(nodeDescriptor, validationResult);
            return validationResult;
        }

        public ValidationResult visitDbAttribute(DbAttribute attribute) {
            dbAttrValidator.validate(attribute, validationResult);
            return validationResult;
        }

        public ValidationResult visitDbEntity(DbEntity entity) {
            dbEntityValidator.validate(entity, validationResult);

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
            return validationResult;
        }

        public ValidationResult visitDbRelationship(DbRelationship relationship) {
            dbRelValidator.validate(relationship, validationResult);
            return validationResult;
        }

        public ValidationResult visitEmbeddable(Embeddable embeddable) {
            embeddableValidator.validate(embeddable, validationResult);
            Iterator<EmbeddableAttribute> it = embeddable.getAttributes().iterator();
            while (it.hasNext()) {
                EmbeddableAttribute attr = it.next();
                visitEmbeddableAttribute(attr);
            }
            return validationResult;
        }

        public ValidationResult visitEmbeddableAttribute(EmbeddableAttribute attribute) {
            embeddableAttributeValidator.validate(attribute, validationResult);
            return validationResult;
        }

        public ValidationResult visitObjAttribute(ObjAttribute attribute) {
            objAttrValidator.validate(attribute, validationResult);
            return validationResult;
        }

        public ValidationResult visitObjEntity(ObjEntity entity) {
            objEntityValidator.validate(entity, validationResult);

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
            return validationResult;
        }

        public ValidationResult visitObjRelationship(ObjRelationship relationship) {
            objRelValidator.validate(relationship, validationResult);
            return validationResult;
        }

        public ValidationResult visitProcedure(Procedure procedure) {
            procedureValidator.validate(procedure, validationResult);
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

            return validationResult;
        }

        public ValidationResult visitProcedureParameter(ProcedureParameter parameter) {
            procedureParameterValidator.validate(parameter, validationResult);
            return validationResult;
        }

        public ValidationResult visitQuery(Query query) {
            if (query instanceof SelectQuery) {
                selectQueryValidator.validate((SelectQuery) query, validationResult);
            }
            else if (query instanceof SQLTemplate) {
                sqlTemplateValidator.validate((SQLTemplate) query, validationResult);
            }
            else if (query instanceof ProcedureQuery) {
                procedureQueryValidator
                        .validate((ProcedureQuery) query, validationResult);
            }
            else if (query instanceof EJBQLQuery) {
                ejbqlQueryValidator.validate((EJBQLQuery) query, validationResult);
            }

            return validationResult;
        }
    }
}
