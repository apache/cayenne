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
package org.apache.cayenne.access.jdbc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.parser.EJBQLEquals;
import org.apache.cayenne.ejbql.parser.EJBQLIdentificationVariable;
import org.apache.cayenne.ejbql.parser.EJBQLPath;
import org.apache.cayenne.ejbql.parser.EJBQLPositionalInputParameter;
import org.apache.cayenne.ejbql.parser.EJBQLSubselect;
import org.apache.cayenne.ejbql.parser.EJBQLTrimBoth;
import org.apache.cayenne.ejbql.parser.EJBQLTrimSpecification;
import org.apache.cayenne.ejbql.parser.Node;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
public class EJBQLConditionTranslator extends EJBQLBaseVisitor {

    protected EJBQLTranslationContext context;
    protected List<EJBQLMultiColumnOperand> multiColumnOperands;

    public EJBQLConditionTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    protected void addMultiColumnOperand(EJBQLMultiColumnOperand operand) {
        if (multiColumnOperands == null) {
            multiColumnOperands = new ArrayList<EJBQLMultiColumnOperand>(2);
        }

        multiColumnOperands.add(operand);
    }

    public boolean visitAggregate(EJBQLExpression expression) {
        expression.visit(context.getTranslatorFactory().getAggregateColumnTranslator(
                context));
        return false;
    }

    public boolean visitAnd(EJBQLExpression expression, int finishedChildIndex) {
        afterChild(expression, " AND", finishedChildIndex);
        return true;
    }

    public boolean visitBetween(EJBQLExpression expression, int finishedChildIndex) {
        switch (finishedChildIndex) {
            case 0:
                if (expression.isNegated()) {
                    context.append(" NOT");
                }
                context.append(" BETWEEN");
                break;
            case 1:
                context.append(" AND");
                break;
        }

        return true;
    }

    public boolean visitExists(EJBQLExpression expression) {
        context.append(" EXISTS");
        return true;
    }

    public boolean visitIsEmpty(EJBQLExpression expression) {

        // handle as "path is [not] null" (an alt. way would've been a correlated subquery
        // on the target entity)...

        if (expression.isNegated()) {
            context.pushMarker(EJBQLSelectTranslator.makeDistinctMarker(), true);
            context.append(" DISTINCT");
            context.popMarker();
        }

        visitIsNull(expression, -1);
        for (int i = 0; i < expression.getChildrenCount(); i++) {
            expression.getChild(i).visit(this);
            visitIsNull(expression, i);
        }

        return false;
    }

    public boolean visitSize(EJBQLExpression expression) {

        // run as a correlated subquery.
        // see "visitMemberOf" for correlated subquery logic
        // also note that the code below is mostly copy/paste from MEMBER OF method ...
        // maybe there's enough commonality in building correlated subqueries to make it
        // reusable???

        if (expression.getChildrenCount() != 1) {
            throw new EJBQLException("SIZE must have exactly one child, got: "
                    + expression.getChildrenCount());
        }

        if (!(expression.getChild(0) instanceof EJBQLPath)) {
            throw new EJBQLException(
                    "First child of SIZE must be a collection path, got: "
                            + expression.getChild(1));
        }

        EJBQLPath path = (EJBQLPath) expression.getChild(0);

        String id = path.getAbsolutePath();

        String correlatedEntityId = path.getId();
        ClassDescriptor correlatedEntityDescriptor = context
                .getEntityDescriptor(correlatedEntityId);
        String correlatedTableName = correlatedEntityDescriptor
                .getEntity()
                .getDbEntity()
                .getFullyQualifiedName();
        String correlatedTableAlias = context.getTableAlias(
                correlatedEntityId,
                correlatedTableName);

        String subqueryId = context.createIdAlias(id);
        ClassDescriptor targetDescriptor = context.getEntityDescriptor(subqueryId);
        String subqueryTableName = targetDescriptor
                .getEntity()
                .getDbEntity()
                .getFullyQualifiedName();
        String subqueryRootAlias = context.getTableAlias(subqueryId, subqueryTableName);

        context.append(" (SELECT COUNT(1) FROM ");
        // not using "AS" to separate table name and alias name - OpenBase doesn't
        // support "AS", and the rest of the databases do not care
        context.append(subqueryTableName).append(' ').append(subqueryRootAlias);
        context.append(" WHERE");

        // TODO: andrus, 8/11/2007 flattened?
        DbRelationship correlatedJoinRelationship = context.getIncomingRelationships(
                new EJBQLTableId(id)).get(0);
        Iterator<DbJoin> it = correlatedJoinRelationship.getJoins().iterator();
        while (it.hasNext()) {
            DbJoin join = it.next();
            context.append(' ').append(subqueryRootAlias).append('.').append(
                    join.getTargetName()).append(" = ");
            context.append(correlatedTableAlias).append('.').append(join.getSourceName());

            if (it.hasNext()) {
                context.append(" AND");
            }
        }

        context.append(")");

        return false;
    }

