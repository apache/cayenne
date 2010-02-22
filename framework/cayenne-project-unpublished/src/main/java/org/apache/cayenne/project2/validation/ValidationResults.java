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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.apache.cayenne.project2.Project;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;

public class ValidationResults {

    private List<ValidationInfo> validationResults;
    private int maxSeverity;

    public ValidationResults(ConfigurationNode node, Project project,
            DefaultProjectValidator defaultProjectValidator) {
        ValidationVisitor vis = new ValidationVisitor(
                defaultProjectValidator,
                project);
        validationResults = node.acceptVisitor(vis);
        this.maxSeverity = vis.getMaxSeverity();
    }

    public int getMaxSeverity() {
        return maxSeverity;
    }

    public List<ValidationInfo> getValidationResults() {
        return validationResults;
    }
}

class ValidationVisitor implements ConfigurationNodeVisitor<List<ValidationInfo>> {

    private List<ValidationInfo> validationResults = new ArrayList<ValidationInfo>();
    private int maxSeverity;

    private DefaultProjectValidator defaultProjectValidator;
    private Project project;

    int getMaxSeverity() {
        return maxSeverity;
    }

    Project getProject() {
        return project;
    }

    ValidationVisitor(DefaultProjectValidator defaultProjectValidator,
            Project project) {
        this.defaultProjectValidator = defaultProjectValidator;
        this.project = project;
    }

    public List<ValidationInfo> visitDataChannelDescriptor(
            DataChannelDescriptor channelDescriptor) {
        defaultProjectValidator.getDataChannelValidator().validate(
                channelDescriptor,
                this);
        Iterator<DataNodeDescriptor> it = channelDescriptor
                .getNodeDescriptors()
                .iterator();
        if (it.hasNext()) {
            DataNodeDescriptor node = it.next();
            visitDataNodeDescriptor(node);
        }

        Iterator<DataMap> itMap = channelDescriptor.getDataMaps().iterator();
        if (itMap.hasNext()) {
            DataMap map = itMap.next();
            visitDataMap(map);
        }
        return validationResults;
    }

    public List<ValidationInfo> visitDataMap(DataMap dataMap) {
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

    public List<ValidationInfo> visitDataNodeDescriptor(DataNodeDescriptor nodeDescriptor) {
        defaultProjectValidator.getNodeValidator().validate(nodeDescriptor, this);
        return validationResults;
    }

    public List<ValidationInfo> visitDbAttribute(DbAttribute attribute) {
        defaultProjectValidator.getDbAttrValidator().validate(attribute, this);
        return validationResults;
    }

    public List<ValidationInfo> visitDbEntity(DbEntity entity) {
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

    public List<ValidationInfo> visitDbRelationship(DbRelationship relationship) {
        defaultProjectValidator.getDbRelValidator().validate(relationship, this);
        return validationResults;
    }

    public List<ValidationInfo> visitEmbeddable(Embeddable embeddable) {
        defaultProjectValidator.getEmbeddableValidator().validate(embeddable, this);
        Iterator<EmbeddableAttribute> it = embeddable.getAttributes().iterator();
        while (it.hasNext()) {
            EmbeddableAttribute attr = it.next();
            visitEmbeddableAttribute(attr);
        }
        return validationResults;
    }

    public List<ValidationInfo> visitEmbeddableAttribute(EmbeddableAttribute attribute) {
        defaultProjectValidator.getEmbeddableAttributeValidator().validate(
                attribute,
                this);
        return validationResults;
    }

    public List<ValidationInfo> visitObjAttribute(ObjAttribute attribute) {
        defaultProjectValidator.getObjAttrValidator().validate(attribute, this);
        return validationResults;
    }

    public List<ValidationInfo> visitObjEntity(ObjEntity entity) {
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

    public List<ValidationInfo> visitObjRelationship(ObjRelationship relationship) {
        defaultProjectValidator.getObjRelValidator().validate(relationship, this);
        return validationResults;
    }

    public List<ValidationInfo> visitProcedure(Procedure procedure) {
        defaultProjectValidator.getProcedureValidator().validate(procedure, this);
        ProcedureParameter parameter = procedure.getResultParam();
        visitProcedureParameter(parameter);
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

    public List<ValidationInfo> visitProcedureParameter(ProcedureParameter parameter) {
        defaultProjectValidator
                .getProcedureParameterValidator()
                .validate(parameter, this);
        return validationResults;
    }

    public List<ValidationInfo> visitQuery(Query query) {
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
    public void registerValidated(int severity, String message, Object object) {
        ValidationInfo result = new ValidationInfo(severity, message, object);
        validationResults.add(result);
        if (maxSeverity < severity) {
            maxSeverity = severity;
        }
    }

    public void registerError(String message, Object object) {
        registerValidated(ValidationInfo.ERROR, message, object);
    }

    public void registerWarning(String message, Object object) {
        registerValidated(ValidationInfo.WARNING, message, object);
    }
}