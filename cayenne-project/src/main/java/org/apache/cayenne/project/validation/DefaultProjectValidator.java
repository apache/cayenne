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
package org.apache.cayenne.project.validation;

import org.apache.cayenne.configuration.ProjectNode;
import org.apache.cayenne.configuration.ProjectNodeVisitor;
import org.apache.cayenne.configuration.Project;
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
import org.apache.cayenne.map.ProcedureQueryDescriptor;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.validation.ValidationResult;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @since 3.1
 */
public class DefaultProjectValidator implements ProjectValidator {

    protected final Map<Class<? extends ProjectNode>, ProjectNodeValidator<?>> validators;
    protected ValidationConfig defaultConfig;

    protected DefaultProjectValidator(Supplier<ValidationConfig> configSupplier) {
        validators = prepareValidators(configSupplier);
    }

    public DefaultProjectValidator() {
        defaultConfig = new ValidationConfig();
        validators = prepareValidators(() -> defaultConfig);
    }

    public ValidationResult validate(ProjectNode node) {
        return node.acceptVisitor(new ValidationVisitor());
    }

    private static Map<Class<? extends ProjectNode>, ProjectNodeValidator<?>> prepareValidators(
            Supplier<ValidationConfig> configSupplier) {
        Map<Class<? extends ProjectNode>, ProjectNodeValidator<?>> validators = new HashMap<>();
        validators.put(Project.class, new DataChannelValidator(configSupplier));
        validators.put(DataMap.class, new DataMapValidator(configSupplier));
        validators.put(ObjEntity.class, new ObjEntityValidator(configSupplier));
        validators.put(ObjAttribute.class, new ObjAttributeValidator(configSupplier));
        validators.put(ObjRelationship.class, new ObjRelationshipValidator(configSupplier));
        validators.put(DbEntity.class, new DbEntityValidator(configSupplier));
        validators.put(DbAttribute.class, new DbAttributeValidator(configSupplier));
        validators.put(DbRelationship.class, new DbRelationshipValidator(configSupplier));
        validators.put(Embeddable.class, new EmbeddableValidator(configSupplier));
        validators.put(EmbeddableAttribute.class, new EmbeddableAttributeValidator(configSupplier));
        validators.put(Procedure.class, new ProcedureValidator(configSupplier));
        validators.put(ProcedureParameter.class, new ProcedureParameterValidator(configSupplier));
        validators.put(SelectQueryDescriptor.class, new SelectQueryValidator(configSupplier));
        validators.put(ProcedureQueryDescriptor.class, new ProcedureQueryValidator(configSupplier));
        validators.put(SQLTemplateDescriptor.class, new SQLTemplateValidator(configSupplier));
        return validators;
    }

    @SuppressWarnings("unchecked")
    protected <T extends ProjectNode> ProjectNodeValidator<T> getValidator(Class<T> node) {
        return (ProjectNodeValidator<T>) validators.get(node);
    }

    class ValidationVisitor implements ProjectNodeVisitor<ValidationResult> {

        private final ValidationResult validationResult;

        ValidationVisitor() {
            validationResult = new ValidationResult();
        }

        public ValidationResult visitProject(Project project) {
            getValidator(Project.class).validate(project, validationResult);

            for (DataMap map : project.getDataMaps()) {
                visitDataMap(map);
            }

            return validationResult;
        }

        public ValidationResult visitDataMap(DataMap dataMap) {
            getValidator(DataMap.class).validate(dataMap, validationResult);
            for (Embeddable emb : dataMap.getEmbeddables()) {
                visitEmbeddable(emb);
            }

            for (ObjEntity ent : dataMap.getObjEntities()) {
                visitObjEntity(ent);
            }

            for (DbEntity ent : dataMap.getDbEntities()) {
                visitDbEntity(ent);
            }

            for (Procedure proc : dataMap.getProcedures()) {
                visitProcedure(proc);
            }

            for (QueryDescriptor q : dataMap.getQueryDescriptors()) {
                visitQuery(q);
            }

            return validationResult;
        }

        public ValidationResult visitDbAttribute(DbAttribute attribute) {
            getValidator(DbAttribute.class).validate(attribute, validationResult);
            return validationResult;
        }

        public ValidationResult visitDbEntity(DbEntity entity) {
            getValidator(DbEntity.class).validate(entity, validationResult);

            for (DbAttribute attr : entity.getAttributes()) {
                visitDbAttribute(attr);
            }

            for (DbRelationship rel : entity.getRelationships()) {
                visitDbRelationship(rel);
            }
            return validationResult;
        }

        public ValidationResult visitDbRelationship(DbRelationship relationship) {
            getValidator(DbRelationship.class).validate(relationship, validationResult);
            return validationResult;
        }

        public ValidationResult visitEmbeddable(Embeddable embeddable) {
            getValidator(Embeddable.class).validate(embeddable, validationResult);
            for (EmbeddableAttribute attr : embeddable.getAttributes()) {
                visitEmbeddableAttribute(attr);
            }
            return validationResult;
        }

        public ValidationResult visitEmbeddableAttribute(EmbeddableAttribute attribute) {
            getValidator(EmbeddableAttribute.class).validate(attribute, validationResult);
            return validationResult;
        }

        public ValidationResult visitObjAttribute(ObjAttribute attribute) {
            getValidator(ObjAttribute.class).validate(attribute, validationResult);
            return validationResult;
        }

        public ValidationResult visitObjEntity(ObjEntity entity) {
            getValidator(ObjEntity.class).validate(entity, validationResult);

            for (ObjAttribute attr : entity.getAttributes()) {
                visitObjAttribute(attr);
            }

            for (ObjRelationship rel : entity.getRelationships()) {
                visitObjRelationship(rel);
            }
            return validationResult;
        }

        public ValidationResult visitObjRelationship(ObjRelationship relationship) {
            getValidator(ObjRelationship.class).validate(relationship, validationResult);
            return validationResult;
        }

        public ValidationResult visitProcedure(Procedure procedure) {
            getValidator(Procedure.class).validate(procedure, validationResult);
            ProcedureParameter parameter = procedure.getResultParam();
            if (parameter != null) {
                visitProcedureParameter(parameter);
            }

            for (ProcedureParameter procPar : procedure.getCallOutParameters()) {
                visitProcedureParameter(procPar);
            }

            for (ProcedureParameter procPar : procedure.getCallParameters()) {
                visitProcedureParameter(procPar);
            }

            return validationResult;
        }

        public ValidationResult visitProcedureParameter(ProcedureParameter parameter) {
            getValidator(ProcedureParameter.class).validate(parameter, validationResult);
            return validationResult;
        }

        public ValidationResult visitQuery(QueryDescriptor query) {
            switch (query) {
                case SelectQueryDescriptor select ->
                        getValidator(SelectQueryDescriptor.class).validate(select, validationResult);
                case SQLTemplateDescriptor sqlTemplate ->
                        getValidator(SQLTemplateDescriptor.class).validate(sqlTemplate, validationResult);
                case ProcedureQueryDescriptor procedureQuery ->
                        getValidator(ProcedureQueryDescriptor.class).validate(procedureQuery, validationResult);
                default -> {
                    // no other query types
                }
            }

            return validationResult;
        }
    }
}
