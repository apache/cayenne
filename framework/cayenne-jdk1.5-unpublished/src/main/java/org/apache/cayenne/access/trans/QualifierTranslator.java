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

package org.apache.cayenne.access.trans;

import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.QualifiedQuery;
import org.apache.cayenne.query.Query;
import org.apache.commons.collections.IteratorUtils;

/** 
 * Translates query qualifier to SQL. Used as a helper class by query translators.
 * 
 * @author Andrus Adamchik
 */
public class QualifierTranslator
    extends QueryAssemblerHelper
    implements TraversalHandler {

    protected StringBuffer qualBuf = new StringBuffer();

    protected DataObjectMatchTranslator objectMatchTranslator;
    protected boolean matchingObject;

    public QualifierTranslator() {
        this(null);
    }

    public QualifierTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler);
    }

    /** Translates query qualifier to SQL WHERE clause. 
     *  Qualifier is obtained from <code>queryAssembler</code> object. 
     */
    public String doTranslation() {
        qualBuf.setLength(0);

        Expression rootNode = extractQualifier();
        if (rootNode == null) {
            return null;
        }

        // build SQL where clause string based on expression
        // (using '?' for object values)
        rootNode.traverse(this);
        return qualBuf.length() > 0 ? qualBuf.toString() : null;
    }

    protected Expression extractQualifier() {
        Query q = queryAssembler.getQuery();

        Expression qualifier = ((QualifiedQuery) q).getQualifier();

        // append Entity qualifiers, taking inheritance into account
        ObjEntity entity = getObjEntity();

        if (entity != null) {
            EntityInheritanceTree tree = queryAssembler
                    .getEntityResolver()
                    .lookupInheritanceTree(entity);
            Expression entityQualifier = (tree != null) ? tree
                    .qualifierForEntityAndSubclasses() : entity.getDeclaredQualifier();
            if (entityQualifier != null) {
                qualifier = (qualifier != null)
                        ? qualifier.andExp(entityQualifier)
                        : entityQualifier;
            }
        }

        return qualifier;
    }

    /**
     * Called before processing an expression to initialize objectMatchTranslator if
     * needed.
     */
    protected void detectObjectMatch(Expression exp) {
        // On demand initialization of
        // objectMatchTranslator is not possible since there may be null
        // object values that would not allow to detect the need for 
        // such translator in the right time (e.g.: null = dbpath)

        matchingObject = false;

        if (exp.getOperandCount() != 2) {
            // only binary expressions are supported
            return;
        }

        // check if there are DataObjects among direct children of the Expression
        for (int i = 0; i < 2; i++) {
            Object op = exp.getOperand(i);
            if (op instanceof Persistent || op instanceof ObjectId) {
                matchingObject = true;

                if (objectMatchTranslator == null) {
                    objectMatchTranslator = new DataObjectMatchTranslator();
                }
                else {
                    objectMatchTranslator.reset();
                }
                break;
            }
        }
    }

    protected void appendObjectMatch() {
        if (!matchingObject || objectMatchTranslator == null) {
            throw new IllegalStateException("An invalid attempt to append object match.");
        }

        // turn off special handling, so that all the methods behave as a superclass's impl.
        matchingObject = false;

        boolean first = true;
        DbRelationship relationship = objectMatchTranslator.getRelationship();
        
        if(!relationship.isToMany() && !relationship.isToPK()) {
            queryAssembler.dbRelationshipAdded(relationship);
        }
        
        Iterator it = objectMatchTranslator.keys();
        while (it.hasNext()) {
            if (first) {
                first = false;
            }
            else {
                qualBuf.append(" AND ");
            }

            String key = (String) it.next();
            DbAttribute attr = objectMatchTranslator.getAttribute(key);
            Object val = objectMatchTranslator.getValue(key);
           
            processColumn(qualBuf, attr, relationship);
            qualBuf.append(objectMatchTranslator.getOperation());
            appendLiteral(qualBuf, val, attr, objectMatchTranslator.getExpression());
        }

        objectMatchTranslator.reset();
    }

    /** Opportunity to insert an operation */
    public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {

        if (!hasMoreChildren) {
            return;
        }

        StringBuffer buf = (matchingObject) ? new StringBuffer() : qualBuf;

        switch (node.getType()) {
            case Expression.AND :
                buf.append(" AND ");
                break;
            case Expression.OR :
                buf.append(" OR ");
                break;
            case Expression.EQUAL_TO :
                // translate NULL as IS NULL
                if (childIndex == 0
                    && node.getOperandCount() == 2
                    && node.getOperand(1) == null) {
                    buf.append(" IS ");
                }
                else {
                    buf.append(" = ");
                }
                break;
            case Expression.NOT_EQUAL_TO :
                // translate NULL as IS NOT NULL
                if (childIndex == 0
                    && node.getOperandCount() == 2
                    && node.getOperand(1) == null) {
                    buf.append(" IS NOT ");
                }
                else {
                    buf.append(" <> ");
                }
                break;
            case Expression.LESS_THAN :
                buf.append(" < ");
                break;
            case Expression.GREATER_THAN :
                buf.append(" > ");
                break;
            case Expression.LESS_THAN_EQUAL_TO :
                buf.append(" <= ");
                break;
            case Expression.GREATER_THAN_EQUAL_TO :
                buf.append(" >= ");
                break;
            case Expression.IN :
                buf.append(" IN ");
                break;
            case Expression.NOT_IN :
                buf.append(" NOT IN ");
                break;
            case Expression.LIKE :
                buf.append(" LIKE ");
                break;
            case Expression.NOT_LIKE :
                buf.append(" NOT LIKE ");
                break;
            case Expression.LIKE_IGNORE_CASE :
                buf.append(") LIKE UPPER(");
                break;
            case Expression.NOT_LIKE_IGNORE_CASE :
                buf.append(") NOT LIKE UPPER(");
                break;
            case Expression.ADD :
                buf.append(" + ");
                break;
            case Expression.SUBTRACT :
                buf.append(" - ");
                break;
            case Expression.MULTIPLY :
                buf.append(" * ");
                break;
            case Expression.DIVIDE :
                buf.append(" / ");
                break;
            case Expression.BETWEEN :
                if (childIndex == 0)
                    buf.append(" BETWEEN ");
                else if (childIndex == 1)
                    buf.append(" AND ");
                break;
            case Expression.NOT_BETWEEN :
                if (childIndex == 0)
                    buf.append(" NOT BETWEEN ");
                else if (childIndex == 1)
                    buf.append(" AND ");
                break;
        }

        if (matchingObject) {
            objectMatchTranslator.setOperation(buf.toString());
            objectMatchTranslator.setExpression(node);
        }
    }

    public void startNode(Expression node, Expression parentNode) {
        int count = node.getOperandCount();
        
        if (count == 2) {
            // binary nodes are the only ones that currently require this
            detectObjectMatch(node);
        }

        if (parenthesisNeeded(node, parentNode)) {
            qualBuf.append('(');
        }
        
        if (count == 0) {
            // not all databases handle true/false
            if (node.getType() == Expression.TRUE) {
                qualBuf.append("1 = 1");
            }
            if (node.getType() == Expression.FALSE) {
                qualBuf.append("1 = 0");
            }
        }

        if (count == 1) {
            if (node.getType() == Expression.NEGATIVE)
                qualBuf.append('-');
            // ignore POSITIVE - it is a NOOP
            // else if(node.getType() == Expression.POSITIVE)
            //     qualBuf.append('+');
            else if (node.getType() == Expression.NOT)
                qualBuf.append("NOT ");
        }
        else if (
            node.getType() == Expression.LIKE_IGNORE_CASE
                || node.getType() == Expression.NOT_LIKE_IGNORE_CASE) {
            qualBuf.append("UPPER(");
        }
    }

    /**
     * @since 1.1
     */
    public void endNode(Expression node, Expression parentNode) {

        // check if we need to use objectMatchTranslator to finish building the expression
        if (node.getOperandCount() == 2 && matchingObject) {
            appendObjectMatch();
        }

        if (parenthesisNeeded(node, parentNode)) {
            qualBuf.append(')');
        }

        if (node.getType() == Expression.LIKE_IGNORE_CASE
            || node.getType() == Expression.NOT_LIKE_IGNORE_CASE) {
            qualBuf.append(')');
        }
    }

    public void objectNode(Object leaf, Expression parentNode) {
        if (parentNode.getType() == Expression.OBJ_PATH) {
            appendObjPath(qualBuf, parentNode);
        }
        else if (parentNode.getType() == Expression.DB_PATH) {
            appendDbPath(qualBuf, parentNode);
        }
        else if (parentNode.getType() == Expression.LIST) {
            appendList(parentNode, paramsDbType(parentNode));
        }
        else {
            appendLiteral(qualBuf, leaf, paramsDbType(parentNode), parentNode);
        }
    }

    protected boolean parenthesisNeeded(Expression node, Expression parentNode) {
        if (parentNode == null)
            return false;

        // only unary expressions can go w/o parenthesis
        if (node.getOperandCount() > 1)
            return true;

        if (node.getType() == Expression.OBJ_PATH)
            return false;

        if (node.getType() == Expression.DB_PATH)
            return false;
        
        return true;
    }

    private final void appendList(Expression listExpr, DbAttribute paramDesc) {
        Iterator it = null;
        Object list = listExpr.getOperand(0);
        if (list instanceof List) {
            it = ((List) list).iterator();
        }
        else if (list instanceof Object[]) {
            it = IteratorUtils.arrayIterator((Object[]) list);
        }
        else {
            String className = (list != null) ? list.getClass().getName() : "<null>";
            throw new IllegalArgumentException(
                "Unsupported type for the list expressions: " + className);
        }

        // process first element outside the loop
        // (unroll loop to avoid condition checking
        if (it.hasNext())
            appendLiteral(qualBuf, it.next(), paramDesc, listExpr);
        else
            return;

        while (it.hasNext()) {
            qualBuf.append(", ");
            appendLiteral(qualBuf, it.next(), paramDesc, listExpr);
        }
    }

    protected void appendLiteral(
        StringBuffer buf,
        Object val,
        DbAttribute attr,
        Expression parentExpression) {

        if (!matchingObject) {
            super.appendLiteral(buf, val, attr, parentExpression);
        }
        else if (val == null || (val instanceof Persistent)) {
            objectMatchTranslator.setDataObject((Persistent) val);
        }
        else if(val instanceof ObjectId) {
            objectMatchTranslator.setObjectId((ObjectId) val);
        }
        else {
            throw new IllegalArgumentException("Attempt to use literal other than DataObject during object match.");
        }
    }

    protected void processRelTermination(StringBuffer buf, DbRelationship rel) {

        if (!matchingObject) {
            super.processRelTermination(buf, rel);
        }
        else {
            if (rel.isToMany()) {
                // append joins
                queryAssembler.dbRelationshipAdded(rel);
            }
            objectMatchTranslator.setRelationship(rel);
        }
    }
}
