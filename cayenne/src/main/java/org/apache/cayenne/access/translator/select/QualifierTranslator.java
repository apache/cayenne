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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.EmbeddableObject;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.sqlbuilder.ExpressionNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.ValueNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.sqltree.BetweenNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.BitwiseNotNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.CaseNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ElseNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.EmptyNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.EqualNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.InNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LikeNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.NegateNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NotEqualNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.NotNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.OpExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ThenNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.WhenNode;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.AndExp;
import org.apache.cayenne.exp.CustomOperatorExp;
import org.apache.cayenne.exp.DbIdPathExp;
import org.apache.cayenne.exp.DbPathExp;
import org.apache.cayenne.exp.ExistsExp;
import org.apache.cayenne.exp.FalseExp;
import org.apache.cayenne.exp.FullObjectExp;
import org.apache.cayenne.exp.FunctionCallExp;
import org.apache.cayenne.exp.NotExp;
import org.apache.cayenne.exp.NotExistsExp;
import org.apache.cayenne.exp.ObjPathExp;
import org.apache.cayenne.exp.OrExp;
import org.apache.cayenne.exp.ScalarExp;
import org.apache.cayenne.exp.SubqueryExp;
import org.apache.cayenne.exp.TrueExp;
import org.apache.cayenne.exp.AggregateConditionExp;
import org.apache.cayenne.exp.PatternMatchExp;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.exp.AddExp;
import org.apache.cayenne.exp.AllExp;
import org.apache.cayenne.exp.AnyExp;
import org.apache.cayenne.exp.AsteriskExp;
import org.apache.cayenne.exp.BetweenExp;
import org.apache.cayenne.exp.BitwiseAndExp;
import org.apache.cayenne.exp.BitwiseLeftShiftExp;
import org.apache.cayenne.exp.BitwiseNotExp;
import org.apache.cayenne.exp.BitwiseOrExp;
import org.apache.cayenne.exp.BitwiseRightShiftExp;
import org.apache.cayenne.exp.BitwiseXorExp;
import org.apache.cayenne.exp.CaseWhenExp;
import org.apache.cayenne.exp.DivideExp;
import org.apache.cayenne.exp.ElseExp;
import org.apache.cayenne.exp.EnclosingObjectExp;
import org.apache.cayenne.exp.EqualExp;
import org.apache.cayenne.exp.GreaterExp;
import org.apache.cayenne.exp.GreaterOrEqualExp;
import org.apache.cayenne.exp.InExp;
import org.apache.cayenne.exp.LessExp;
import org.apache.cayenne.exp.LessOrEqualExp;
import org.apache.cayenne.exp.ListExp;
import org.apache.cayenne.exp.MultiplyExp;
import org.apache.cayenne.exp.NegateExp;
import org.apache.cayenne.exp.NotBetweenExp;
import org.apache.cayenne.exp.NotEqualExp;
import org.apache.cayenne.exp.NotInExp;
import org.apache.cayenne.exp.NotLikeExp;
import org.apache.cayenne.exp.NotLikeIgnoreCaseExp;
import org.apache.cayenne.exp.SubtractExp;
import org.apache.cayenne.exp.ThenExp;
import org.apache.cayenne.exp.WhenExp;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

/**
 * @since 4.2
 */
class QualifierTranslator implements TraversalHandler {

    private final SelectTranslatorContext context;
    private final PathTranslator pathTranslator;
    private final Set<Object> expressionsToSkip;
    private final Deque<Node> nodeStack;
    /**
     * The expression nodes being traversed, innermost on top. Expression nodes don't know their parents, and the
     * translation of a list value needs the node that holds the list.
     */
    private final Deque<Expression> expressionStack;

    private Node currentNode;

    static final String ERR_MSG_ARRAYS_NOT_SUPPORTED = "Arrays are not supported as a arguments in";

    QualifierTranslator(SelectTranslatorContext context) {
        this.context = context;
        this.pathTranslator = context.getPathTranslator();
        // must use identity comparison for the node skipping, or it could skip the wrong node
        // and mess everything up, see for example CAY-2871
        this.expressionsToSkip = Collections.newSetFromMap(new IdentityHashMap<>());
        this.nodeStack = new ArrayDeque<>();
        this.expressionStack = new ArrayDeque<>();
    }

