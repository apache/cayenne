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

package org.apache.cayenne.exp;


/**
 * Collection of factory methods to create function call expressions.
 *
 * @since 4.0
 */
public class FunctionExpressionFactory {

    /**
     * Call SUBSTRING(string, offset, length) function
     *
     * @param exp    expression that must evaluate to string
     * @param offset start offset of substring
     * @param length length of substring
     * @return SUBSTRING() call expression
     */
    public static Expression substringExp(Expression exp, int offset, int length) {
        return substringExp(exp, ExpressionFactory.wrapScalarValue(offset), ExpressionFactory.wrapScalarValue(length));
    }

    /**
     * Call SUBSTRING(string, offset, length) function
     *
     * @param path   Object path value
     * @param offset start offset of substring
     * @param length length of substring
     * @return SUBSTRING() call expression
     */
    public static Expression substringExp(String path, int offset, int length) {
        return substringExp(ExpressionFactory.pathExp(path), ExpressionFactory.wrapScalarValue(offset), ExpressionFactory.wrapScalarValue(length));
    }

    /**
     * Call SUBSTRING(string, offset, length) function
     *
     * @param exp    expression that must evaluate to string
     * @param offset start offset of substring must evaluate to int
     * @param length length of substring must evaluate to int
     * @return SUBSTRING() call expression
     */
    public static Expression substringExp(Expression exp, Expression offset, Expression length) {
        return new SubstringExp(exp, offset, length);
    }

    /**
     * @param exp string expression to trim
     * @return TRIM() call expression
     */
    public static Expression trimExp(Expression exp) {
        return new TrimExp(exp);
    }

    /**
     * @param path object path value
     * @return TRIM() call expression
     */
    public static Expression trimExp(String path) {
        return new TrimExp(ExpressionFactory.pathExp(path));
    }

    /**
     * @param exp string expression
     * @return LOWER() call expression
     */
    public static Expression lowerExp(Expression exp) {
        return new LowerExp(exp);
    }

    /**
     * @param path object path value
     * @return LOWER() call expression
     */
    public static Expression lowerExp(String path) {
        return new LowerExp(ExpressionFactory.pathExp(path));
    }

    /**
     * @param exp string expression
     * @return UPPER() call expression
     */
    public static Expression upperExp(Expression exp) {
        return new UpperExp(exp);
    }

    /**
     * @param path object path value
     * @return UPPER() call expression
     */
    public static Expression upperExp(String path) {
        return new UpperExp(ExpressionFactory.pathExp(path));
    }

    /**
     * @param exp string expression
     * @return LENGTH() call expression
     */
    public static Expression lengthExp(Expression exp) {
        return new LengthExp(exp);
    }

    /**
     * @param path object path value
     * @return LENGTH() call expression
     */
    public static Expression lengthExp(String path) {
        return new LengthExp(ExpressionFactory.pathExp(path));
    }

    /**
     * Call LOCATE(substring, string) function that return position
     * of substring in string or 0 if it is not found.
     *
     * @param substring object path value
     * @param exp       string expression
     * @return LOCATE() call expression
     */
    public static Expression locateExp(String substring, Expression exp) {
        return locateExp(ExpressionFactory.wrapScalarValue(substring), exp);
    }

    /**
     * Call LOCATE(substring, string) function that return position
     * of substring in string or 0 if it is not found.
     *
     * @param substring object path value
     * @param path      object path
     * @return LOCATE() call expression
     */
    public static Expression locateExp(String substring, String path) {
        return locateExp(ExpressionFactory.wrapScalarValue(substring), ExpressionFactory.pathExp(path));
    }

    /**
     * Call LOCATE(substring, string) function that return position
     * of substring in string or 0 if it is not found.
     *
     * @param substring string expression
     * @param exp       string expression
     * @return LOCATE() call expression
     */
    public static Expression locateExp(Expression substring, Expression exp) {
        return new LocateExp(substring, exp);
    }

    /**
     * @param exp numeric expression
     * @return ABS() call expression
     */
    public static Expression absExp(Expression exp) {
        return new AbsExp(exp);
    }

    /**
     * @param path object path value
     * @return ABS() call expression
     */
    public static Expression absExp(String path) {
        return new AbsExp(ExpressionFactory.pathExp(path));
    }

    /**
     * @param exp numeric expression
     * @return SQRT() call expression
     */
    public static Expression sqrtExp(Expression exp) {
        return new SqrtExp(exp);
    }

    /**
     * @param path object path value
     * @return SQRT() call expression
     */
    public static Expression sqrtExp(String path) {
        return new SqrtExp(ExpressionFactory.pathExp(path));
    }

    /**
     * @param exp    numeric expression
     * @param number divisor
     * @return MOD() call expression
     */
    public static Expression modExp(Expression exp, Number number) {
        return modExp(exp, ExpressionFactory.wrapScalarValue(number));
    }