    public boolean visitMemberOf(EJBQLExpression expression) {

        // create a correlated subquery, using the following transformation:

        // * Subquery Root is always an entity that is a target of relationship
        // * A subquery has a join based on reverse relationship, pointing to the
        // original ID.
        // * Join must be transled as a part of the subquery WHERE clause instead of
        // FROM.
        // * A condition is added: subquery_root_id = LHS_memberof

        if (expression.getChildrenCount() != 2) {
            throw new EJBQLException("MEMBER OF must have exactly two children, got: "
                    + expression.getChildrenCount());
        }

        if (!(expression.getChild(1) instanceof EJBQLPath)) {
            throw new EJBQLException(
                    "Second child of the MEMBER OF must be a collection path, got: "
                            + expression.getChild(1));
        }

        EJBQLPath path = (EJBQLPath) expression.getChild(1);

        // make sure the ID for the path does not overlap with other condition
        // joins...
        String id = path.getAbsolutePath();

        String correlatedEntityId = path.getId();
        ClassDescriptor correlatedEntityDescriptor = context
                .getEntityDescriptor(correlatedEntityId);
        String correlatedTableName = correlatedEntityDescriptor
                .getEntity()
                .getDbEntity()
                .getFullyQualifiedName();
        String correlatedTableAlias = context.getTableAlias(
                correlatedEntityId,
                correlatedTableName);

        String subqueryId = context.createIdAlias(id);
        ClassDescriptor targetDescriptor = context.getEntityDescriptor(subqueryId);
        String subqueryTableName = targetDescriptor
                .getEntity()
                .getDbEntity()
                .getFullyQualifiedName();
        String subqueryRootAlias = context.getTableAlias(subqueryId, subqueryTableName);

        if (expression.isNegated()) {
            context.append(" NOT");
        }

        context.append(" EXISTS (SELECT 1 FROM ");
        // not using "AS" to separate table name and alias name - OpenBase doesn't
        // support "AS", and the rest of the databases do not care
        context.append(subqueryTableName).append(' ').append(subqueryRootAlias);
        context.append(" WHERE");

        // TODO: andrus, 8/11/2007 flattened?
        DbRelationship correlatedJoinRelationship = context.getIncomingRelationships(
                new EJBQLTableId(id)).get(0);

        for (DbJoin join : correlatedJoinRelationship.getJoins()) {
            context.append(' ').append(subqueryRootAlias).append('.').append(
                    join.getTargetName()).append(" = ");
            context.append(correlatedTableAlias).append('.').append(join.getSourceName());
            context.append(" AND");
        }

        // translate subquery_root_id = LHS_of_memberof
        EJBQLEquals equals = new EJBQLEquals(-1);
        EJBQLIdentificationVariable identifier = new EJBQLIdentificationVariable(-1);
        identifier.setText(subqueryId);
        equals.jjtAddChild(identifier, 0);
        equals.jjtAddChild((Node) expression.getChild(0), 1);
        equals.visit(this);

        context.append(")");

        return false;
    }

    public boolean visitAll(EJBQLExpression expression) {
        context.append(" ALL");
        return true;
    }

    public boolean visitAny(EJBQLExpression expression) {
        context.append(" ANY");
        return true;
    }

    public boolean visitOr(EJBQLExpression expression, int finishedChildIndex) {
        afterChild(expression, " OR", finishedChildIndex);
        return true;
    }

    public boolean visitEquals(EJBQLExpression expression, int finishedChildIndex) {
        switch (finishedChildIndex) {
            case 0:
                context.append(" =");
                break;
            case 1:
                // check multicolumn match condition and undo op insertion and append it
                // from scratch if needed
                if (multiColumnOperands != null) {

                    if (multiColumnOperands.size() != 2) {
                        throw new EJBQLException(
                                "Invalid multi-column equals expression. Expected 2 multi-column operands, got "
                                        + multiColumnOperands.size());
                    }

                    context.trim(2);

                    EJBQLMultiColumnOperand lhs = multiColumnOperands.get(0);
                    EJBQLMultiColumnOperand rhs = multiColumnOperands.get(1);

                    Iterator<?> it = lhs.getKeys().iterator();
                    while (it.hasNext()) {
                        Object key = it.next();

                        lhs.appendValue(key);
                        context.append(" =");
                        rhs.appendValue(key);

                        if (it.hasNext()) {
                            context.append(" AND");
                        }
                    }

                    multiColumnOperands = null;
                }

                break;
        }

        return true;
    }

    public boolean visitNamedInputParameter(EJBQLExpression expression) {
        String parameter = context.bindNamedParameter(expression.getText());
        processParameter(parameter);
        return true;
    }

