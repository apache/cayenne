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
import org.apache.cayenne.exp.parser.ASTCustomOperator;
import org.apache.cayenne.exp.parser.ASTDbIdPath;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTExists;
import org.apache.cayenne.exp.parser.ASTFullObject;
import org.apache.cayenne.exp.parser.ASTFunctionCall;
import org.apache.cayenne.exp.parser.ASTNotExists;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.ASTScalar;
import org.apache.cayenne.exp.parser.ASTSubquery;
import org.apache.cayenne.exp.parser.PatternMatchNode;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;
import static org.apache.cayenne.exp.Expression.*;

/**
 * @since 4.2
 */
class QualifierTranslator implements TraversalHandler {

    private final SelectTranslatorContext context;
    private final PathTranslator pathTranslator;
    private final Set<Object> expressionsToSkip;
    private final Deque<Node> nodeStack;

    private Node currentNode;

    static final String ERR_MSG_ARRAYS_NOT_SUPPORTED = "Arrays are not supported as a arguments in";

    QualifierTranslator(SelectTranslatorContext context) {
        this.context = context;
        this.pathTranslator = context.getPathTranslator();
        // must use identity comparison for the node skipping, or it could skip the wrong node
        // and mess everything up, see for example CAY-2871
        this.expressionsToSkip = Collections.newSetFromMap(new IdentityHashMap<>());
        this.nodeStack = new ArrayDeque<>();
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

        // expand complex expressions that could be only interpreted at the execution time
        qualifier = expandExpression(qualifier);

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
     * Preprocess complex expressions that ExpressionFactory can't handle at the creation time.
     * <br>
     * Right we only expand {@code EXIST} expressions that could spawn several subqueries.
     *
     * @param qualifier to process
     * @return qualifier with preprocessed complex expressions
     */
    Expression expandExpression(Expression qualifier) {
        return qualifier.transform(o -> {
            if (o instanceof ASTExists || o instanceof ASTNotExists) {
                return new ExistsExpressionTranslator(context, (SimpleNode) o).translate();
            }
            return o;
        });
    }

    @Override
    public void startNode(Expression node, Expression parentNode) {
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
        switch (node.getType()) {
            case NOT_IN:
                return new InNode(true);
            case IN:
                return new InNode(false);
            case NOT_BETWEEN:
            case BETWEEN:
                return new BetweenNode(node.getType() == NOT_BETWEEN);
            case NOT:
                return new NotNode();
            case BITWISE_NOT:
                return new BitwiseNotNode();
            case EQUAL_TO:
                return new EqualNode();
            case NOT_EQUAL_TO:
                return new NotEqualNode();

            case LIKE:
            case NOT_LIKE:
            case LIKE_IGNORE_CASE:
            case NOT_LIKE_IGNORE_CASE:
                PatternMatchNode patternMatchNode = (PatternMatchNode) node;
                boolean not = node.getType() == NOT_LIKE || node.getType() == NOT_LIKE_IGNORE_CASE;
                return new LikeNode(patternMatchNode.isIgnoringCase(), not, patternMatchNode.getEscapeChar());

            case OBJ_PATH:
                CayennePath path = (CayennePath) node.getOperand(0);
                PathTranslationResult result = pathTranslator.translatePath(context.getMetadata().getObjEntity(), path);
                return processPathTranslationResult(node, parentNode, result);

            case DB_PATH:
                CayennePath dbPath = (CayennePath) node.getOperand(0);
                PathTranslationResult dbResult = pathTranslator.translatePath(context.getMetadata().getDbEntity(), dbPath);
                return processPathTranslationResult(node, parentNode, dbResult);

            case DBID_PATH:
                CayennePath dbIdPath = (CayennePath) node.getOperand(0);
                PathTranslationResult dbIdResult = pathTranslator.translateIdPath(context.getMetadata().getObjEntity(), dbIdPath);
                return processPathTranslationResult(node, parentNode, dbIdResult);

            case FUNCTION_CALL:
                ASTFunctionCall functionCall = (ASTFunctionCall) node;
                return function(functionCall.getFunctionName()).build();

            case ADD:
            case SUBTRACT:
            case MULTIPLY:
            case DIVIDE:
            case BITWISE_AND:
            case BITWISE_LEFT_SHIFT:
            case BITWISE_OR:
            case BITWISE_RIGHT_SHIFT:
            case BITWISE_XOR:
            case OR:
            case AND:
            case LESS_THAN:
            case LESS_THAN_EQUAL_TO:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL_TO:
                return new OpExpressionNode(expToStr(node.getType()));
            case NEGATIVE:
                // If operand is a scalar null, produce NULL directly (no unary minus)
                if (node.getOperandCount() > 0 && node.getOperand(0) == null) {
                    expressionsToSkip.add(node);
                    return new ValueNode(null, false, null, false);
                }
                // we need to add minus sign as a prefix, not a separator
                return new FunctionNode(expToStr(node.getType()), null, false);
            case TRUE:
            case FALSE:
            case ASTERISK:
                return new TextNode(' ' + expToStr(node.getType()));

            case CUSTOM_OP:
                return new OpExpressionNode(((ASTCustomOperator) node).getOperator());

            case EXISTS:
                return new FunctionNode("EXISTS", null, false);
            case NOT_EXISTS:
                return new FunctionNode("NOT EXISTS", null, false);
            case ALL:
                return new FunctionNode("ALL", null, false);
            case ANY:
                return new FunctionNode("ANY", null, false);

            case SUBQUERY:
                ASTSubquery subquery = (ASTSubquery) node;
                SelectTranslatorContext subContext = new SelectTranslatorContext(
                        subquery.getQuery(), context.getAdapter(), context.getResolver(), context);
                // skip SQL translation stage for nested translators, it should be performed by root context only
                subContext.setSkipSQLGeneration(true);
                subContext.translate();
                return subContext.getSelectBuilder().build();

            case ENCLOSING_OBJECT:
                // Translate via parent context's translator
                Expression expression = (Expression) node.getOperand(0);
                if (context.getParentContext() == null) {
                    throw new CayenneRuntimeException("Unable to translate qualifier, no parent context to use for expression " + node);
                }
                expressionsToSkip.add(expression);
                return context.getParentContext().getQualifierTranslator().translate(expression);

            case FULL_OBJECT:
                ASTFullObject fullObject = (ASTFullObject) node;
                if (fullObject.getOperandCount() == 0) {
                    Collection<DbAttribute> dbAttributes = context.getMetadata().getDbEntity().getPrimaryKeys();
                    if (dbAttributes.size() > 1) {
                        throw new CayenneRuntimeException("Unable to translate reference on entity with more than one PK.");
                    }
                    DbAttribute attribute = dbAttributes.iterator().next();
                    String alias = context.getTableTree().aliasForPath(CayennePath.EMPTY_PATH);
                    return table(alias).column(attribute).build();
                } else {
                    return null;
                }
            case SCALAR:
                if (parentNode != null) {
                    throw new CayenneRuntimeException("Incorrect state, a node %s can't have parent here", node.getClass().getName());
                }
                Object scalarVal = ((ASTScalar) node).getValue();
                if (scalarVal instanceof Collection || scalarVal.getClass().isArray()) {
                    throw new CayenneRuntimeException("%s %s", ERR_MSG_ARRAYS_NOT_SUPPORTED, node.getClass().getName());
                } else {
                    objectNode(scalarVal, null);
                }
                return null;

            case CASE_WHEN:
                return new CaseNode();
            case WHEN:
                return new WhenNode();
            case THEN:
                return new ThenNode();
            case ELSE:
                return new ElseNode();
        }
        return null;
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
            } else if (operand instanceof ASTObjPath) {
                // TODO: support comparison of multi attribute ObjPath with other multi attribute ObjPath
                throw new UnsupportedOperationException("Comparison of multiple attributes not supported for ObjPath");
            }
        }