    /**
     * @param path   object path value
     * @param number divisor
     * @return MOD() call expression
     */
    public static Expression modExp(String path, Number number) {
        return modExp(ExpressionFactory.pathExp(path), ExpressionFactory.wrapScalarValue(number));
    }

    /**
     * @param exp    object path value
     * @param number numeric expression
     * @return MOD() call expression
     */
    public static Expression modExp(Expression exp, Expression number) {
        return new ModExp(exp, number);
    }

    /**
     * <p>
     * Factory method for expression to call CONCAT(string1, string2, ...) function
     * </p>
     * <p>
     * Can be used like: <pre>
     *  Expression concat = concatExp(SomeClass.POPERTY_1.getPath(), SomeClass.PROPERTY_2.getPath());
     * </pre>
     * </p>
     * <p>
     * SQL generation note:
     * <ul>
     *      <li> if DB supports CONCAT function with vararg then it will be used
     *      <li> if DB supports CONCAT function with two args but also supports concat operator, then operator (eg ||) will be used
     *      <li> if DB supports only CONCAT function with two args then it will be used what can lead to SQL exception if
     * used with more than two arguments
     * </ul>
     * </p>
     * <p>Currently only known DB with limited concatenation functionality is Openbase.</p>
     *
     * @param expressions array of expressions
     * @return CONCAT() call expression
     */
    public static Expression concatExp(Expression... expressions) {
        if (expressions == null || expressions.length == 0) {
            return new ConcatExp();
        }

        return new ConcatExp((Object[]) expressions);
    }

    /**
     * <p>
     * Factory method for expression to call CONCAT(string1, string2, ...) function
     * </p>
     * <p>
     * Can be used like:<pre>
     *  Expression concat = concatExp("property1", "property2");
     * </pre>
     * </p>
     * <p>
     * SQL generation note:
     * <ul>
     *      <li> if DB supports CONCAT function with vararg then it will be used
     *      <li> if DB supports CONCAT function with two args but also supports concat operator, then operator (eg ||) will be used
     *      <li> if DB supports only CONCAT function with two args then it will be used what can lead to SQL exception if
     * used with more than two arguments
     * </ul>
     * </p>
     * <p>Currently only Openbase DB has limited concatenation functionality.</p>
     *
     * @param paths array of paths
     * @return CONCAT() call expression
     */
    public static Expression concatExp(String... paths) {
        if (paths == null || paths.length == 0) {
            return new ConcatExp();
        }

        Expression[] expressions = new Expression[paths.length];
        for (int i = 0; i < paths.length; i++) {
            expressions[i] = ExpressionFactory.pathExp(paths[i]);
        }
        return new ConcatExp((Object[]) expressions);
    }

    /**
     * @return Expression COUNT(&ast;)
     */
    public static Expression countExp() {
        return new CountExp(new AsteriskExp());
    }

    /**
     * @return Expression COUNT(exp)
     */
    public static Expression countExp(Expression exp) {
        return new CountExp(exp);
    }

    /**
     * @return Expression COUNT(DISTINCT(exp))
     * @since 4.1
     */
    public static Expression countDistinctExp(Expression exp) {
        return new CountExp(new DistinctExp(exp));
    }

    /**
     * @return Expression MIN(exp)
     */
    public static Expression minExp(Expression exp) {
        return new MinExp(exp);
    }

    /**
     * @return Expression MAX(exp)
     */
    public static Expression maxExp(Expression exp) {
        return new MaxExp(exp);
    }

    /**
     * @return Expression AVG(exp)
     */
    public static Expression avgExp(Expression exp) {
        return new AvgExp(exp);
    }

    /**
     * @return SUM(exp) expression
     */
    public static Expression sumExp(Expression exp) {
        return new SumExp(exp);
    }

    /**
     * @return *function*(exp) expression
     * @since 5.0
     */
    public static Expression customAggregateExp(String function, Expression exp) {
        CustomAggregateExp aggregate = new CustomAggregateExp(exp);
        aggregate.setFunctionName(function);
        return aggregate;
    }

    /**
     * @return CURRENT_DATE expression
     */
    public static Expression currentDate() {
        return new CurrentDateExp();
    }

    /**
     * @return CURRENT_TIME expression
     */
    public static Expression currentTime() {
        return new CurrentTimeExp();
    }

    /**
     * @return CURRENT_TIMESTAMP expression
     */
    public static Expression currentTimestamp() {
        return new CurrentTimestampExp();
    }

    /**
     * @param exp date/timestamp expression
     * @return year(exp) function expression
     */
    public static Expression yearExp(Expression exp) {
        return extractExp(exp, ExtractExp.DateTimePart.YEAR);
    }

