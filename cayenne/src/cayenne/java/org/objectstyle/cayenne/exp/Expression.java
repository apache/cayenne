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
package org.objectstyle.cayenne.exp;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Transformer;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.exp.parser.ExpressionParser;
import org.objectstyle.cayenne.exp.parser.ParseException;
import org.objectstyle.cayenne.util.ConversionUtil;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.XMLEncoder;
import org.objectstyle.cayenne.util.XMLSerializable;

/** 
 * Superclass of Cayenne expressions that defines basic
 * API for expressions use.
 */
public abstract class Expression implements Serializable, XMLSerializable {
    private final static Logger logObj = Logger.getLogger(Expression.class);
    
    private final static Object nullValue = new Object();

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
    public static final int EXISTS = 15;
    public static final int ADD = 16;
    public static final int SUBTRACT = 17;
    public static final int MULTIPLY = 18;
    public static final int DIVIDE = 19;
    public static final int NEGATIVE = 20;
    public static final int POSITIVE = 21;

    /**
     * Currently not supported in Cayenne.
     */
    public static final int ALL = 22;

    /**
     * Currently not supported in Cayenne.
     */
    public static final int SOME = 23;

    /**
     * Currently not supported in Cayenne.
     */
    public static final int ANY = 24;

    /** 
     * Expression interpreted as raw SQL. 
     * No translations will be done for this kind of expressions. 
     */
    public static final int RAW_SQL = 25;

    /** 
     * Expression describes a path relative to an ObjEntity.
     * OBJ_PATH expression is resolved relative to some root ObjEntity. Path expression components
     * are separated by "." (dot). Path can point to either one of these:
     * <ul>
     *    <li><i>An attribute of root ObjEntity.</i>
     *    For entity Gallery OBJ_PATH expression "galleryName" will point to ObjAttribute "galleryName" 
     *    <li><i>Another ObjEntity related to root ObjEntity via a chain of relationships.</i>
     *    For entity Gallery OBJ_PATH expression "paintingArray.toArtist" will point to ObjEntity "Artist" 
     *    <li><i>ObjAttribute of another ObjEntity related to root ObjEntity via a chain of relationships.</i>
     *    For entity Gallery OBJ_PATH expression "paintingArray.toArtist.artistName" will point to ObjAttribute "artistName" 
     * </ul>
     */
    public static final int OBJ_PATH = 26;

    /** 
     * Expression describes a path relative to a DbEntity.
     * DB_PATH expression is resolved relative to some root DbEntity. 
     * Path expression components are separated by "." (dot). Path can 
     * point to either one of these:
     * <ul>
     *    <li><i>An attribute of root DbEntity.</i>
     *    For entity GALLERY, DB_PATH expression "GALLERY_NAME" will point 
     *    to a DbAttribute "GALLERY_NAME".
     * 	  </li>
     * 
     *    <li><i>Another DbEntity related to root DbEntity via a chain of relationships.</i>
     *    For entity GALLERY DB_PATH expression "paintingArray.toArtist" will point to 
     *    DbEntity "ARTIST".
     *    </li>
     * 
     *    <li><i>DbAttribute of another ObjEntity related to root DbEntity via a chain 
     *    of relationships.</i>
     *    For entity GALLERY DB_PATH expression "paintingArray.toArtist.ARTIST_NAME" will point 
     *    to DbAttribute "ARTIST_NAME".
     *    </li>
     * </ul>
     */
    public static final int DB_PATH = 27;

    /** 
     * Interpreted as a comma-separated list of literals. 
     */
    public static final int LIST = 28;

    /**
     * Currently not supported in Cayenne.
     */
    public static final int SUBQUERY = 29;

    /**
     * Currently not supported in Cayenne.
     */
    public static final int COUNT = 30;
    
    /**
     * Currently not supported in Cayenne.
     */
    public static final int AVG = 31;

    /**
     * Currently not supported in Cayenne.
     */
    public static final int SUM = 32;

    /**
     * Currently not supported in Cayenne.
     */
    public static final int MAX = 33;

    /**
     * Currently not supported in Cayenne.
     */
    public static final int MIN = 34;

    public static final int NOT_BETWEEN = 35;
    public static final int NOT_IN = 36;
    public static final int NOT_LIKE = 37;
    public static final int NOT_LIKE_IGNORE_CASE = 38;

    protected int type;