    Node translate(Property<?> property) {
        if (property == null) {
            return null;
        }

        Node result = translate(property.getExpression());
        if (property.getAlias() != null) {
            return aliased(result, property.getAlias()).build();
        }
        return result;
    }

    Node translate(Expression qualifier) {
        if (qualifier == null) {
            return null;
        }

        return translateExpanded(expandExpression(qualifier));
    }

    /**
     * Translates a boolean predicate, such as a WHERE, HAVING or join qualifier. Unlike
     * {@link #translate(Expression)}, a predicate that folds to a constant "true" is translated to null, so that
     * the caller can omit the clause entirely instead of emitting "1=1".
     */
    Node translatePredicate(Expression predicate) {
        if (predicate == null) {
            return null;
        }

        Expression expanded = expandExpression(predicate);
        return expanded instanceof TrueExp ? null : translateExpanded(expanded);
    }

    private Node translateExpanded(Expression qualifier) {
        Node rootNode = new EmptyNode();
        expressionsToSkip.clear();
        boolean hasCurrentNode = currentNode != null;
        if (hasCurrentNode) {
            nodeStack.push(currentNode);
        }

        currentNode = rootNode;
        qualifier.traverse(this);

        if (hasCurrentNode) {
            currentNode = nodeStack.pop();
        } else {
            currentNode = null;
        }

        if (rootNode.getChildrenCount() == 1) {
            // trim empty node
            Node child = rootNode.getChild(0);
            child.setParent(null);
            return child;
        }
        return rootNode;
    }

    /**
     * Preprocess complex expressions that ExpressionFactory can't handle at the creation time, and fold boolean
     * constants that sneak in via things like {@code notInExp(path, emptyCollection)}, so that they don't end up in
     * SQL as "1=1" / "1=0" noise.
     * <br>
     * Right we only expand {@code EXIST} expressions that could spawn several subqueries.
     *
     * @param qualifier to process
     * @return qualifier with preprocessed complex expressions
     */
    Expression expandExpression(Expression qualifier) {
        // the transform is bottom-up, so by the time an AND / OR / NOT is visited, its children are already folded
        return qualifier.transform(o -> {
            if (o instanceof ExistsExp || o instanceof NotExistsExp) {
                return new ExistsExpressionTranslator(context, (Expression) o).translate();
            }
            if (o instanceof AndExp || o instanceof OrExp) {
                return foldAndOr((AggregateConditionExp) o);
            }
            if (o instanceof NotExp not) {
                return foldNot(not);
            }
            return o;
        });
    }

    /**
     * Applies boolean identities to an AND / OR node: "x AND true" is "x", "x AND false" is "false", "x OR false" is
     * "x", "x OR true" is "true". Sound under SQL three-valued logic, as the identities hold for a NULL "x" too.
     * Returns the node itself when there is nothing to fold.
     */
    private static Object foldAndOr(AggregateConditionExp node) {
        boolean and = node instanceof AndExp;
        int count = node.getOperandCount();
        List<Object> kept = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Object operand = node.getOperand(i);

            // a constant that decides the whole expression: "false" for AND, "true" for OR
            if (and ? operand instanceof FalseExp : operand instanceof TrueExp) {
                return operand;
            }

            // a neutral constant: "true" for AND, "false" for OR
            if (and ? operand instanceof TrueExp : operand instanceof FalseExp) {
                continue;
            }

            kept.add(operand);
        }

        if (kept.size() == count) {
            return node;
        }

