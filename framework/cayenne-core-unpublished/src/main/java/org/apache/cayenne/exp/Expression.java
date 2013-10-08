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

package org.apache.cayenne.exp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.parser.ASTScalar;
import org.apache.cayenne.exp.parser.ExpressionParser;
import org.apache.cayenne.exp.parser.ExpressionParserTokenManager;
import org.apache.cayenne.exp.parser.JavaCharStream;
import org.apache.cayenne.exp.parser.ParseException;
import org.apache.cayenne.util.ConversionUtil;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;
import org.apache.commons.collections.Transformer;

/**
 * Superclass of Cayenne expressions that defines basic API for expressions use.
 */
public abstract class Expression implements Serializable, XMLSerializable {

    /**
     * A value that a Transformer might return to indicate that a node has to be
     * pruned from the expression during the transformation.
     * 
     * @since 1.2
     */
    public final static Object PRUNED_NODE = new Object();

    public static final int AND = 0;
    public static final int OR = 1;
    public static final int NOT = 2;
    public static final int EQUAL_TO = 3;
    public static final int NOT_EQUAL_TO = 4;
    public static final int LESS_THAN = 5;
    public static final int GREATER_THAN = 6;
    public static final int LESS_THAN_EQUAL_TO = 7;
    public static final int GREATER_THAN_EQUAL_TO = 8;
    public static final int BETWEEN = 9;
    public static final int IN = 10;
    public static final int LIKE = 11;
    public static final int LIKE_IGNORE_CASE = 12;
    public static final int ADD = 16;
    public static final int SUBTRACT = 17;
    public static final int MULTIPLY = 18;
    public static final int DIVIDE = 19;
    public static final int NEGATIVE = 20;
    public static final int TRUE = 21;
    public static final int FALSE = 22;

    /**
     * Expression describes a path relative to an ObjEntity. OBJ_PATH expression
     * is resolved relative to some root ObjEntity. Path expression components
     * are separated by "." (dot). Path can point to either one of these:
     * <ul>
     * <li><i>An attribute of root ObjEntity.</i> For entity Gallery OBJ_PATH
     * expression "galleryName" will point to ObjAttribute "galleryName"
     * <li><i>Another ObjEntity related to root ObjEntity via a chain of
     * relationships.</i> For entity Gallery OBJ_PATH expression
     * "paintingArray.toArtist" will point to ObjEntity "Artist"
     * <li><i>ObjAttribute of another ObjEntity related to root ObjEntity via a
     * chain of relationships.</i> For entity Gallery OBJ_PATH expression
     * "paintingArray.toArtist.artistName" will point to ObjAttribute
     * "artistName"
     * </ul>
     */
    public static final int OBJ_PATH = 26;

    /**
     * Expression describes a path relative to a DbEntity. DB_PATH expression is
     * resolved relative to some root DbEntity. Path expression components are
     * separated by "." (dot). Path can point to either one of these:
     * <ul>
     * <li><i>An attribute of root DbEntity.</i> For entity GALLERY, DB_PATH
     * expression "GALLERY_NAME" will point to a DbAttribute "GALLERY_NAME".</li>
     * <li><i>Another DbEntity related to root DbEntity via a chain of
     * relationships.</i> For entity GALLERY DB_PATH expression
     * "paintingArray.toArtist" will point to DbEntity "ARTIST".</li>
     * <li><i>DbAttribute of another ObjEntity related to root DbEntity via a
     * chain of relationships.</i> For entity GALLERY DB_PATH expression
     * "paintingArray.toArtist.ARTIST_NAME" will point to DbAttribute
     * "ARTIST_NAME".</li>
     * </ul>
     */
    public static final int DB_PATH = 27;

    /**
     * Interpreted as a comma-separated list of literals.
     */
    public static final int LIST = 28;

