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
package org.apache.cayenne.project2.validate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.project.validator.ValidationInfo;
import org.apache.cayenne.project2.Project;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;


public class ConfigurationValidationVisitor implements ConfigurationNodeVisitor {

    protected List<ValidationInfo> validationResults = new ArrayList<ValidationInfo>();
    protected int maxSeverity;
    protected Project project;
    
    /* Validators */
    protected DataChannelValidator dataChannelValidator = new DataChannelValidator();
    protected DataNodeValidator nodeValidator = new DataNodeValidator();
    protected DataMapValidator mapValidator = new DataMapValidator();
    protected ObjEntityValidator objEntityValidator = new ObjEntityValidator();
    protected ObjAttributeValidator objAttrValidator = new ObjAttributeValidator();
    protected ObjRelationshipValidator objRelValidator = new ObjRelationshipValidator();
    protected DbEntityValidator dbEntityValidator = new DbEntityValidator();
    protected DbAttributeValidator dbAttrValidator = new DbAttributeValidator();
    protected DbRelationshipValidator dbRelValidator = new DbRelationshipValidator();
    protected EmbeddableAttributeValidator embeddableAttributeValidator = new EmbeddableAttributeValidator();
    protected EmbeddableValidator embeddableValidator = new EmbeddableValidator();
    protected ProcedureValidator procedureValidator = new ProcedureValidator();
    protected ProcedureParameterValidator procedureParameterValidator = new ProcedureParameterValidator();
    protected SelectQueryValidator selectQueryValidator = new SelectQueryValidator();
    protected ProcedureQueryValidator procedureQueryValidator = new ProcedureQueryValidator();
    protected EJBQLQueryValidator ejbqlQueryValidator = new EJBQLQueryValidator();
    protected SQLTemplateValidator sqlTemplateValidator = new SQLTemplateValidator();
    
    public ConfigurationValidationVisitor(Project project) {
        this.project = project;
    }
    
    public int getMaxSeverity() {
        return maxSeverity;
    }
    
    public Project getProject() {
        return project;
    }

    public Object visitDataChannelDescriptor(DataChannelDescriptor channelDescriptor) {
        dataChannelValidator.validate(channelDescriptor, this);
        Iterator<DataNodeDescriptor> it = channelDescriptor.getNodeDescriptors().iterator();
        if(it.hasNext()){
            DataNodeDescriptor node = it.next();
            visitDataNodeDescriptor(node);
        }
        
        Iterator<DataMap> itMap = channelDescriptor.getDataMaps().iterator();
        if(itMap.hasNext()){
            DataMap map = itMap.next();
            visitDataMap(map);
        }
        return validationResults;
    }

    public Object visitDataMap(DataMap dataMap) {
        mapValidator.validate(dataMap, this);
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

    public Object visitDataNodeDescriptor(DataNodeDescriptor nodeDescriptor) {
        nodeValidator.validate(nodeDescriptor, this);
        return validationResults;
    }

    public Object visitDbAttribute(DbAttribute attribute) {
        dbAttrValidator.validate(attribute, this);
        return validationResults;
    }

    public Object visitDbEntity(DbEntity entity) {
        dbEntityValidator.validate(entity, this);
        
        Iterator<DbAttribute> itAttr = entity.getAttributes().iterator();
        while(itAttr.hasNext()){
            DbAttribute attr = itAttr.next();
            visitDbAttribute(attr);
        }
        
        Iterator<DbRelationship> itRel = entity.getRelationships().iterator();
        while(itRel.hasNext()){
            DbRelationship rel = itRel.next();
            visitDbRelationship(rel);
        }
        return validationResults;
    }

    public Object visitDbRelationship(DbRelationship relationship) {
        return null;
    }

    public Object visitEmbeddable(Embeddable embeddable) {
        embeddableValidator.validate(embeddable, this);
        Iterator<EmbeddableAttribute> it = embeddable.getAttributes().iterator();
        while(it.hasNext()){
            EmbeddableAttribute attr = it.next();
            visitEmbeddableAttribute(attr);
        }
        return validationResults;
    }

    public Object visitEmbeddableAttribute(EmbeddableAttribute attribute) {
        embeddableAttributeValidator.validate(attribute, this);
        return validationResults;
    }

    public Object visitObjAttribute(ObjAttribute attribute) {
        objAttrValidator.validate(attribute, this);
        return validationResults;
    }

    public Object visitObjEntity(ObjEntity entity) {
        objEntityValidator.validate(entity, this);
        
        Iterator<ObjAttribute> itAttr = entity.getAttributes().iterator();
        while(itAttr.hasNext()){
            ObjAttribute attr = itAttr.next();
            visitObjAttribute(attr);
        }
        
        Iterator<ObjRelationship> itRel = entity.getRelationships().iterator();
        while(itRel.hasNext()){
            ObjRelationship rel = itRel.next();
            visitObjRelationship(rel);
        }
        return validationResults;
    }

    public Object visitObjRelationship(ObjRelationship relationship) {
        objRelValidator.validate(relationship, this);
        return validationResults;
    }

    public Object visitProcedure(Procedure procedure) {
        procedureValidator.validate(procedure, this);
        ProcedureParameter parameter = procedure.getResultParam();
        visitProcedureParameter(parameter);
        Iterator<ProcedureParameter> itPrOut = procedure.getCallOutParameters().iterator();
        while(itPrOut.hasNext()){
            ProcedureParameter procPar = itPrOut.next();
            visitProcedureParameter(procPar);
        }
        
        Iterator<ProcedureParameter> itPr = procedure.getCallParameters().iterator();
        while(itPr.hasNext()){
            ProcedureParameter procPar = itPr.next();
            visitProcedureParameter(procPar);
        }
        return validationResults;
    }

    public Object visitProcedureParameter(ProcedureParameter parameter) {
        procedureParameterValidator.validate(parameter, this);
        return validationResults;
    }

    public Object visitQuery(Query query) {
        if (query instanceof SelectQuery) {
            selectQueryValidator.validate(query, this);
        }
        else if (query instanceof SQLTemplate) {
            sqlTemplateValidator.validate(query, this);
        }
        else if (query instanceof ProcedureQuery) {
            procedureQueryValidator.validate(query, this);
        }
        else if (query instanceof EJBQLQuery) {
            ejbqlQueryValidator.validate(query, this);
        }
        else {
            // ignore unknown nodes
            return null;
        }
        return validationResults;
    }
    
    /**
     * Registers validation result. 
     * Increases internally stored max severity if 
     * <code>result</code> parameter has a higher severity then the current value. 
     * Leaves current value unchanged otherwise.
     */
    public void registerValidated(
        int severity,
        String message,
        ProjectPath treeNodePath) {
        ValidationInfo result = new ValidationInfo(severity, message, treeNodePath);
        validationResults.add(result);
        if (maxSeverity < severity) {
            maxSeverity = severity;
        }
    }
    
    public void registerError(String message, ProjectPath treeNodePath) {
        registerValidated(ValidationInfo.ERROR, message, treeNodePath);
    }

    public void registerWarning(String message, ProjectPath treeNodePath) {
        registerValidated(ValidationInfo.WARNING, message, treeNodePath);
    }
    
    /** Return collection of ValidationInfo objects from last validation. */
    public List<ValidationInfo> validationResults() {
        return validationResults;
    }
}