    public boolean visitNot(EJBQLExpression expression) {
        context.append(" NOT");
        return true;
    }

    public boolean visitNotEquals(EJBQLExpression expression, int finishedChildIndex) {
        switch (finishedChildIndex) {
            case 0:
                context.append(" <>");
                break;
            case 1:
                // check multicolumn match condition and undo op insertion and append it
                // from scratch if needed
                if (multiColumnOperands != null) {

                    if (multiColumnOperands.size() != 2) {
                        throw new EJBQLException(
                                "Invalid multi-column equals expression. Expected 2 multi-column operands, got "
                                        + multiColumnOperands.size());
                    }

                    context.trim(3);

                    EJBQLMultiColumnOperand lhs = multiColumnOperands.get(0);
                    EJBQLMultiColumnOperand rhs = multiColumnOperands.get(1);

                    Iterator<?> it = lhs.getKeys().iterator();
                    while (it.hasNext()) {
                        Object key = it.next();

                        lhs.appendValue(key);
                        context.append(" <>");
                        rhs.appendValue(key);

                        if (it.hasNext()) {
                            context.append(" OR");
                        }
                    }

                    multiColumnOperands = null;
                }

                break;
        }
        return true;
    }

    public boolean visitGreaterThan(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex == 0) {
            context.append(" >");
        }

