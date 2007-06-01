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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.exp.parser.ASTAdd;
import org.objectstyle.cayenne.exp.parser.ASTAnd;
import org.objectstyle.cayenne.exp.parser.ASTBetween;
import org.objectstyle.cayenne.exp.parser.ASTDbPath;
import org.objectstyle.cayenne.exp.parser.ASTDivide;
import org.objectstyle.cayenne.exp.parser.ASTEqual;
import org.objectstyle.cayenne.exp.parser.ASTGreater;
import org.objectstyle.cayenne.exp.parser.ASTGreaterOrEqual;
import org.objectstyle.cayenne.exp.parser.ASTIn;
import org.objectstyle.cayenne.exp.parser.ASTLess;
import org.objectstyle.cayenne.exp.parser.ASTLessOrEqual;
import org.objectstyle.cayenne.exp.parser.ASTLike;
import org.objectstyle.cayenne.exp.parser.ASTLikeIgnoreCase;
import org.objectstyle.cayenne.exp.parser.ASTList;
import org.objectstyle.cayenne.exp.parser.ASTMultiply;
import org.objectstyle.cayenne.exp.parser.ASTNegate;
import org.objectstyle.cayenne.exp.parser.ASTNot;
import org.objectstyle.cayenne.exp.parser.ASTNotBetween;
import org.objectstyle.cayenne.exp.parser.ASTNotEqual;
import org.objectstyle.cayenne.exp.parser.ASTNotIn;
import org.objectstyle.cayenne.exp.parser.ASTNotLike;
import org.objectstyle.cayenne.exp.parser.ASTNotLikeIgnoreCase;
import org.objectstyle.cayenne.exp.parser.ASTObjPath;
import org.objectstyle.cayenne.exp.parser.ASTOr;
import org.objectstyle.cayenne.exp.parser.ASTSubtract;
import org.objectstyle.cayenne.exp.parser.SimpleNode;

/** 
 * Helper class to build expressions. 
 * 
 * @author Andrei Adamchik
 */
public class ExpressionFactory {
    private static Logger logObj = Logger.getLogger(ExpressionFactory.class);

    private static Class[] typeLookup;

    static {
        // make sure all types are small integers, then we can use
        // them as indexes in lookup array
        int[] allTypes =
            new int[] {
                Expression.AND,
                Expression.OR,
                Expression.NOT,
                Expression.EQUAL_TO,
                Expression.NOT_EQUAL_TO,
                Expression.LESS_THAN,
                Expression.GREATER_THAN,
                Expression.LESS_THAN_EQUAL_TO,
                Expression.GREATER_THAN_EQUAL_TO,
                Expression.BETWEEN,
                Expression.IN,
                Expression.LIKE,
                Expression.LIKE_IGNORE_CASE,
                Expression.EXISTS,
                Expression.ADD,
                Expression.SUBTRACT,
                Expression.MULTIPLY,
                Expression.DIVIDE,
                Expression.NEGATIVE,
                Expression.POSITIVE,
                Expression.ALL,
                Expression.SOME,
                Expression.ANY,
                Expression.RAW_SQL,
                Expression.OBJ_PATH,
                Expression.DB_PATH,
                Expression.LIST,
                Expression.SUBQUERY,
                Expression.COUNT,
                Expression.SUM,
                Expression.AVG,
                Expression.MIN,
                Expression.MAX,
                Expression.NOT_BETWEEN,
                Expression.NOT_IN,
                Expression.NOT_LIKE,
                Expression.NOT_LIKE_IGNORE_CASE };

        int max = 0;
        int min = 0;
        int allLen = allTypes.length;
        for (int i = 0; i < allLen; i++) {
            if (allTypes[i] > max)
                max = allTypes[i];
            else if (allTypes[i] < min)
                min = allTypes[i];
        }

        // sanity check....
        if (max > 500)
            throw new RuntimeException("Types values are too big: " + max);
        if (min < 0)
            throw new RuntimeException("Types values are too small: " + min);

        // now we know that if types are used as indexes, 
        // they will fit in array "max + 1" long (though gaps are possible)

        typeLookup = new Class[max + 1];

        typeLookup[Expression.AND] = ASTAnd.class;
        typeLookup[Expression.OR] = ASTOr.class;
        typeLookup[Expression.BETWEEN] = ASTBetween.class;
        typeLookup[Expression.NOT_BETWEEN] = ASTNotBetween.class;

        // binary types
        typeLookup[Expression.EQUAL_TO] = ASTEqual.class;
        typeLookup[Expression.NOT_EQUAL_TO] = ASTNotEqual.class;
        typeLookup[Expression.LESS_THAN] = ASTLess.class;
        typeLookup[Expression.GREATER_THAN] = ASTGreater.class;
        typeLookup[Expression.LESS_THAN_EQUAL_TO] = ASTLessOrEqual.class;
        typeLookup[Expression.GREATER_THAN_EQUAL_TO] = ASTGreaterOrEqual.class;
        typeLookup[Expression.IN] = ASTIn.class;
        typeLookup[Expression.NOT_IN] = ASTNotIn.class;
        typeLookup[Expression.LIKE] = ASTLike.class;
        typeLookup[Expression.LIKE_IGNORE_CASE] = ASTLikeIgnoreCase.class;
        typeLookup[Expression.NOT_LIKE] = ASTNotLike.class;
        typeLookup[Expression.NOT_LIKE_IGNORE_CASE] = ASTNotLikeIgnoreCase.class;
        typeLookup[Expression.ADD] = ASTAdd.class;
        typeLookup[Expression.SUBTRACT] = ASTSubtract.class;
        typeLookup[Expression.MULTIPLY] = ASTMultiply.class;
        typeLookup[Expression.DIVIDE] = ASTDivide.class;

        typeLookup[Expression.NOT] = ASTNot.class;
        typeLookup[Expression.NEGATIVE] = ASTNegate.class;
        typeLookup[Expression.OBJ_PATH] = ASTObjPath.class;
        typeLookup[Expression.DB_PATH] = ASTDbPath.class;
        typeLookup[Expression.LIST] = ASTList.class;

        // remainging "old" types
        typeLookup[Expression.EXISTS] = UnaryExpression.class;
        typeLookup[Expression.POSITIVE] = UnaryExpression.class;
        typeLookup[Expression.ALL] = UnaryExpression.class;
        typeLookup[Expression.SOME] = UnaryExpression.class;
        typeLookup[Expression.ANY] = UnaryExpression.class;
        typeLookup[Expression.RAW_SQL] = UnaryExpression.class;
        typeLookup[Expression.SUBQUERY] = UnaryExpression.class;
        typeLookup[Expression.SUM] = UnaryExpression.class;
        typeLookup[Expression.AVG] = UnaryExpression.class;
        typeLookup[Expression.COUNT] = UnaryExpression.class;
        typeLookup[Expression.MIN] = UnaryExpression.class;
        typeLookup[Expression.MAX] = UnaryExpression.class;
    }

