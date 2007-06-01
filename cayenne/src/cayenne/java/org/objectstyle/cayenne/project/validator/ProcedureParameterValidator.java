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
package org.objectstyle.cayenne.project.validator;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.ProcedureParameter;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.util.Util;

/**
 * Validator for stored procedure parameters.
 * 
 * @author Andrei Adamchik
 */
public class ProcedureParameterValidator extends TreeNodeValidator {

    public void validateObject(ProjectPath treeNodePath, Validator validator) {
        ProcedureParameter parameter = (ProcedureParameter) treeNodePath.getObject();

        // Must have name
        if (Util.isEmptyString(parameter.getName())) {
            validator.registerError("Unnamed ProcedureParameter.", treeNodePath);
        }

        // all attributes must have type
        if (parameter.getType() == TypesMapping.NOT_DEFINED) {
            validator.registerWarning("ProcedureParameter has no type.", treeNodePath);
        }

        // VARCHAR and CHAR attributes must have max length
        if (parameter.getMaxLength() < 0
            && (parameter.getType() == java.sql.Types.VARCHAR
                || parameter.getType() == java.sql.Types.CHAR)) {

            validator.registerWarning(
                "Character procedure parameter doesn't have max length.",
                treeNodePath);
        }

        // all attributes must have type
        if (parameter.getDirection() <= 0) {
            validator.registerWarning(
                "ProcedureParameter has no direction.",
                treeNodePath);
        }

    }
}