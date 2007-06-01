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

import org.objectstyle.cayenne.access.QueryEngine;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionException;
import org.objectstyle.cayenne.exp.TraversalHelper;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.util.Util;

/**
 * Validator for SelectQueries.
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class SelectQueryValidator extends TreeNodeValidator {

    public void validateObject(ProjectPath treeNodePath, Validator validator) {
        SelectQuery query = (SelectQuery) treeNodePath.getObject();

        validateName(query, treeNodePath, validator);

        // Resolve root to Entity for further validation
        Entity root = validateRoot(query, treeNodePath, validator);

        // validate path-based parts
        if (root != null) {
            validateQualifier(root, query.getQualifier(), treeNodePath, validator);

            Iterator orderings = query.getOrderings().iterator();
            while (orderings.hasNext()) {
                validateOrdering(
                        root,
                        (Ordering) orderings.next(),
                        treeNodePath,
                        validator);
            }

            Iterator prefecthes = query.getPrefetches().iterator();
            while (prefecthes.hasNext()) {
                validatePrefetch(
                        root,
                        (String) prefecthes.next(),
                        treeNodePath,
                        validator);
            }
        }
    }

    protected Entity validateRoot(Query query, ProjectPath path, Validator validator) {
        DataMap map = (DataMap) path.firstInstanceOf(DataMap.class);
        if (query.getRoot() == null && map != null) {
            validator.registerWarning("Query has no root", path);
            return null;
        }

        if (query.getRoot() == map) {
            // map-level query... everything is clean
            return null;
        }

        if (map == null) {
            // maybe standalone entity, otherwise bail...
            return (query.getRoot() instanceof Entity) ? (Entity) query.getRoot() : null;
        }

        // can't validate Class root - it is likely not accessible from here...
        if (query.getRoot() instanceof Class) {
            return null;
        }

        // resolve entity
        QueryEngine parent = (QueryEngine) path.firstInstanceOf(QueryEngine.class);

        if (parent == null) {
            return null;
        }

        Entity entity = parent.getEntityResolver().lookupObjEntity(query);
        if (entity == null) {
            entity = parent.getEntityResolver().lookupDbEntity(query);
        }

        // if no entity is found register warning and return null
        if (entity == null) {
            validator.registerWarning("Unknown query root.", path);
            return null;
        }

        return entity;
    }

    protected void validateName(Query query, ProjectPath path, Validator validator) {
        String name = query.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed SelectQuery.", path);
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

    protected void validateQualifier(
            Entity entity,
            Expression qualifier,
            ProjectPath path,
            Validator validator) {

        try {
            testExpression(entity, qualifier);
        }
        catch (ExpressionException e) {
            validator.registerWarning(buildValidationMessage(
                    e,
                    "Invalid path in qualifier"), path);
        }
    }

    protected void validateOrdering(
            Entity entity,
            Ordering ordering,
            ProjectPath path,
            Validator validator) {

        if (ordering == null) {
            return;
        }

        try {
            testExpression(entity, ordering.getSortSpec());
        }
        catch (ExpressionException e) {
            validator
                    .registerWarning(buildValidationMessage(e, "Invalid ordering"), path);
        }
    }

    protected void validatePrefetch(
            Entity entity,
            String prefetch,
            ProjectPath path,
            Validator validator) {

        if (prefetch == null) {
            return;
        }

        try {
            testExpression(entity, Expression.fromString(prefetch));
        }
        catch (ExpressionException e) {
            validator
                    .registerWarning(buildValidationMessage(e, "Invalid prefetch"), path);
        }
    }

    private void testExpression(Entity rootEntity, Expression exp)
            throws ExpressionException {

        if (exp != null) {
            exp.traverse(new EntityExpressionValidator(rootEntity));
        }
    }

    private String buildValidationMessage(ExpressionException e, String prefix) {
        StringBuffer buffer = new StringBuffer(prefix);
        if (e.getExpressionString() != null) {
            buffer.append(": '").append(e.getExpressionString()).append("'");
        }

        buffer.append(".");
        return buffer.toString();
    }

    final class EntityExpressionValidator extends TraversalHelper {

        Entity rootEntity;

        EntityExpressionValidator(Entity rootEntity) {
            this.rootEntity = rootEntity;
        }

        public void startNode(Expression node, Expression parentNode) {
            // check if path nodes are compatibe with root entity
            if (node.getType() == Expression.OBJ_PATH
                    || node.getType() == Expression.DB_PATH) {
                // this will throw an exception if the path is invalid
                node.evaluate(rootEntity);
            }
        }
    }
}