    /** 
     * Creates a new expression for the type requested. 
     * If type is unknown, ExpressionException is thrown. 
     */
    public static Expression expressionOfType(int type) {
        if (type < 0 || type >= typeLookup.length) {
            throw new ExpressionException("Bad expression type: " + type);
        }

        if (typeLookup[type] == null) {
            throw new ExpressionException("Bad expression type: " + type);
        }

        // expected this
        if (SimpleNode.class.isAssignableFrom(typeLookup[type])) {
            try {
                return (Expression) typeLookup[type].newInstance();
            }
            catch (Exception ex) {
                throw new ExpressionException("Error creating expression", ex);
            }
        }

        // backwards compatibility:
        if (BinaryExpression.class == typeLookup[type]) {
            return new BinaryExpression(type);
        }

        if (UnaryExpression.class == typeLookup[type]) {
            return new UnaryExpression(type);
        }

        if (TernaryExpression.class == typeLookup[type]) {
            return new TernaryExpression(type);
        }

        if (ListExpression.class == typeLookup[type]) {
            return new ListExpression(type);
        }

        throw new ExpressionException("Bad expression type: " + type);
    }

    /**
     * Applies a few default rules for adding operands to
     * expressions. In particular wraps all lists into LIST
     * expressions. Applied only in path expressions.
     */
    protected static Object wrapPathOperand(Object op) {
        if (op instanceof Collection) {
            return new ASTList((Collection) op);
        }
        else if (op instanceof Object[]) {
            return new ASTList((Object[]) op);
        }
        else {
            return op;
        }
    }

    /** 
     * @deprecated Since 1.1 use {@link Expression#fromString(String)} or one of the more 
     * specific factory methods.
     */
    public static Expression unaryExp(int type, Object operand) {
        Expression exp = expressionOfType(type);
        exp.setOperand(0, operand);
        return exp;
    }

    /** 
     * @deprecated Since 1.1 use {@link Expression#fromString(String)} or one of the more 
     * specific factory methods.
     */
    public static Expression listExp(int type, List operands) {
        Expression exp = expressionOfType(type);
        for (int i = 0; i < operands.size(); i++) {
            exp.setOperand(i, operands.get(i));
        }

        return exp;
    }

    /** 
     * @deprecated Since 1.1 use {@link Expression#fromString(String)} or one of the more 
     * specific factory methods.
     */
    public static Expression binaryExp(
        int type,
        Object leftOperand,
        Object rightOperand) {
        Expression exp = expressionOfType(type);
        exp.setOperand(0, leftOperand);
        exp.setOperand(1, rightOperand);
        return exp;
    }

