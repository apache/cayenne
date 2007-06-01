/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

package org.objectstyle.cayenne.modeler.dialog.validator;

import javax.swing.JFrame;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.Attribute;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.ProcedureParameter;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.project.validator.ValidationInfo;
import org.objectstyle.cayenne.query.Query;

/** 
 * Superclass of CayenneModeler validation messages.
 * 
 * @author Michael Misha Shengaout 
 * @author Andrei Adamchik
 */
public abstract class ValidationDisplayHandler {
    private static Logger logObj = Logger.getLogger(ValidationDisplayHandler.class);

    public static final int NO_ERROR = ValidationInfo.VALID;
    public static final int WARNING = ValidationInfo.WARNING;
    public static final int ERROR = ValidationInfo.ERROR;

    protected ValidationInfo validationInfo;
    protected DataDomain domain;

    public static ValidationDisplayHandler getErrorMsg(ValidationInfo result) {
        Object validatedObj = result.getValidatedObject();

        ValidationDisplayHandler msg = null;
        if (validatedObj instanceof Attribute) {
            msg = new AttributeErrorMsg(result);
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

    /** Returns the severity of the error message.*/
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