    /**
     * Parses string, converting it to Expression. If string does
     * not represent a semantically correct expression, an ExpressionException
     * is thrown.
     * 
     * @since 1.1
     */
    // TODO: cache expression strings, since this operation is pretty slow
    public static Expression fromString(String expressionString) {
        if (expressionString == null) {
            throw new NullPointerException("Null expression string.");
        }

        Reader reader = new StringReader(expressionString);
        try {
            return new ExpressionParser(reader).expression();
        }
        catch (ParseException ex) {
            throw new ExpressionException(ex.getMessage(), ex);
        }
        catch (Throwable th) {
            // another common error is TokenManagerError
            throw new ExpressionException(th.getMessage(), th);
        }
    }

    /**
     * Returns String label for this expression. Used for debugging.
     */
    public String expName() {
        switch (type) {
            case AND :
                return "AND";
            case OR :
                return "OR";
            case NOT :
                return "NOT";
            case EQUAL_TO :
                return "=";
            case NOT_EQUAL_TO :
                return "<>";
            case LESS_THAN :
                return "<";
            case LESS_THAN_EQUAL_TO :
                return "<=";
            case GREATER_THAN :
                return ">";
            case GREATER_THAN_EQUAL_TO :
                return ">=";
            case BETWEEN :
                return "BETWEEN";
            case IN :
                return "IN";
            case LIKE :
                return "LIKE";
            case LIKE_IGNORE_CASE :
                return "LIKE_IGNORE_CASE";
            case EXISTS :
                return "EXISTS";
            case OBJ_PATH :
                return "OBJ_PATH";
            case DB_PATH :
                return "DB_PATH";
            case LIST :
                return "LIST";
            case NOT_BETWEEN :
                return "NOT BETWEEN";
            case NOT_IN :
                return "NOT IN";
            case NOT_LIKE :
                return "NOT LIKE";
            case NOT_LIKE_IGNORE_CASE :
                return "NOT LIKE IGNORE CASE";
            default :
                return "other";
        }
    }

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
     * Returns a type of expression. Most common types are defined 
     * as public static fields of this interface.
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
    public Expression expWithParameters(Map parameters) {
        return expWithParameters(parameters, true);
    }

    /**
     * Creates and returns a new Expression instance using this expression as a
     * prototype. All ExpressionParam operands are substituted with the values
     * in the <code>params</code> map. 
     * 
     * <p><i>Null values in the <code>params</code> map should be
     * explicitly created in the map for the corresponding key.
     * </i></p>
     * 
     * @param parameters a map of parameters, with each key being a string name of
     * an expression parameter, and value being the value that should be used in
     * the final expression.
     * @param pruneMissing If <code>true</code>, subexpressions that rely
     * on missing parameters will be pruned from the resulting tree. If
     * <code>false</code>, any missing values will generate an exception.
     * 
     * @return Expression resulting from the substitution of parameters with
     * real values, or null if the whole expression was pruned, due to the
     * missing parameters.
     */
    public Expression expWithParameters(
        final Map parameters,
        final boolean pruneMissing) {

        // create transformer for named parameters
        Transformer transformer = new Transformer() {
            public Object transform(Object object) {
                if (!(object instanceof ExpressionParameter)) {
                    return object;
                }

                String name = ((ExpressionParameter) object).getName();
                if (!parameters.containsKey(name)) {
                    if (pruneMissing) {
                        return null;
                    }
                    else {
                        throw new ExpressionException(
                            "Missing required parameter: $" + name);
                    }
                }
                else {
                    Object value = parameters.get(name);

                    // wrap lists (for now); also support null parameters
                    return (value != null)
                            ? ExpressionFactory.wrapPathOperand(value)
                            : nullValue;
                }
            }
        };

        return transform(transformer);
    }