    public static final int NOT_BETWEEN = 35;
    public static final int NOT_IN = 36;
    public static final int NOT_LIKE = 37;
    public static final int NOT_LIKE_IGNORE_CASE = 38;

    /**
     * @since 3.1
     */
    public static final int BITWISE_NOT = 39;

    /**
     * @since 3.1
     */
    public static final int BITWISE_AND = 40;

    /**
     * @since 3.1
     */
    public static final int BITWISE_OR = 41;

    /**
     * @since 3.1
     */
    public static final int BITWISE_XOR = 42;

    /**
     * @since 3.2
     */
    public static final int BITWISE_LEFT_SHIFT = 43;

    /**
     * @since 3.2
     */
    public static final int BITWISE_RIGHT_SHIFT = 44;

    private static final int PARSE_BUFFER_MAX_SIZE = 4096;

    protected int type;

    /**
     * Parses string, converting it to Expression. If string does not represent
     * a semantically correct expression, an ExpressionException is thrown.
     * 
     * @since 1.1
     */
    // TODO: cache expression strings, since this operation is pretty slow
    public static Expression fromString(String expressionString) {
        if (expressionString == null) {
            throw new NullPointerException("Null expression string.");
        }

        // optimizing parser buffers per CAY-1667...
        // adding 1 extra char to the buffer size above the String length, as
        // otherwise
        // resizing still occurs at the end of the stream
        int bufferSize = expressionString.length() > PARSE_BUFFER_MAX_SIZE ? PARSE_BUFFER_MAX_SIZE : expressionString
                .length() + 1;
        Reader reader = new StringReader(expressionString);
        JavaCharStream stream = new JavaCharStream(reader, 1, 1, bufferSize);
        ExpressionParserTokenManager tm = new ExpressionParserTokenManager(stream);
        ExpressionParser parser = new ExpressionParser(tm);

        try {
            return parser.expression();
        } catch (ParseException ex) {

            // can be null
            String message = ex.getMessage();
            throw new ExpressionException(message != null ? message : "", ex);
        } catch (Throwable th) {
            // can be null
            String message = th.getMessage();

            // another common error is TokenManagerError
            throw new ExpressionException(message != null ? message : "", th);
        }
    }

    /**
     * Returns a map of path aliases for this expression. It returns a non-empty
     * map only if this is a path expression and the aliases are known at the
     * expression creation time. Otherwise an empty map is returned.
     * 
     * @since 3.0
     */
    public abstract Map<String, String> getPathAliases();

    /**
     * Returns String label for this expression. Used for debugging.
     */
    public String expName() {
        switch (type) {
        case AND:
            return "AND";
        case OR:
            return "OR";
        case NOT:
            return "NOT";
        case EQUAL_TO:
            return "=";
        case NOT_EQUAL_TO:
            return "<>";
        case LESS_THAN:
            return "<";
        case LESS_THAN_EQUAL_TO:
            return "<=";
        case GREATER_THAN:
            return ">";
        case GREATER_THAN_EQUAL_TO:
            return ">=";
        case BETWEEN:
            return "BETWEEN";
        case IN:
            return "IN";
        case LIKE:
            return "LIKE";
        case LIKE_IGNORE_CASE:
            return "LIKE_IGNORE_CASE";
        case OBJ_PATH:
            return "OBJ_PATH";
        case DB_PATH:
            return "DB_PATH";
        case LIST:
            return "LIST";
        case NOT_BETWEEN:
            return "NOT BETWEEN";
        case NOT_IN:
            return "NOT IN";
        case NOT_LIKE:
            return "NOT LIKE";
        case NOT_LIKE_IGNORE_CASE:
            return "NOT LIKE IGNORE CASE";
        default:
            return "other";
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Expression)) {
            return false;
        }

        Expression e = (Expression) object;

        if (e.getType() != getType() || e.getOperandCount() != getOperandCount()) {
            return false;
        }