        return true;
    }

    public boolean visitGreaterOrEqual(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex == 0) {
            context.append(" >=");
        }

        return true;
    }

    public boolean visitLessOrEqual(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex == 0) {
            context.append(" <=");
        }

        return true;
    }

    public boolean visitLessThan(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex == 0) {
            context.append(" <");
        }

        return true;
    }

    public boolean visitLike(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex == 0) {
            if (expression.isNegated()) {
                context.append(" NOT");
            }
            context.append(" LIKE");
        }

        return true;
    }

    public boolean visitIn(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex == 0) {
            if (expression.isNegated()) {
                context.append(" NOT");
            }
            context.append(" IN");

            // a cosmetic hack for preventing extra pair of parenthesis from being
            // appended in 'visitSubselect'
            if (expression.getChildrenCount() == 2
                    && expression.getChild(1) instanceof EJBQLSubselect) {
                visitSubselect(expression.getChild(1));
                return false;
            }

            context.append(" (");
        }
        else if (finishedChildIndex == expression.getChildrenCount() - 1) {
            context.append(")");
        }
        else if (finishedChildIndex > 0) {
            context.append(',');
        }

        return true;
    }

    protected void afterChild(EJBQLExpression e, String text, int childIndex) {
        if (childIndex >= 0) {
            if (childIndex + 1 < e.getChildrenCount()) {
                context.append(text);
            }
        }
    }

    public boolean visitIdentificationVariable(EJBQLExpression expression) {
        // this is a match on a variable, like "x = :x"

        ClassDescriptor descriptor = context.getEntityDescriptor(expression.getText());
        if (descriptor == null) {
            throw new EJBQLException("Invalid identification variable: "
                    + expression.getText());
        }

        DbEntity table = descriptor.getEntity().getDbEntity();
        String alias = context.getTableAlias(expression.getText(), table
                .getFullyQualifiedName());

        Collection<DbAttribute> pks = table.getPrimaryKeys();

        if (pks.size() == 1) {
            DbAttribute pk = pks.iterator().next();
            context.append(' ').append(alias).append('.').append(pk.getName());
        }
        else {
            throw new EJBQLException(
                    "Multi-column PK to-many matches are not yet supported.");
        }
        return false;
    }

    public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {

        expression.visit(new EJBQLPathTranslator(context) {

            protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
                EJBQLConditionTranslator.this.addMultiColumnOperand(operand);
            }
        });
        return false;
    }

    public boolean visitIntegerLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            context.append("null");
        }
        else {
            Object value;

            try {
                value = new Integer(expression.getText());
            }
            catch (NumberFormatException nfex) {
                throw new EJBQLException("Invalid integer: " + expression.getText());
            }

            String var = context.bindParameter(value);
            context.append(" #bind($").append(var).append(" 'INTEGER')");
        }
        return true;
    }

    public boolean visitDecimalLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            context.append("null");
        }
        else {
            Object value;

            try {
                value = new BigDecimal(expression.getText());
            }
            catch (NumberFormatException nfex) {
                throw new EJBQLException("Invalid decimal: " + expression.getText());
            }

            String var = context.bindParameter(value);
            context.append(" #bind($").append(var).append(" 'DECIMAL')");
        }
        return true;
    }

    public boolean visitEscapeCharacter(EJBQLExpression expression) {
        // note that EscapeChar text is already wrapped in single quotes
        context.append(" ESCAPE ").append(expression.getText());
        return false;
    }

    public boolean visitIsNull(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex == 0) {
            context.append(expression.isNegated() ? " IS NOT NULL" : " IS NULL");
        }

        return true;
    }

    public boolean visitPositionalInputParameter(EJBQLPositionalInputParameter expression) {

        String parameter = context.bindPositionalParameter(expression.getPosition());
        processParameter(parameter);
        return true;
    }

    public boolean visitBooleanLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            context.append("null");
        }
        else {
            Object value = new Boolean(expression.getText());
            String var = context.bindParameter(value);
            context.append(" #bind($").append(var).append(" 'BOOLEAN')");
        }

        return true;
    }

    public boolean visitStringLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            context.append("null");
        }
        else {
            // note that String Literal text is already wrapped in single quotes, with
            // quotes that are part of the string escaped.
            context.append(" #bind(").append(expression.getText()).append(" 'VARCHAR')");
        }
        return true;
    }

    public boolean visitSubselect(EJBQLExpression expression) {
        context.append(" (");
        expression.visit(new EJBQLSelectTranslator(context));
        context.append(')');
        return false;
    }

    private void processParameter(String boundName) {
        Object object = context.getBoundParameter(boundName);

        Map<?, ?> map = null;
        if (object instanceof Persistent) {
            map = ((Persistent) object).getObjectId().getIdSnapshot();
        }
        else if (object instanceof ObjectId) {
            map = ((ObjectId) object).getIdSnapshot();
        }
        else if (object instanceof Map) {
            map = (Map<?, ?>) object;
        }

        if (map != null) {
            if (map.size() == 1) {
                context.rebindParameter(boundName, map.values().iterator().next());
            }
            else {
                addMultiColumnOperand(EJBQLMultiColumnOperand.getObjectOperand(
                        context,
                        map));
                return;
            }
        }

        if (object != null) {
            context.append(" #bind($").append(boundName).append(")");
        }
        else {
            // this is a hack to prevent execptions on DB's like Derby for expressions
            // "X = NULL". The 'VARCHAR' parameter is totally bogus, but seems to work on
            // all tested DB's... Also note what JPA spec, chapter 4.11 says: "Comparison
            // or arithmetic operations with a NULL value always yield an unknown value."

            // TODO: andrus 6/28/2007 Ideally we should track the type of the current
            // expression to provide a meaningful type.
            context.append(" #bind($").append(boundName).append(" 'VARCHAR')");
        }
    }

    public boolean visitCurrentDate(EJBQLExpression expression) {
        context.append(" {fn CURDATE()}");
        return false;
    }

    public boolean visitCurrentTime(EJBQLExpression expression) {
        context.append(" {fn CURTIME()}");
        return false;
    }

    public boolean visitCurrentTimestamp(EJBQLExpression expression) {
        context.append(" {fn NOW()}");
        return false;
    }

    public boolean visitAbs(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn ABS(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }

        return true;
    }

    public boolean visitSqrt(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn SQRT(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }

        return true;
    }

    public boolean visitMod(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn MOD(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }
        else {
            context.append(',');
        }

        return true;
    }

    public boolean visitConcat(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn CONCAT(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }
        else {
            context.append(',');
        }

        return true;
    }

    public boolean visitSubstring(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn SUBSTRING(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }
        else {
            context.append(',');
        }

        return true;
    }

    public boolean visitLower(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn LCASE(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }

        return true;
    }

    public boolean visitUpper(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn UCASE(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }

        return true;
    }

    public boolean visitLength(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn LENGTH(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }

        return true;
    }

    public boolean visitLocate(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn LOCATE(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }
        else {
            context.append(',');
        }

        return true;
    }

    public boolean visitTrim(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {

            if (!(expression.getChild(0) instanceof EJBQLTrimSpecification)) {
                context.append(" {fn LTRIM({fn RTRIM(");
            }
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            if (!(expression.getChild(0) instanceof EJBQLTrimSpecification)
                    || expression.getChild(0) instanceof EJBQLTrimBoth) {
                context.append(")})}");
            }
            else {
                context.append(")}");
            }
        }

        return true;
    }

    public boolean visitTrimCharacter(EJBQLExpression expression) {
        // this is expected to be overwritten in adapter-specific translators
        if (!"' '".equals(expression.getText())) {
            throw new UnsupportedOperationException(
                    "TRIM character other than space is not supported by a generic adapter: "
                            + expression.getText());
        }

        return false;
    }

    public boolean visitTrimLeading(EJBQLExpression expression) {
        context.append(" {fn LTRIM(");
        return false;
    }

    public boolean visitTrimTrailing(EJBQLExpression expression) {
        context.append(" {fn RTRIM(");
        return false;
    }

    public boolean visitTrimBoth(EJBQLExpression expression) {
        context.append(" {fn LTRIM({fn RTRIM(");
        return false;
    }
}