    /**
     * @param path String path
     * @return year(path) function expression
     */
    public static Expression yearExp(String path) {
        return extractExp(path, ExtractExp.DateTimePart.YEAR);
    }

    /**
     * @param exp date/timestamp expression
     * @return month(exp) function expression
     */
    public static Expression monthExp(Expression exp) {
        return extractExp(exp, ExtractExp.DateTimePart.MONTH);
    }

    /**
     * @param path String path
     * @return month(path) function expression
     */
    public static Expression monthExp(String path) {
        return extractExp(path, ExtractExp.DateTimePart.MONTH);
    }

    /**
     * @param exp date/timestamp expression
     * @return week(exp) function expression
     */
    public static Expression weekExp(Expression exp) {
        return extractExp(exp, ExtractExp.DateTimePart.WEEK);
    }

    /**
     * @param path String path
     * @return week(path) function expression
     */
    public static Expression weekExp(String path) {
        return extractExp(path, ExtractExp.DateTimePart.WEEK);
    }

    /**
     * @param exp date/timestamp expression
     * @return dayOfYear(exp) function expression
     */
    public static Expression dayOfYearExp(Expression exp) {
        return extractExp(exp, ExtractExp.DateTimePart.DAY_OF_YEAR);
    }

    /**
     * @param path String path
     * @return dayOfYear(path) function expression
     */
    public static Expression dayOfYearExp(String path) {
        return extractExp(path, ExtractExp.DateTimePart.DAY_OF_YEAR);
    }

    /**
     * @param exp date/timestamp expression
     * @return dayOfMonth(exp) function expression, synonym for day()
     */
    public static Expression dayOfMonthExp(Expression exp) {
        return extractExp(exp, ExtractExp.DateTimePart.DAY_OF_MONTH);
    }

    /**
     * @param path String path
     * @return dayOfMonth(path) function expression, synonym for day()
     */
    public static Expression dayOfMonthExp(String path) {
        return extractExp(path, ExtractExp.DateTimePart.DAY_OF_MONTH);
    }

    /**
     * @param exp date/timestamp expression
     * @return dayOfWeek(exp) function expression
     */
    public static Expression dayOfWeekExp(Expression exp) {
        return extractExp(exp, ExtractExp.DateTimePart.DAY_OF_WEEK);
    }

    /**
     * @param path String path
     * @return dayOfWeek(path) function expression
     */
    public static Expression dayOfWeekExp(String path) {
        return extractExp(path, ExtractExp.DateTimePart.DAY_OF_WEEK);
    }

    /**
     * @param exp date/timestamp expression
     * @return hour(exp) function expression
     */
    public static Expression hourExp(Expression exp) {
        return extractExp(exp, ExtractExp.DateTimePart.HOUR);
    }

    /**
     * @param path String path
     * @return hour(path) function expression
     */
    public static Expression hourExp(String path) {
        return extractExp(path, ExtractExp.DateTimePart.HOUR);
    }

    /**
     * @param exp date/timestamp expression
     * @return minute(exp) function expression
     */
    public static Expression minuteExp(Expression exp) {
        return extractExp(exp, ExtractExp.DateTimePart.MINUTE);
    }

    /**
     * @param path String path
     * @return minute(path) function expression
     */
    public static Expression minuteExp(String path) {
        return extractExp(path, ExtractExp.DateTimePart.MINUTE);
    }

    /**
     * @param exp date/timestamp expression
     * @return second(exp) function expression
     */
    public static Expression secondExp(Expression exp) {
        return extractExp(exp, ExtractExp.DateTimePart.SECOND);
    }

    /**
     * @param path String path
     * @return second(path) function expression
     */
    public static Expression secondExp(String path) {
        return extractExp(path, ExtractExp.DateTimePart.SECOND);
    }

    /**
     * @param function name to call
     * @param args     function arguments
     * @return expression to call "function" with provided arguments
     * @since 4.2
     */
    public static Expression functionCall(String function, Object... args) {
        CustomFunctionExp call = new CustomFunctionExp(args);
        call.setFunctionName(function);
        return call;
    }

    /**
     * @param operator to call
     * @param args     arguments
     * @return expression to use custom "operator" with provided arguments
     * @since 4.2
     */
    public static Expression operator(String operator, Object... args) {
        CustomOperatorExp call = new CustomOperatorExp(args);
        call.setOperator(operator);
        return call;
    }

    static Expression extractExp(String path, ExtractExp.DateTimePart part) {
        return extractExp(ExpressionFactory.pathExp(path), part);
    }

    static Expression extractExp(Expression exp, ExtractExp.DateTimePart part) {
        ExtractExp extract = new ExtractExp(exp);
        extract.setPart(part);
        return extract;
    }
}
