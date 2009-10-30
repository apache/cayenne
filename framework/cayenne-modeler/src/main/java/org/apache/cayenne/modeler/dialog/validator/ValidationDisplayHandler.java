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

package org.apache.cayenne.modeler.dialog.validator;

import javax.swing.JFrame;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.project.validator.ValidationInfo;
import org.apache.cayenne.query.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Superclass of CayenneModeler validation messages.
 * 
 */
public abstract class ValidationDisplayHandler {

    private static Log logObj = LogFactory.getLog(ValidationDisplayHandler.class);

    public static final int NO_ERROR = ValidationInfo.VALID;
    public static final int WARNING = ValidationInfo.WARNING;
    public static final int ERROR = ValidationInfo.ERROR;

    protected ValidationInfo validationInfo;
    protected DataDomain domain;

    public static ValidationDisplayHandler getErrorMsg(ValidationInfo result) {
        Object validatedObj = result.getValidatedObject();

        ValidationDisplayHandler msg = null;
        if (validatedObj instanceof Embeddable) {
            msg = new EmbeddableErrorMsg(result);
        }
        else if (validatedObj instanceof Attribute) {
            msg = new AttributeErrorMsg(result);
        }
        else if (validatedObj instanceof EmbeddableAttribute) {
            msg = new EmbeddableAttributeErrorMsg(result);
        }
        else if (validatedObj instanceof Relationship) {
            msg = new RelationshipErrorMsg(result);
        }
        else if (validatedObj instanceof Entity) {
            msg = new EntityErrorMsg(result);
        }
        else if (validatedObj instanceof DataNode) {
            msg = new DataNodeErrorMsg(result);
        }
        else if (validatedObj instanceof DataMap) {
            msg = new DataMapErrorMsg(result);
        }
        else if (validatedObj instanceof DataDomain) {
            msg = new DomainErrorMsg(result);
        }
        else if (validatedObj instanceof Procedure) {
            msg = new ProcedureErrorMsg(result);
        }
        else if (validatedObj instanceof ProcedureParameter) {
            msg = new ProcedureParameterErrorMsg(result);
        }
        else if (validatedObj instanceof Query) {
            msg = new QueryErrorMsg(result);
        }
        else {
            // do nothing ... this maybe a project node that is not displayed
            logObj.info("unknown project node: " + validatedObj);
            msg = new NullHanlder(result);
        }

        return msg;
    }

    public ValidationDisplayHandler(ValidationInfo validationInfo) {
        this.validationInfo = validationInfo;
    }

    /**
     * Fires event to display the screen where error should be corrected.
     */
    public abstract void displayField(ProjectController mediator, JFrame frame);

    /** Returns the text of the error message. */
    public String getMessage() {
        return validationInfo.getMessage();
    }

    /** Returns the severity of the error message. */
    public int getSeverity() {
        return validationInfo.getSeverity();
    }

    public DataDomain getDomain() {
        return domain;
    }

    public void setDomain(DataDomain domain) {
        this.domain = domain;
    }

    public String toString() {
        return getMessage();
    }

    public ProjectPath getPath() {
        return validationInfo.getPath();
    }

    public ValidationInfo getValidationInfo() {
        return validationInfo;
    }

    private static final class NullHanlder extends ValidationDisplayHandler {

        NullHanlder(ValidationInfo info) {
            super(info);
        }

        public void displayField(ProjectController mediator, JFrame frame) {
            // noop
        }
    }
}