        return switch (kept.size()) {
            case 0 -> and ? new TrueExp() : new FalseExp();
            case 1 -> kept.get(0);
            default -> and ? new AndExp(kept.toArray()) : new OrExp(kept.toArray());
        };
    }

    private static Object foldNot(NotExp node) {
        if (node.getOperandCount() != 1) {
            return node;
        }

        Object operand = node.getOperand(0);
        if (operand instanceof TrueExp) {
            return new FalseExp();
        }
        if (operand instanceof FalseExp) {
            return new TrueExp();
        }
        return node;
    }

    @Override
    public void startNode(Expression node, Expression parentNode) {
        expressionStack.push(node);
        if (expressionsToSkip.contains(node) || expressionsToSkip.contains(parentNode)) {
            return;
        }
        Node nextNode = expressionNodeToSqlNode(node, parentNode);
        if (nextNode == null) {
            return;
        }
        currentNode.addChild(nextNode);
        nextNode.setParent(currentNode);
        currentNode = nextNode;
    }

    private Node expressionNodeToSqlNode(Expression node, Expression parentNode) {
        // the common node kinds go first: a pattern switch tests its cases in order
        return switch (node) {
            case ObjPathExp path -> processPathTranslationResult(node, parentNode,
                    pathTranslator.translatePath(context.getMetadata().getObjEntity(), path.getPath()));
            case DbIdPathExp path -> processPathTranslationResult(node, parentNode,
                    pathTranslator.translateIdPath(context.getMetadata().getObjEntity(), path.getPath()));
            case DbPathExp path -> processPathTranslationResult(node, parentNode,
                    pathTranslator.translatePath(context.getMetadata().getDbEntity(), path.getPath()));
            case ScalarExp scalar -> {
                if (parentNode != null) {
                    throw new CayenneRuntimeException("Incorrect state, a node %s can't have parent here",
                            node.getClass().getName());
                }
                Object scalarVal = scalar.getValue();
                if (scalarVal instanceof Collection || scalarVal.getClass().isArray()) {
                    throw new CayenneRuntimeException("%s %s", ERR_MSG_ARRAYS_NOT_SUPPORTED, node.getClass().getName());
                } else {
                    objectNode(scalarVal, null);
                }
                yield null;
            }
            case EqualExp e -> new EqualNode();
            case NotEqualExp e -> new NotEqualNode();
            case AndExp e -> new OpExpressionNode("AND");
            case OrExp e -> new OpExpressionNode("OR");
            case InExp e -> new InNode(false);
            case NotInExp e -> new InNode(true);
            case PatternMatchExp match -> new LikeNode(match.isIgnoringCase(),
                    match instanceof NotLikeExp || match instanceof NotLikeIgnoreCaseExp, match.getEscapeChar());
            case FunctionCallExp functionCall -> function(functionCall.getFunctionName()).build();
            case ListExp e -> null;

            case LessExp e -> new OpExpressionNode("<");
            case LessOrEqualExp e -> new OpExpressionNode("<=");
            case GreaterExp e -> new OpExpressionNode(">");
            case GreaterOrEqualExp e -> new OpExpressionNode(">=");
            case BetweenExp e -> new BetweenNode(false);
            case NotBetweenExp e -> new BetweenNode(true);
            case NotExp e -> new NotNode();
            case TrueExp e -> new TextNode(" 1=1");
            case FalseExp e -> new TextNode(" 1=0");

            case AddExp e -> new OpExpressionNode("+");
            case SubtractExp e -> new OpExpressionNode("-");
            case MultiplyExp e -> new OpExpressionNode("*");
            case DivideExp e -> new OpExpressionNode("/");
            case NegateExp e -> {
                // If operand is a scalar null, produce NULL directly (no unary minus)
                if (node.getOperandCount() > 0 && node.getOperand(0) == null) {
                    expressionsToSkip.add(node);
                    yield new ValueNode(null, false, null, false);
                }
                // the minus sign is a prefix that hugs its operand (-1, not - 1)
                yield new NegateNode();
            }
            case BitwiseAndExp e -> new OpExpressionNode("&");
            case BitwiseOrExp e -> new OpExpressionNode("|");
            case BitwiseXorExp e -> new OpExpressionNode("^");
            case BitwiseLeftShiftExp e -> new OpExpressionNode("<<");
            case BitwiseRightShiftExp e -> new OpExpressionNode(">>");
            case BitwiseNotExp e -> new BitwiseNotNode();
            case AsteriskExp e -> new TextNode(" *");
            case CustomOperatorExp op -> new OpExpressionNode(op.getOperator());

            case ExistsExp e -> new FunctionNode("EXISTS", null, false);
            case NotExistsExp e -> new FunctionNode("NOT EXISTS", null, false);
            case AllExp e -> new FunctionNode("ALL", null, false);
            case AnyExp e -> new FunctionNode("ANY", null, false);
            case SubqueryExp subquery -> {
                SelectTranslatorContext subContext = new SelectTranslatorContext(
                        subquery.getQuery(), context.getAdapter(), context.getResolver(), context);
                // skip SQL translation stage for nested translators, it should be performed by root context only
                subContext.setSkipSQLGeneration(true);
                subContext.translate();
                yield subContext.getSelectBuilder().build();
            }
            case EnclosingObjectExp enclosing -> {
                // Translate via parent context's translator
                Expression expression = (Expression) enclosing.getOperand(0);
                if (context.getParentContext() == null) {
                    throw new CayenneRuntimeException(
                            "Unable to translate qualifier, no parent context to use for expression " + node);
                }
                expressionsToSkip.add(expression);
                yield context.getParentContext().getQualifierTranslator().translate(expression);
            }
            case FullObjectExp e -> {
                Collection<DbAttribute> dbAttributes = context.getMetadata().getDbEntity().getPrimaryKeys();
                String alias = context.getTableTree().aliasForPath(CayennePath.EMPTY_PATH);
                if (dbAttributes.size() > 1) {
                    yield createMultiPkMatch(node, parentNode, dbAttributes, alias);
                }
                DbAttribute attribute = dbAttributes.iterator().next();
                yield table(alias).column(attribute).build();
            }

            case CaseWhenExp e -> new CaseNode();
            case WhenExp e -> new WhenNode();
            case ThenExp e -> new ThenNode();
            case ElseExp e -> new ElseNode();
        };
    }

    private Node processPathTranslationResult(Expression node, Expression parentNode, PathTranslationResult result) {
        if (result.getEmbeddable().isPresent()) {
            return createEmbeddableMatch(node, parentNode, result);
        } else if (result.getDbRelationship().isPresent()
                && result.getDbAttributes().size() > 1
                && result.getDbRelationship().get().getTargetEntity().getPrimaryKeys().size() > 1) {
            return createMultiAttributeMatch(node, parentNode, result);
        } else if (result.getDbAttributes().isEmpty()) {
            return new EmptyNode();
        } else {
            String alias = context.getTableTree().aliasForPath(result.getLastAttributePath());
            // special case when the path should be processed in the context of the current join clause
            if (TableTree.CURRENT_ALIAS.equals(alias)) {
                alias = context.getTableTree().nonNullActiveNode().getTableAlias();
            }
            return table(alias).column(result.getLastAttribute()).build();
        }
    }

    private Node createEmbeddableMatch(Expression node, Expression parentNode, PathTranslationResult result) {
        Embeddable embeddable = result.getEmbeddable()
                .orElseThrow(() -> new CayenneRuntimeException("Incorrect path '%s' translation, embeddable expected"
                        , result.getFinalPath()));

        Map<String, Object> valueSnapshot = getEmbeddableValueSnapshot(embeddable, node, parentNode);

        expressionsToSkip.add(node);
        expressionsToSkip.add(parentNode);

        return buildMultiValueComparison(result, valueSnapshot);
    }

    private Map<String, Object> getEmbeddableValueSnapshot(Embeddable embeddable, Expression node, Expression parentNode) {
        int siblings = parentNode.getOperandCount();
        for (int i = 0; i < siblings; i++) {
            Object operand = parentNode.getOperand(i);
            if (node == operand) {
                continue;
            }

            if (operand instanceof EmbeddableObject embeddableObject) {
                Map<String, Object> snapshot = new HashMap<>(embeddable.getAttributes().size());
                embeddable.getAttributeMap().forEach((name, attr) ->
                        snapshot.put(attr.getDbAttributeName(), embeddableObject.readPropertyDirectly(name)));
                return snapshot;
            }
        }

        throw new CayenneRuntimeException("Embeddable attribute ObjPath isn't matched with a valid value.");
    }

    private Node createMultiAttributeMatch(Expression node, Expression parentNode, PathTranslationResult result) {
        DbRelationship relationship = result.getDbRelationship()
                .orElseThrow(() -> new CayenneRuntimeException("Incorrect path '%s' translation, relationship expected"
                        , result.getFinalPath()));

        DbEntity targetEntity = relationship.getTargetEntity();
        if (result.getDbAttributes().size() != targetEntity.getPrimaryKeys().size()) {
            throw new CayenneRuntimeException("Unsupported or incorrect mapping for relationship '%s.%s': " +
                    "target entity has different count of primary keys than count of joins."
                    , relationship.getSourceEntityName(), relationship.getName());
        }

        Map<String, Object> valueSnapshot = getMultiAttributeValueSnapshot(node, parentNode);
        // convert snapshot if we have attributes from source, not target
        if (result.getLastAttribute().getEntity() == relationship.getSourceEntity()) {
            valueSnapshot = relationship.srcFkSnapshotWithTargetSnapshot(valueSnapshot);
        }

        // build compound PK/FK comparison node
        Node multiValueComparison = buildMultiValueComparison(result, valueSnapshot);

        // replace current node with multi value comparison
        Node currentNodeParent = currentNode.getParent();
        currentNodeParent.replaceChild(currentNodeParent.getChildrenCount() - 1, multiValueComparison);
        multiValueComparison.setParent(currentNodeParent);
        currentNode = currentNodeParent;

        // we should skip all related nodes as we build this part of the tree manually
        expressionsToSkip.add(node);
        expressionsToSkip.add(parentNode);
        for (int i = 0; i < parentNode.getOperandCount(); i++) {
            expressionsToSkip.add(parentNode.getOperand(i));
        }

        return null;
    }

    /**
     * Matches the root entity referenced as a whole (i.e. a bare {@link FullObjectExp}) against an
     * {@link ObjectId} or a {@link Persistent}, expanding the comparison over all the PK columns. This is the
     * root-entity counterpart of {@link #createMultiAttributeMatch(Expression, Expression, PathTranslationResult)}.
     */
    private Node createMultiPkMatch(Expression node, Expression parentNode,
                                    Collection<DbAttribute> pkAttributes, String alias) {
        if (parentNode == null) {
            throw new CayenneRuntimeException("Unable to translate reference on entity with more than one PK.");
        }

        Map<String, Object> valueSnapshot = getMultiAttributeValueSnapshot(node, parentNode);
        Node multiValueComparison = buildMultiValueComparison(pkAttributes, alias, valueSnapshot);

        // replace current node with multi value comparison
        Node currentNodeParent = currentNode.getParent();
        currentNodeParent.replaceChild(currentNodeParent.getChildrenCount() - 1, multiValueComparison);
        multiValueComparison.setParent(currentNodeParent);
        currentNode = currentNodeParent;

        // we should skip all related nodes as we build this part of the tree manually
        expressionsToSkip.add(node);
        expressionsToSkip.add(parentNode);
        for (int i = 0; i < parentNode.getOperandCount(); i++) {
            expressionsToSkip.add(parentNode.getOperand(i));
        }

        return null;
    }

    private Map<String, Object> getMultiAttributeValueSnapshot(Expression node, Expression parentNode) {
        int siblings = parentNode.getOperandCount();
        for (int i = 0; i < siblings; i++) {
            Object operand = parentNode.getOperand(i);
            if (node == operand) {
                continue;
            }

            if (operand instanceof Persistent) {
                return ((Persistent) operand).getObjectId().getIdSnapshot();
            } else if (operand instanceof ObjectId) {
                return ((ObjectId) operand).getIdSnapshot();
            } else if (operand instanceof ObjPathExp) {
                // TODO: support comparison of multi attribute ObjPath with other multi attribute ObjPath
                throw new UnsupportedOperationException("Comparison of multiple attributes not supported for ObjPath");
            }
        }

        throw new CayenneRuntimeException("Multi attribute ObjPath isn't matched with valid value. " +
                "List or Persistent object required.");
    }

    private Node buildMultiValueComparison(PathTranslationResult result, Map<String, Object> valueSnapshot) {
        CayennePath path = result.getLastAttributePath();
        String alias = context.getTableTree().aliasForPath(path);
        return buildMultiValueComparison(result.getDbAttributes(), alias, valueSnapshot);
    }

    private Node buildMultiValueComparison(Collection<DbAttribute> attributes, String alias,
                                           Map<String, Object> valueSnapshot) {
        ExpressionNodeBuilder expressionNodeBuilder = null;
        ExpressionNodeBuilder eq;

        for (DbAttribute attribute : attributes) {
            Object nextValue = valueSnapshot.get(attribute.getName());
            eq = table(alias).column(attribute).eq(value(nextValue));
            if (expressionNodeBuilder == null) {
                expressionNodeBuilder = eq;
            } else {
                expressionNodeBuilder = expressionNodeBuilder.and(eq);
            }
        }

        return expressionNodeBuilder.build();
    }

    private boolean nodeProcessed(Expression node) {
        // must be in sync with expressionNodeToSqlNode(): every node kind but a list opens a SQL node
        return !(node instanceof ListExp);
    }

    @Override
    public void endNode(Expression node, Expression parentNode) {
        expressionStack.pop();
        if (expressionsToSkip.contains(node) || expressionsToSkip.contains(parentNode)) {
            return;
        }

        if (nodeProcessed(node)) {
            if (currentNode.getParent() != null) {
                currentNode = currentNode.getParent();
            }
        }
    }

    @Override
    public void objectNode(Object leaf, Expression parentNode) {
        if (expressionsToSkip.contains(parentNode)) {
            return;
        }
        if (parentNode != null &&
                (parentNode instanceof ObjPathExp
                        || parentNode instanceof DbPathExp
                        || parentNode instanceof DbIdPathExp)) {
            return;
        }

        ValueNodeBuilder valueNodeBuilder = value(leaf)
                .needBinding(needBinding(parentNode))
                .attribute(findDbAttribute(parentNode));
        if (parentNode != null && parentNode instanceof ListExp) {
            valueNodeBuilder.array(true);
        }
        Node nextNode = valueNodeBuilder.build();

        currentNode.addChild(nextNode);
        nextNode.setParent(currentNode);
    }

    /**
     * Returns the parent of the given node in the expression being traversed, or null for the root or a node that is
     * not being traversed.
     */
    private Expression enclosingExpression(Expression node) {
        Iterator<Expression> ancestors = expressionStack.iterator();
        while (ancestors.hasNext()) {
            if (ancestors.next() == node) {
                return ancestors.hasNext() ? ancestors.next() : null;
            }
        }
        return null;
    }

    private boolean needBinding(Expression parentNode) {
        return (parentNode != null);
    }

    protected DbAttribute findDbAttribute(Expression node) {
        if (node == null) {
            return null;
        }
        if (node instanceof ListExp) {
            // the attribute is an operand of the node holding the list, e.g. the path in "path in (list)"
            Expression parent = enclosingExpression(node);
            if (parent != null) {
                node = parent;
            } else {
                return null;
            }
        } else if (node instanceof FunctionCallExp) {
            return null;
        }

        PathTranslationResult result = null;
        for (int i = 0; i < node.getOperandCount(); i++) {
            Object op = node.getOperand(i);
            if (op instanceof ObjPathExp) {
                result = pathTranslator.translatePath(context.getMetadata().getObjEntity(), ((ObjPathExp) op).getPath());
                break;
            } else if (op instanceof DbIdPathExp) {
                result = pathTranslator.translateIdPath(context.getMetadata().getObjEntity(), ((DbIdPathExp) op).getPath());
                break;
            } else if (op instanceof DbPathExp) {
                result = pathTranslator.translatePath(context.getMetadata().getDbEntity(), ((DbPathExp) op).getPath());
                break;
            }
        }

        if (result == null) {
            return null;
        }

        return result.getLastAttribute();
    }
}
