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

package org.apache.cayenne.project.validator;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
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
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;

/**
 * Validator of a single node in a project object tree. <i>Do not confuse with
 * org.apache.cayenne.access.DataNode. </i>
 */
public abstract class TreeNodeValidator {

    // initialize singleton validators
    protected static final DomainValidator domainValidator = new DomainValidator();
    protected static final DataNodeValidator nodeValidator = new DataNodeValidator();
    protected static final DataMapValidator mapValidator = new DataMapValidator();
    protected static final ObjEntityValidator objEntityValidator = new ObjEntityValidator();
    protected static final ObjAttributeValidator objAttrValidator = new ObjAttributeValidator();
    protected static final ObjRelationshipValidator objRelValidator = new ObjRelationshipValidator();
    protected static final DbEntityValidator dbEntityValidator = new DbEntityValidator();
    protected static final DbAttributeValidator dbAttrValidator = new DbAttributeValidator();
    protected static final DbRelationshipValidator dbRelValidator = new DbRelationshipValidator();
    protected static final EmbeddableAttributeValidator embeddableAttributeValidator = new EmbeddableAttributeValidator();
    protected static final EmbeddableValidator embeddableValidator = new EmbeddableValidator();

    protected static final ProcedureValidator procedureValidator = new ProcedureValidator();

    protected static final ProcedureParameterValidator procedureParameterValidator = new ProcedureParameterValidator();
    protected static final SelectQueryValidator selectQueryValidator = new SelectQueryValidator();

    protected static final ProcedureQueryValidator procedureQueryValidator = new ProcedureQueryValidator();
    protected static final EJBQLQueryValidator ejbqlQueryValidator = new EJBQLQueryValidator();

    protected static final SQLTemplateValidator sqlTemplateValidator = new SQLTemplateValidator();

    /**
     * Validates an object, appending any validation messages to the validator provided.
     */
    public static void validate(ProjectPath path, Validator validator) {
        Object validatedObj = path.getObject();
        TreeNodeValidator validatorObj = null;

        if (validatedObj instanceof Embeddable) {
            validatorObj = embeddableValidator;
        }
        else if (validatedObj instanceof EmbeddableAttribute) {
            validatorObj = embeddableAttributeValidator;
        }
        else if (validatedObj instanceof ObjAttribute) {
            validatorObj = objAttrValidator;
        }
        else if (validatedObj instanceof ObjRelationship) {
            validatorObj = objRelValidator;
        }
        else if (validatedObj instanceof ObjEntity) {
            validatorObj = objEntityValidator;
        }
        else if (validatedObj instanceof DbAttribute) {
            validatorObj = dbAttrValidator;
        }
        else if (validatedObj instanceof DbRelationship) {
            validatorObj = dbRelValidator;
        }
        else if (validatedObj instanceof DbEntity) {
            validatorObj = dbEntityValidator;
        }
        else if (validatedObj instanceof DataNode) {
            validatorObj = nodeValidator;
        }
        else if (validatedObj instanceof DataMap) {
            validatorObj = mapValidator;
        }
        else if (validatedObj instanceof DataDomain) {
            validatorObj = domainValidator;
        }
        else if (validatedObj instanceof Procedure) {
            validatorObj = procedureValidator;
        }
        else if (validatedObj instanceof ProcedureParameter) {
            validatorObj = procedureParameterValidator;
        }
        else if (validatedObj instanceof SelectQuery) {
            validatorObj = selectQueryValidator;
        }
        else if (validatedObj instanceof SQLTemplate) {
            validatorObj = sqlTemplateValidator;
        }
        else if (validatedObj instanceof ProcedureQuery) {
            validatorObj = procedureQueryValidator;
        }
        else if (validatedObj instanceof EJBQLQuery) {
            validatorObj = ejbqlQueryValidator;
        }
        else {
            // ignore unknown nodes
            return;
        }

        validatorObj.validateObject(path, validator);
    }

    /**
     * Constructor for TreeNodeValidator.
     */
    public TreeNodeValidator() {
        super();
    }

    /**
     * Validates an object, appending any warnings or errors to the validator. Object to
     * be validated is the last object in a <code>treeNodePath</code> array argument.
     * Concrete implementations would expect an object of a specific type. Otherwise,
     * ClassCastException will be thrown.
     */
    public abstract void validateObject(ProjectPath treeNodePath, Validator validator);
}
