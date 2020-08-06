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

package org.apache.cayenne.modeler.dialog.validator;

import javax.swing.JFrame;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.pref.DataNodeDefaults;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.validation.ValidationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Superclass of CayenneModeler validation messages.
 * 
 */
public abstract class ValidationDisplayHandler {

    private static Logger logObj = LoggerFactory.getLogger(ValidationDisplayHandler.class);

    public static final int NO_ERROR = 0;
    public static final int WARNING = 1;
    public static final int ERROR = 2;

    protected ValidationFailure validationFailure;
    protected DataChannelDescriptor domain;

    public static ValidationDisplayHandler getErrorMsg(ValidationFailure result) {
        Object validatedObj = result.getSource();

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
        else if (validatedObj instanceof DataNodeDefaults) {
            msg = new DataNodeErrorMsg(result);
        }
        else if (validatedObj instanceof DataMap) {
            msg = new DataMapErrorMsg(result);
        }
        else if (validatedObj instanceof DataChannelDescriptor) {
            msg = new DomainErrorMsg(result);
        }
        else if (validatedObj instanceof Procedure) {
            msg = new ProcedureErrorMsg(result);
        }
        else if (validatedObj instanceof ProcedureParameter) {
            msg = new ProcedureParameterErrorMsg(result);
        }
        else if (validatedObj instanceof QueryDescriptor) {
            msg = new QueryErrorMsg(result);
        }
        else {
            // do nothing ... this maybe a project node that is not displayed
            logObj.info("unknown project node: " + validatedObj);
            msg = new NullHanlder(result);
        }

        return msg;
    }

    public ValidationDisplayHandler(ValidationFailure validationFailure) {
        this.validationFailure = validationFailure;
    }

    /**
     * Fires event to display the screen where error should be corrected.
     */
    public abstract void displayField(ProjectController mediator, JFrame frame);

    /** Returns the text of the error message. */
    public String getMessage() {
        return validationFailure.getDescription();
    }

    public DataChannelDescriptor getDomain() {
        return domain;
    }

    public void setDomain(DataChannelDescriptor domain) {
        this.domain = domain;
    }

    public String toString() {
        return getMessage();
    }

    public Object getObject() {
        return validationFailure.getSource();
    }

    public ValidationFailure getValidationFailure() {
        return validationFailure;
    }

    private static final class NullHanlder extends ValidationDisplayHandler {

        NullHanlder(ValidationFailure info) {
            super(info);
        }

        public void displayField(ProjectController mediator, JFrame frame) {
            // noop
        }
    }
}