        // compare operands
        int len = e.getOperandCount();
        for (int i = 0; i < len; i++) {
            if (!Util.nullSafeEquals(e.getOperand(i), getOperand(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a type of expression. Most common types are defined as public
     * static fields of this interface.
     */
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    /**
     * A shortcut for <code>expWithParams(params, true)</code>.
     */
    public Expression expWithParameters(Map<String, ?> parameters) {
        return expWithParameters(parameters, true);
    }

    /**
     * Creates and returns a new Expression instance using this expression as a
     * prototype. All ExpressionParam operands are substituted with the values
     * in the <code>params</code> map.
     * <p>
     * <i>Null values in the <code>params</code> map should be explicitly
     * created in the map for the corresponding key. </i>
     * </p>
     * 
     * @param parameters
     *            a map of parameters, with each key being a string name of an
     *            expression parameter, and value being the value that should be
     *            used in the final expression.
     * @param pruneMissing
     *            If <code>true</code>, subexpressions that rely on missing
     *            parameters will be pruned from the resulting tree. If
     *            <code>false</code> , any missing values will generate an
     *            exception.
     * @return Expression resulting from the substitution of parameters with
     *         real values, or null if the whole expression was pruned, due to
     *         the missing parameters.
     */
    public Expression expWithParameters(final Map<String, ?> parameters, final boolean pruneMissing) {

        // create transformer for named parameters
        Transformer transformer = new Transformer() {

            public Object transform(Object object) {
                if (!(object instanceof ExpressionParameter)) {

                    // mainly for the ASTList array child...
                    if (object instanceof Object[]) {

                        Object[] source = (Object[]) object;
                        int len = source.length;
                        Object[] target = new Object[len];

                        for (int i = 0; i < len; i++) {
                            target[i] = transform(source[i]);
                        }
                        
                        return target;
                    }

                    return object;
                }

                String name = ((ExpressionParameter) object).getName();
                if (!parameters.containsKey(name)) {
                    if (pruneMissing) {
                        return PRUNED_NODE;
                    } else {
                        throw new ExpressionException("Missing required parameter: $" + name);
                    }
                } else {
                    Object value = parameters.get(name);

                    // wrap lists (for now); also support null parameters
                    // TODO: andrus 8/14/2007 - shouldn't we also wrap non-null
                    // object
                    // values in ASTScalars?
                    return (value != null) ? ExpressionFactory.wrapPathOperand(value) : new ASTScalar(null);
                }
            }
        };

        return transform(transformer);
    }

    /**
     * Creates a new expression that joins this object with another expression,
     * using specified join type. It is very useful for incrementally building
     * chained expressions, like long AND or OR statements.
     */
    public Expression joinExp(int type, Expression exp) {
        return joinExp(type, exp, new Expression[0]);
    }

    /**
     * Creates a new expression that joins this object with other expressions,
     * using specified join type. It is very useful for incrementally building
     * chained expressions, like long AND or OR statements.
     * 
     * @since 3.2
     */
    public Expression joinExp(int type, Expression exp, Expression... expressions) {
        Expression join = ExpressionFactory.expressionOfType(type);
        join.setOperand(0, this);
        join.setOperand(1, exp);
        for (int i = 0; i < expressions.length; i++) {
            Expression expressionInArray = expressions[i];
            join.setOperand(2 + i, expressionInArray);
        }
        join.flattenTree();
        return join;
    }

    /**
     * Chains this expression with another expression using "and".
     */
    public Expression andExp(Expression exp) {
        return joinExp(Expression.AND, exp);
    }

    /**
     * Chains this expression with other expressions using "and".
     * 
     * @since 3.2
     */
    public Expression andExp(Expression exp, Expression... expressions) {
        return joinExp(Expression.AND, exp, expressions);
    }

    /**
     * Chains this expression with another expression using "or".
     */
    public Expression orExp(Expression exp) {
        return joinExp(Expression.OR, exp);
    }

    /**
     * Chains this expression with other expressions using "or".
     * 
     * @since 3.2
     */
    public Expression orExp(Expression exp, Expression... expressions) {
        return joinExp(Expression.OR, exp, expressions);
    }

    /**
     * Returns a logical NOT of current expression.
     * 
     * @since 1.0.6
     */
    public abstract Expression notExp();

    /**
     * Returns a count of operands of this expression. In real life there are
     * unary (count == 1), binary (count == 2) and ternary (count == 3)
     * expressions.
     */
    public abstract int getOperandCount();

    /**
     * Returns a value of operand at <code>index</code>. Operand indexing starts
     * at 0.
     */
    public abstract Object getOperand(int index);

    /**
     * Sets a value of operand at <code>index</code>. Operand indexing starts at
     * 0.
     */
    public abstract void setOperand(int index, Object value);

    /**
     * Calculates expression value with object as a context for path
     * expressions.
     * 
     * @since 1.1
     */
    public abstract Object evaluate(Object o);

    /**
     * Calculates expression boolean value with object as a context for path
     * expressions.
     * 
     * @since 1.1
     */
    public boolean match(Object o) {
        return ConversionUtil.toBoolean(evaluate(o));
    }

    /**
     * Returns the first object in the list that matches the expression.
     * 
     * @since 3.1
     */
    public <T> T first(List<T> objects) {
        for (T o : objects) {
            if (match(o)) {
                return o;
            }
        }

        return null;
    }

    /**
     * Returns a list of objects that match the expression.
     */
    public <T> List<T> filterObjects(Collection<T> objects) {
        if (objects == null || objects.size() == 0) {
            return Collections.EMPTY_LIST;
        }

        return (List<T>) filter(objects, new LinkedList<T>());
    }

    /**
     * Adds objects matching this expression from the source collection to the
     * target collection.
     * 
     * @since 1.1
     */
    public <T> Collection<?> filter(Collection<T> source, Collection<T> target) {
        for (T o : source) {
            if (match(o)) {
                target.add(o);
            }
        }

        return target;
    }

    /**
     * Clones this expression.
     * 
     * @since 1.1
     */
    public Expression deepCopy() {
        return transform(null);
    }

    /**
     * Creates a copy of this expression node, without copying children.
     * 
     * @since 1.1
     */
    public abstract Expression shallowCopy();

    /**
     * Returns true if this node should be pruned from expression tree in the
     * event a child is removed.
     * 
     * @since 1.1
     */
    protected abstract boolean pruneNodeForPrunedChild(Object prunedChild);

    /**
     * Restructures expression to make sure that there are no children of the
     * same type as this expression.
     * 
     * @since 1.1
     */
    protected abstract void flattenTree();

    /**
     * Traverses itself and child expressions, notifying visitor via callback
     * methods as it goes. This is an Expression-specific implementation of the
     * "Visitor" design pattern.
     * 
     * @since 1.1
     */
    public void traverse(TraversalHandler visitor) {
        if (visitor == null) {
            throw new NullPointerException("Null Visitor.");
        }

        traverse(null, visitor);
    }

    /**
     * Traverses itself and child expressions, notifying visitor via callback
     * methods as it goes.
     * 
     * @since 1.1
     */
    protected void traverse(Expression parentExp, TraversalHandler visitor) {

        visitor.startNode(this, parentExp);

        // recursively traverse each child
        int count = getOperandCount();
        for (int i = 0; i < count; i++) {
            Object child = getOperand(i);

            if (child instanceof Expression) {
                Expression childExp = (Expression) child;
                childExp.traverse(this, visitor);
            } else {
                visitor.objectNode(child, this);
            }

            visitor.finishedChild(this, i, i < count - 1);
        }

        visitor.endNode(this, parentExp);
    }

    /**
     * Creates a transformed copy of this expression, applying transformation
     * provided by Transformer to all its nodes. Null transformer will result in
     * an identical deep copy of this expression.
     * <p>
     * To force a node and its children to be pruned from the copy, Transformer
     * should return Expression.PRUNED_NODE. Otherwise an expectation is that if
     * a node is an Expression it must be transformed to null or another
     * Expression. Any other object type would result in a ExpressionException.
     * 
     * @since 1.1
     */
    public Expression transform(Transformer transformer) {
        Object transformed = transformExpression(transformer);

        if (transformed == PRUNED_NODE || transformed == null) {
            return null;
        } else if (transformed instanceof Expression) {
            return (Expression) transformed;
        }

        throw new ExpressionException("Invalid transformed expression: " + transformed);
    }

    /**
     * A recursive method called from "transform" to do the actual
     * transformation.
     * 
     * @return null, Expression.PRUNED_NODE or transformed expression.
     * @since 1.2
     */
    protected Object transformExpression(Transformer transformer) {
        Expression copy = shallowCopy();
        int count = getOperandCount();
        for (int i = 0, j = 0; i < count; i++) {
            Object operand = getOperand(i);
            Object transformedChild;

            if (operand instanceof Expression) {
                transformedChild = ((Expression) operand).transformExpression(transformer);
            } else if (transformer != null) {
                transformedChild = transformer.transform(operand);
            } else {
                transformedChild = operand;
            }

            // prune null children only if there is a transformer and it
            // indicated so
            boolean prune = transformer != null && transformedChild == PRUNED_NODE;

            if (!prune) {
                copy.setOperand(j, transformedChild);
                j++;
            }

            if (prune && pruneNodeForPrunedChild(operand)) {
                // bail out early...
                return PRUNED_NODE;
            }
        }

        // all the children are processed, only now transform this copy
        return (transformer != null) ? (Expression) transformer.transform(copy) : copy;
    }

    /**
     * Encodes itself, wrapping the string into XML CDATA section.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<![CDATA[");
        try {
            appendAsString(encoder.getPrintWriter());
        } catch (IOException e) {
            throw new CayenneRuntimeException("Unexpected IO exception appending to PrintWriter", e);
        }
        encoder.print("]]>");
    }

    /**
     * Stores a String representation of Expression using a provided
     * PrintWriter.
     * 
     * @since 1.1
     * @deprecated since 3.2 use {@link #appendAsString(Appendable)}.
     */
    @Deprecated
    public abstract void encodeAsString(PrintWriter pw);

    /**
     * Appends own content as a String to the provided Appendable.
     * 
     * @since 3.2
     * @throws IOException
     */
    public abstract void appendAsString(Appendable out) throws IOException;

    /**
     * Stores a String representation of Expression as EJBQL using a provided
     * PrintWriter. DB path expressions produce non-standard EJBQL path
     * expressions.
     * 
     * @since 3.0
     * @deprecated since 3.2 use {@link #appendAsEJBQL(Appendable, String)}
     */
    @Deprecated
    public abstract void encodeAsEJBQL(PrintWriter pw, String rootId);

    /**
     * Stores a String representation of Expression as EJBQL using a provided
     * Appendable. DB path expressions produce non-standard EJBQL path
     * expressions.
     * 
     * @since 3.2
     * @throws IOException
     */
    public abstract void appendAsEJBQL(Appendable out, String rootId) throws IOException;

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        try {
            appendAsString(out);
        } catch (IOException e) {
            throw new CayenneRuntimeException("Unexpected IO exception appending to StringBuilder", e);
        }
        return out.toString();
    }

    /**
     * @since 3.0
     */
    public String toEJBQL(String rootId) {
        StringBuilder out = new StringBuilder();
        try {
            appendAsEJBQL(out, rootId);
        } catch (IOException e) {
            throw new CayenneRuntimeException("Unexpected IO exception appending to StringBuilder", e);
        }
        return out.toString();
    }
}