    /** 
     * @deprecated Since 1.1 use {@link Expression#fromString(String)} or one of the more 
     * specific factory methods.
     */
    public static Expression binaryPathExp(int type, String pathSpec, Object value) {
        return binaryExp(
            type,
            unaryExp(Expression.OBJ_PATH, pathSpec),
            wrapPathOperand(value));
    }

    /** 
     * @deprecated Since 1.1 use {@link Expression#fromString(String)} or one of the more 
     * specific factory methods.
     */
    public static Expression binaryDbPathExp(int type, String pathSpec, Object value) {
        return binaryExp(
            type,
            unaryExp(Expression.DB_PATH, pathSpec),
            wrapPathOperand(value));
    }

    /** 
     * @deprecated Since 1.1 use {@link Expression#fromString(String)} or one of the more 
     * specific factory methods.
     */
    public static Expression ternaryExp(
        int type,
        Object firstOperand,
        Object secondOperand,
        Object thirdOperand) {
        Expression exp = expressionOfType(type);
        exp.setOperand(0, firstOperand);
        exp.setOperand(1, secondOperand);
        exp.setOperand(2, thirdOperand);
        return exp;
    }

    /** 
     * Creates an expression that matches any of the key-values pairs in <code>map</code>.
     * 
     * <p>For each pair <code>pairType</code> operator is used to build a binary expression. 
     * Key is considered to be a DB_PATH expression. Therefore all keys must be java.lang.String
     * objects, or ClassCastException is thrown. OR is used to join pair binary expressions.
     */
    public static Expression matchAnyDbExp(Map map, int pairType) {
        List pairs = new ArrayList();

        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            pairs.add(
                binaryDbPathExp(pairType, (String) entry.getKey(), entry.getValue()));
        }