        throw new CayenneRuntimeException("Multi attribute ObjPath isn't matched with valid value. " +
                "List or Persistent object required.");
    }

    private Node buildMultiValueComparison(PathTranslationResult result, Map<String, Object> valueSnapshot) {
        ExpressionNodeBuilder expressionNodeBuilder = null;
        ExpressionNodeBuilder eq;

        CayennePath path = result.getLastAttributePath();
        String alias = context.getTableTree().aliasForPath(path);

        for (DbAttribute attribute : result.getDbAttributes()) {
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
        // must be in sync with expressionNodeToSqlNode() method
        return switch (node.getType()) {
            case NOT_IN, IN, NOT_BETWEEN, BETWEEN, NOT, BITWISE_NOT, EQUAL_TO, NOT_EQUAL_TO, LIKE, NOT_LIKE,
                 LIKE_IGNORE_CASE, NOT_LIKE_IGNORE_CASE, OBJ_PATH, DBID_PATH, DB_PATH, FUNCTION_CALL, ADD, SUBTRACT,
                 MULTIPLY, DIVIDE, NEGATIVE, CUSTOM_OP, BITWISE_AND, BITWISE_LEFT_SHIFT, BITWISE_OR,
                 BITWISE_RIGHT_SHIFT, BITWISE_XOR, OR, AND, LESS_THAN, LESS_THAN_EQUAL_TO, GREATER_THAN,
                 GREATER_THAN_EQUAL_TO, TRUE, FALSE, ASTERISK, EXISTS, NOT_EXISTS, SUBQUERY, ENCLOSING_OBJECT,
                 FULL_OBJECT, SCALAR, CASE_WHEN, WHEN, THEN, ELSE -> true;
            default -> false;
        };
    }

    @Override
    public void endNode(Expression node, Expression parentNode) {
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
                (parentNode.getType() == OBJ_PATH
                        || parentNode.getType() == DB_PATH
                        || parentNode.getType() == DBID_PATH)) {
            return;
        }

        ValueNodeBuilder valueNodeBuilder = value(leaf)
                .needBinding(needBinding(parentNode))
                .attribute(findDbAttribute(parentNode));
        if (parentNode != null && parentNode.getType() == Expression.LIST) {
            valueNodeBuilder.array(true);
        }
        Node nextNode = valueNodeBuilder.build();

        currentNode.addChild(nextNode);
        nextNode.setParent(currentNode);
    }

    private boolean needBinding(Expression parentNode) {
        return (parentNode != null);
    }

    protected DbAttribute findDbAttribute(Expression node) {
        if (node == null) {
            return null;
        }
        if (node.getType() == Expression.LIST) {
            if (node instanceof SimpleNode) {
                Expression parent = (Expression) ((SimpleNode) node).jjtGetParent();
                if (parent != null) {
                    node = parent;
                } else {
                    return null;
                }
            }
        } else if (node.getType() == FUNCTION_CALL) {
            return null;
        }

        PathTranslationResult result = null;
        for (int i = 0; i < node.getOperandCount(); i++) {
            Object op = node.getOperand(i);
            if (op instanceof ASTObjPath) {
                result = pathTranslator.translatePath(context.getMetadata().getObjEntity(), ((ASTObjPath) op).getPath());
                break;
            } else if (op instanceof ASTDbIdPath) {
                result = pathTranslator.translateIdPath(context.getMetadata().getObjEntity(), ((ASTDbIdPath) op).getPath());
                break;
            } else if (op instanceof ASTDbPath) {
                result = pathTranslator.translatePath(context.getMetadata().getDbEntity(), ((ASTDbPath) op).getPath());
                break;
            }
        }

        if (result == null) {
            return null;
        }

        return result.getLastAttribute();
    }

    private String expToStr(int type) {
        return switch (type) {
            case AND -> "AND";
            case OR -> "OR";
            case LESS_THAN -> "<";
            case LESS_THAN_EQUAL_TO -> "<=";
            case GREATER_THAN -> ">";
            case GREATER_THAN_EQUAL_TO -> ">=";
            case ADD -> "+";
            case NEGATIVE, SUBTRACT -> "-";
            case MULTIPLY, ASTERISK -> "*";
            case DIVIDE -> "/";
            case BITWISE_AND -> "&";
            case BITWISE_OR -> "|";
            case BITWISE_XOR -> "^";
            case BITWISE_NOT -> "!";
            case BITWISE_LEFT_SHIFT -> "<<";
            case BITWISE_RIGHT_SHIFT -> ">>";
            case TRUE -> "1=1";
            case FALSE -> "1=0";
            default -> "{other}";
        };
    }
}
