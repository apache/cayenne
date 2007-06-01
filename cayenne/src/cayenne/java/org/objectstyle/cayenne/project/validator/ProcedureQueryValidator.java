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

import java.util.Iterator;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.util.Util;

/**
 * Validator for ProcedureQueries.
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class ProcedureQueryValidator extends TreeNodeValidator {

    public void validateObject(ProjectPath treeNodePath, Validator validator) {
        ProcedureQuery query = (ProcedureQuery) treeNodePath.getObject();

        validateName(query, treeNodePath, validator);
        validateRoot(query, treeNodePath, validator);
        validateResultType(query, treeNodePath, validator);
    }

    protected void validateRoot(Query query, ProjectPath path, Validator validator) {
        DataMap map = (DataMap) path.firstInstanceOf(DataMap.class);
        Object root = query.getRoot();

        if (root == null && map != null) {
            validator.registerWarning("Query has no root", path);
        }

        // procedure query only supports procedure root
        if (root instanceof Procedure) {
            Procedure procedure = (Procedure) root;

            // procedure may have been deleted...
            if (map != null && map.getProcedure(procedure.getName()) != procedure) {
                validator.registerWarning("Invalid Procedure Root - "
                        + procedure.getName(), path);
            }

            return;
        }

        if (root instanceof String) {
            if (map != null && map.getProcedure(root.toString()) == null) {
                validator.registerWarning("Invalid Procedure Root - " + root, path);
            }
        }
    }

    protected void validateName(Query query, ProjectPath path, Validator validator) {
        String name = query.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed Query.", path);
            return;
        }

        DataMap map = (DataMap) path.getObjectParent();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context
        Iterator it = map.getQueries().iterator();
        while (it.hasNext()) {
            Query otherQuery = (Query) it.next();
            if (otherQuery == query) {
                continue;
            }

            if (name.equals(otherQuery.getName())) {
                validator.registerError("Duplicate Query name: " + name + ".", path);
                break;
            }
        }
    }

    protected void validateResultType(
            ProcedureQuery query,
            ProjectPath path,
            Validator validator) {

        if (query.isSelecting()
                && !query.isFetchingDataRows()
                && query.getResultClassName() == null) {
            validator.registerWarning(
                    "Missing result entity for Procedure query fetching DataObjects.",
                    path);
        }
    }
}