        return joinExp(Expression.OR, pairs);
    }

    /** 
     * Creates an expression that matches all key-values pairs in <code>map</code>.
     * 
     * <p>For each pair <code>pairType</code> operator is used to build a binary expression. 
     * Key is considered to be a DB_PATH expression. Therefore all keys must be java.lang.String
     * objects, or ClassCastException is thrown. AND is used to join pair binary expressions.
     */
    public static Expression matchAllDbExp(Map map, int pairType) {
        List pairs = new ArrayList();

        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            pairs.add(
                binaryDbPathExp(pairType, (String) entry.getKey(), entry.getValue()));
        }

        return joinExp(Expression.AND, pairs);
    }

    /** 
     * Creates an expression that matches any of the key-values pairs in the <code>map</code>.
     * 
     * <p>For each pair <code>pairType</code> operator is used to build a binary expression. 
     * Key is considered to be a OBJ_PATH expression. Therefore all keys must be java.lang.String
     * objects, or ClassCastException is thrown. OR is used to join pair binary expressions.
     */
    public static Expression matchAnyExp(Map map, int pairType) {
        List pairs = new ArrayList();

        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            pairs.add(binaryPathExp(pairType, (String) entry.getKey(), entry.getValue()));
        }

        return joinExp(Expression.OR, pairs);
    }

    /** 
     * Creates an expression that matches all key-values pairs in <code>map</code>.
     * 
     * <p>For each pair <code>pairType</code> operator is used to build a binary expression. 
     * Key is considered to be a OBJ_PATH expression. Therefore all keys must be java.lang.String
     * objects, or ClassCastException is thrown. AND is used to join pair binary expressions.
     */
    public static Expression matchAllExp(Map map, int pairType) {
        List pairs = new ArrayList();

        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            pairs.add(binaryPathExp(pairType, (String) entry.getKey(), entry.getValue()));
        }

        return joinExp(Expression.AND, pairs);
    }

    /**
     * A convenience method to create an DB_PATH "equal to" expression.
     */
    public static Expression matchDbExp(String pathSpec, Object value) {
        return new ASTEqual(new ASTDbPath(pathSpec), value);
    }

    /**
     * A convenience method to create an DB_PATH "not equal to" expression.
     */
    public static Expression noMatchDbExp(String pathSpec, Object value) {
        return new ASTNotEqual(new ASTDbPath(pathSpec), value);
    }
    
    /**
     * A convenience shortcut for building IN DB expression.
     */
    public static Expression inDbExp(String pathSpec, Object[] values) {
        return new ASTIn(new ASTDbPath(pathSpec), new ASTList(values));
    }

    /**
     * A convenience shortcut for building IN DB expression.
     */
    public static Expression inDbExp(String pathSpec, Collection values) {
        return new ASTIn(new ASTDbPath(pathSpec), new ASTList(values));
    }
    
    /**
     * A convenience method to create an OBJ_PATH "equal to" expression.
     */
    public static Expression matchExp(String pathSpec, Object value) {
        return new ASTEqual(new ASTObjPath(pathSpec), value);
    }

    /**
     * A convenience method to create an OBJ_PATH "not equal to" expression.
     */
    public static Expression noMatchExp(String pathSpec, Object value) {
        return new ASTNotEqual(new ASTObjPath(pathSpec), value);
    }

    /**
     * A convenience method to create an OBJ_PATH "less than" expression.
     */
    public static Expression lessExp(String pathSpec, Object value) {
        return new ASTLess(new ASTObjPath(pathSpec), value);
    }

    /**
     * A convenience method to create an OBJ_PATH "less than or equal to" expression.
     */
    public static Expression lessOrEqualExp(String pathSpec, Object value) {
        return new ASTLessOrEqual(new ASTObjPath(pathSpec), value);
    }

    /**
     * A convenience method to create an OBJ_PATH "greater than" expression.
     */
    public static Expression greaterExp(String pathSpec, Object value) {
        return new ASTGreater(new ASTObjPath(pathSpec), value);
    }

    /**
     * A convenience method to create an OBJ_PATH "greater than or equal to" expression.
     */
    public static Expression greaterOrEqualExp(String pathSpec, Object value) {
        return new ASTGreaterOrEqual(new ASTObjPath(pathSpec), value);
    }

    /**
     * A convenience shortcut for building IN expression.
     */
    public static Expression inExp(String pathSpec, Object[] values) {
        return new ASTIn(new ASTObjPath(pathSpec), new ASTList(values));
    }

    /**
     * A convenience shortcut for building IN expression.
     */
    public static Expression inExp(String pathSpec, Collection values) {
        return new ASTIn(new ASTObjPath(pathSpec), new ASTList(values));
    }

    /**
     * A convenience shortcut for building NOT_IN expression.
     */
    public static Expression notInExp(String pathSpec, Collection values) {
        return new ASTNotIn(new ASTObjPath(pathSpec), new ASTList(values));
    }

    /**
     * A convenience shortcut for building NOT_IN expression.
     * @since 1.0.6
     */
    public static Expression notInExp(String pathSpec, Object[] values) {
        return new ASTNotIn(new ASTObjPath(pathSpec), new ASTList(values));
    }

    /**
     * A convenience shortcut for building BETWEEN expressions.
     */
    public static Expression betweenExp(String pathSpec, Object value1, Object value2) {
        return new ASTBetween(new ASTObjPath(pathSpec), value1, value2);
    }

    /**
     * A convenience shortcut for building NOT_BETWEEN expressions.
     */
    public static Expression notBetweenExp(
        String pathSpec,
        Object value1,
        Object value2) {
        return new ASTNotBetween(new ASTObjPath(pathSpec), value1, value2);
    }

    /**
     * A convenience shortcut for building LIKE expression.
     */
    public static Expression likeExp(String pathSpec, Object value) {
        return new ASTLike(new ASTObjPath(pathSpec), value);
    }

    /**
     * A convenience shortcut for building NOT_LIKE expression.
     */
    public static Expression notLikeExp(String pathSpec, Object value) {
        return new ASTNotLike(new ASTObjPath(pathSpec), value);
    }

    /**
     * A convenience shortcut for building LIKE_IGNORE_CASE expression.
     */
    public static Expression likeIgnoreCaseExp(String pathSpec, Object value) {
        return new ASTLikeIgnoreCase(new ASTObjPath(pathSpec), value);
    }

    /**
     * A convenience shortcut for building NOT_LIKE_IGNORE_CASE expression.
     */
    public static Expression notLikeIgnoreCaseExp(String pathSpec, Object value) {
        return new ASTNotLikeIgnoreCase(new ASTObjPath(pathSpec), value);
    }

    /** 
     * Joins all <code>expressions</code> in a single expression. 
     * <code>type</code> is used as an expression type for expressions joining 
     * each one of the items on the list. <code>type</code> must be binary 
     * expression type.
     * 
     * <p>For example, if type is Expression.AND, resulting expression would match 
     * all expressions in the list. If type is Expression.OR, resulting expression 
     * would match any of the expressions. </p>
     */
    public static Expression joinExp(int type, List expressions) {
        int len = expressions.size();
        if (len == 0)
            return null;

        Expression currentExp = (Expression) expressions.get(0);
        if (len == 1) {
            return currentExp;
        }

        Expression exp = expressionOfType(type);
        for (int i = 0; i < len; i++) {
            exp.setOperand(i, expressions.get(i));
        }
        return exp;
    }
}