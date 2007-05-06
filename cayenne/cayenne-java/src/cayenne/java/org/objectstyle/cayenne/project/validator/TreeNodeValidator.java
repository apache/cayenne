/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.project.validator;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.ProcedureParameter;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * Validator of a single node in a project object tree. <i>Do not confuse with
 * org.objectstyle.cayenne.access.DataNode. </i>
 * 
 * @author Andrei Adamchik
 */
public abstract class TreeNodeValidator {

    private static Logger logObj = Logger.getLogger(TreeNodeValidator.class);

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

    protected static final ProcedureValidator procedureValidator = new ProcedureValidator();

    protected static final ProcedureParameterValidator procedureParameterValidator = new ProcedureParameterValidator();
    protected static final SelectQueryValidator selectQueryValidator = new SelectQueryValidator();

    protected static final ProcedureQueryValidator procedureQueryValidator = new ProcedureQueryValidator();

    protected static final SQLTemplateValidator sqlTemplateValidator = new SQLTemplateValidator();

    /**
     * Validates an object, appending any validation messages to the validator provided.
     */
    public static void validate(ProjectPath path, Validator validator) {
        Object validatedObj = path.getObject();
        TreeNodeValidator validatorObj = null;
        if (validatedObj instanceof ObjAttribute) {
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
        else {
            // ignore unknown nodes
            String className = (validatedObj != null)
                    ? validatedObj.getClass().getName()
                    : "(null object)";
            logObj.info("Validation not supported for object of class: " + className);
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