    /** 
     * Creates a new expression that joins this object
     * with another expression, using specified join type.
     * It is very useful for incrementally building chained expressions,
     * like long AND or OR statements. 
     */
    public Expression joinExp(int type, Expression exp) {
        Expression join = ExpressionFactory.expressionOfType(type);
        join.setOperand(0, this);
        join.setOperand(1, exp);
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
     * Chains this expression with another expression using "or".
     */
    public Expression orExp(Expression exp) {
        return joinExp(Expression.OR, exp);
    }

    /**
     * Returns a logical NOT of current expression.
     * 
     * @since 1.0.6
     */
    public Expression notExp() {
        Expression exp = ExpressionFactory.expressionOfType(Expression.NOT);
        exp.setOperand(0, this);
        return exp;
    }

    /** 
     * Returns a count of operands of this expression. In real life there are
     * unary (count == 1), binary (count == 2) and ternary (count == 3) 
     * expressions.
     */
    public abstract int getOperandCount();

    /** 
     * Returns a value of operand at <code>index</code>. 
     * Operand indexing starts at 0. 
     */
    public abstract Object getOperand(int index);

    /** 
     * Sets a value of operand at <code>index</code>. 
     * Operand indexing starts at 0.
     */
    public abstract void setOperand(int index, Object value);

    /** 
     * Method for in-memory evaluation of expressions. 
     * 
     * @deprecated Since 1.1 use {@link #evaluate(Object)} or {@link #match(Object)}.
     */
    public boolean eval(Object o) {
        return match(o);
    }

    /**
     * Calculates expression value with object as a context for 
     * path expressions.
     * 
     * @since 1.1
     */
    public Object evaluate(Object o) {
        return ASTCompiler.compile(this).evaluateASTChain(o);
    }

    /**
     * Calculates expression boolean value with object as a context for 
     * path expressions.
     * 
     * @since 1.1
     */
    public boolean match(Object o) {
        return ConversionUtil.toBoolean(evaluate(o));
    }

    /**
     * Returns a list of objects that match the expression.
     */
    public List filterObjects(List objects) {
        if (objects == null || objects.size() == 0) {
            return Collections.EMPTY_LIST;
        }
        
        return (List) filter(objects, new LinkedList());
    }
    
    /**
     * Adds objects matching this expression from the source collection 
     * to the target collection.
     * 
     * @since 1.1
     */
    public Collection filter(Collection source, Collection target) {
        Iterator it = source.iterator();
        while (it.hasNext()) {
            Object o = it.next();
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
     * Returns true if this node should be pruned from expression tree
     * in the event a child is removed.
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
     * methods as it goes. This is an Expression-specific implementation of
     * the "Visitor" design pattern.
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
            }
            else {
                visitor.objectNode(child, this);
            }

            visitor.finishedChild(this, i, i < count - 1);
        }

        visitor.endNode(this, parentExp);
    }

    /**
     * Creates a transformed copy of this expression, applying 
     * transformation provided by Transformer to all its nodes.
     * Null transformer will result in an identical deep copy of
     * this expression.
     * 
     * <p>To force a node and its children to be pruned from the 
     * copy, Transformer should return null for a given node.
     * 
     * <p>There is one limitation on what Transformer is expected to do: 
     * if a node is an Expression it must be transformed to null
     * or another Expression. Any other object type would result in an 
     * exception. 
     * 
     *
     * @since 1.1
     */
    public Expression transform(Transformer transformer) {

        Expression copy = shallowCopy();
        int count = getOperandCount();
        for (int i = 0, j = 0; i < count; i++) {
            Object operand = getOperand(i);
            Object transformedChild = operand;

            if (operand instanceof Expression) {
                transformedChild = ((Expression) operand).transform(transformer);
            }
            else if (transformer != null) {
                transformedChild = transformer.transform(operand);
            }

            if (transformedChild != null) {
                Object value = (transformedChild != nullValue) ? transformedChild : null;
                copy.setOperand(j, value);
                j++;
            }
            else if (pruneNodeForPrunedChild(operand)) {
                // bail out early...
                return null;
            }
        }

        // all the children are processed, only now transform this copy 
        return (transformer != null) ? (Expression) transformer.transform(copy) : copy;
    }

    /**
     * Encodes itself, wrapping the string into XML CDATA section.
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<![CDATA[");
        encodeAsString(encoder.getPrintWriter());
        encoder.print("]]>");
    }

    /**
     * Stores a String representation of Expression using a provided
     * PrintWriter.
     * 
     * @since 1.1
     */
    public abstract void encodeAsString(PrintWriter pw);

    /**
     * Convenience method to log nested expressions. Used mainly for debugging.
     * Called from "toString".
     * 
     * @deprecated Since 1.1 <code>encode</code> is used to recursively
     * print expressions.
     */
    protected void toStringBuffer(StringBuffer buf) {
        for (int i = 0; i < getOperandCount(); i++) {
            if (i > 0 || getOperandCount() == 1) {
                buf.append(" ").append(expName()).append(" ");
            }

            Object op = getOperand(i);
            if (op == null) {
                buf.append("<null>");
            }
            else if (op instanceof String) {
                buf.append("'").append(op).append("'");
            }
            else if (op instanceof Expression) {
                buf.append('(');
                ((Expression) op).toStringBuffer(buf);
                buf.append(')');
            }
            else {
                buf.append(String.valueOf(op));
            }
        }

    }

    public String toString() {
        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        encodeAsString(pw);
        pw.close();
        buffer.flush();
        return buffer.toString();
    }
}
