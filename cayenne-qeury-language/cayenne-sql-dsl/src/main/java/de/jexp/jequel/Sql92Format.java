package de.jexp.jequel;

import com.sun.org.apache.xpath.internal.operations.Bool;
import de.jexp.jequel.expression.Aliased;
import de.jexp.jequel.expression.BinaryExpression;
import de.jexp.jequel.expression.logical.BooleanBinaryExpression;
import de.jexp.jequel.expression.logical.BooleanExpression;
import de.jexp.jequel.expression.logical.BooleanListExpression;
import de.jexp.jequel.expression.logical.BooleanLiteral;
import de.jexp.jequel.expression.logical.BooleanUnaryExpression;
import de.jexp.jequel.expression.CompoundExpression;
import de.jexp.jequel.expression.ConstantExpression;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.ExpressionAlias;
import de.jexp.jequel.expression.numeric.NumericBinaryExpression;
import de.jexp.jequel.expression.numeric.NumericLiteral;
import de.jexp.jequel.expression.numeric.NumericUnaryExpression;
import de.jexp.jequel.expression.visitor.ExpressionFormat;
import de.jexp.jequel.expression.SearchCondition;
import de.jexp.jequel.expression.ParamExpression;
import de.jexp.jequel.expression.RowListExpression;
import de.jexp.jequel.expression.SimpleListExpression;
import de.jexp.jequel.expression.StringExpression;
import de.jexp.jequel.expression.UnaryExpression;
import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.literals.Operator;
import de.jexp.jequel.literals.SelectKeyword;
import de.jexp.jequel.literals.SqlKeyword;
import de.jexp.jequel.sql.SqlModel;
import de.jexp.jequel.sql.Sql;
import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Field;
import de.jexp.jequel.table.ForeignKey;
import de.jexp.jequel.table.JoinTable;
import de.jexp.jequel.table.TableField;
import de.jexp.jequel.table.visitor.TableFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import static org.apache.commons.lang3.StringUtils.join;

public class Sql92Format implements ExpressionFormat, TableFormat, SqlModel.SqlFormat {

    protected String parenthese(String expressionString) {
        return "(" + expressionString + ")";
    }

    public String formatAround(String expressionString, Expression expression) {
        if (expression instanceof Aliased) {
            Aliased aliased = (Aliased) expression;
            if (aliased.getAlias() != null) {
                expressionString = formatAlias(expressionString, aliased);
            }
        }
        return expressionString;

    }

    protected String formatAlias(String expressionString, Aliased aliased) {
        return expressionString + " as " + aliased.getAlias();
    }

    protected <T> String visit(T constantValue) {
        String valueString = formatConcrete(constantValue);
        if (constantValue instanceof Expression) {
            return formatAround(valueString, (Expression) constantValue);
        }
        return valueString;
        // return constantValue.toString();
    }

    protected String formatConcrete(Object value) {
        if (value instanceof BooleanBinaryExpression) return visit((BooleanBinaryExpression) value);
        if (value instanceof BooleanUnaryExpression) return visit((BooleanUnaryExpression) value);
        if (value instanceof Field) return visit((Field) value);
        if (value instanceof ForeignKey) return visit((ForeignKey) value);
        if (value instanceof BaseTable) return visit((BaseTable) value);
        if (value instanceof NumericLiteral) return visit((NumericLiteral) value);
        if (value instanceof ParamExpression) return visit((ParamExpression) value);
        if (value instanceof SqlModel.SelectPartColumnListExpression) return visit((SqlModel.SelectPartColumnListExpression) value);
        if (value instanceof SqlModel.Select) return visit((SqlModel.Select) value);
        if (value instanceof SqlModel.Where) return visit((SqlModel.Where) value);
        if (value instanceof SqlModel.Having) return visit((SqlModel.Having) value);
        if (value instanceof Sql) return "(" + visit((Sql) value) + ")";
        if (value instanceof StringExpression) return visit((StringExpression) value);
        if (value instanceof TableField) return visit((TableField) value);
        if (value instanceof UnaryExpression) return visit((UnaryExpression) value);
        if (value instanceof CompoundExpression) return visit((CompoundExpression) value);
        if (value instanceof ConstantExpression) return visit((ConstantExpression) value);
        return value.toString();
    }

    public <E extends Expression> String visit(ExpressionAlias<E> expression) {
        return visit(expression.getAliased());
    }

    protected String formatBinaryExpression(Expression first, Operator operator, Expression second) {
        return visit(first) + " " + visit(operator) + " " + visit(second);
    }


    protected <E extends Expression> String implode(SqlKeyword delim, Iterable<E> expressions) {
        Collection<String> strings = new ArrayList<String>(10);
        for (E expression : expressions) {
            if (expression != null) {
                String string = visit(expression);
                if (string.length() > 0) {
                    strings.add(string);
                }
            }
        }

        return join(strings, delim.getSqlKeyword());
    }

    public <V> String visit(ConstantExpression<V> constantExpression) {
        String literal = constantExpression.getLiteral();
        V constantValue = constantExpression.getValue();
        return literal != null ? literal : visit(constantValue);
    }

    public String visit(NumericLiteral numericLiteral) {
        Number value = numericLiteral.getValue();
        return value != null ? visit(value) : visit((Expression) numericLiteral);
    }

    public String visit(BooleanLiteral bool) {
        if (bool.getValue() == null) {
            return "NULL";
        }
        return bool.getValue() ? "TRUE" : "FALSE";
    }

    public String visit(StringExpression stringExpression) {
        return "'" + visit((ConstantExpression<String>) stringExpression) + "'";
    }

    public String visit(UnaryExpression unaryExpression) {
        return visit(unaryExpression.getOperator()) + parenthese(visit(unaryExpression.getExpression()));
    }

    public String visit(BooleanUnaryExpression booleanUnaryExpression) {
        return visit(booleanUnaryExpression.getUnaryExpression());
    }

    public String visit(NumericUnaryExpression numericUnaryExpression) {
        return visit(numericUnaryExpression.getUnaryExpression());
    }

    public String visit(BinaryExpression binaryExpression) {
        Expression first = binaryExpression.getFirst();
        Expression second = binaryExpression.getSecond();
        Operator operator = binaryExpression.getOperator();

        if (operator == Operator.IN) {
            return visitIn(first, second);
        }

        if (!binaryExpression.oneIsNull()) {
            return formatBinaryExpression(first, operator, second);
        }
        if (operator == Operator.EQ) {
            return formatBinaryExpression(first, Operator.IS, second);
        }
        if (operator == Operator.NE) {
            return formatBinaryExpression(first, Operator.IS_NOT, second);
        }

        return formatBinaryExpression(first, operator, second); // TODO not all Operators usable
    }

    protected String visitIn(Expression first, Expression second) {
        if (second instanceof Sql) {
            return visit(first) + " in " + visit(second);
        }

        return visit(first) + " in (" + visit(second) + ")";
    }

    public String visit(BooleanBinaryExpression binaryExpression) {
        return visit(binaryExpression.getBinaryExpression());
    }

    @Override
    public String visit(BooleanListExpression list) {
        LinkedList<String> strings = new LinkedList<String>();
        for (BooleanExpression expression : list.getExpressions()) {
            if (expression == null) {
                continue;
            }

            String string = visit(expression);
            if (string.isEmpty()) {
                continue;
            }

            if (expression instanceof BooleanListExpression) {
                string = "(" + string + ")";
            }
            strings.add(string);
        }

        return join(strings, " " + list.getOperator().getSqlKeyword() + " ");

    }

    public String visit(NumericBinaryExpression binaryExpression) {
        return visit(binaryExpression.getBinaryExpression());
    }

    public String visit(CompoundExpression listExpression) {
        return implode(listExpression.getDelimeter(), listExpression.getExpressions());
    }

    public String visit(RowListExpression rowTupleExpression) {
        return visit((CompoundExpression) rowTupleExpression);
    }

    public <T> String visit(ParamExpression<T> paramExpression) {
        if (paramExpression.isNamedExpression()) {
            return ":" + paramExpression.getLiteral();
        }
        return formatPreparedStatementParameter(paramExpression);
    }

    private <T> String formatPreparedStatementParameter(ParamExpression<T> paramExpression) {
        T value = paramExpression.getValue();
        if (value instanceof Iterable) {
            StringBuilder sb = new StringBuilder();
            for (Iterator it = ((Iterable) value).iterator(); it.hasNext();) {
                it.next();
                sb.append("?");
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        } else {
            return "?";
        }
    }

    public String visit(SqlKeyword operator) {
        String sqlKeyword = operator.getSqlKeyword();
        if (sqlKeyword != null) {
            return sqlKeyword;
        } else {
            return operator.name().toLowerCase().replaceAll("_", " ");
        }
    }

    public String visit(SqlModel.SelectPartColumnListExpression sqlPartColumnTupleExpression) {
        if (sqlPartColumnTupleExpression.hasValues()) {
            return visit(sqlPartColumnTupleExpression.getSelectKeyword()) + " " + visit((SimpleListExpression) sqlPartColumnTupleExpression);
        }

        return "";
    }

    public String visitSearchCondition(SelectKeyword keyword, SearchCondition searchCondition) {
        if (!searchCondition.getBooleanExpression().equals(BooleanLiteral.NULL)) {
            return visit(keyword) + " " + visit(searchCondition.getBooleanExpression());
        } else {
            return "";
        }
    }

    public String visit(SqlModel.Select select) {
        if (!select.hasValues()) {
            return "";
        }
        return "select " + visit((SimpleListExpression) select);
    }

    public String visit(SqlModel.Where where) {
        return visitSearchCondition(SelectKeyword.WHERE, where);
    }

    public String visit(SqlModel.Having having) {
        return visitSearchCondition(SelectKeyword.HAVING, having);
    }

    public String visit(JoinTable joinTable) {
        return parenthese(
                formatAround(visit(joinTable.getFirst()), joinTable.getFirst())
                        + " join " +
                        formatAround(visit(joinTable.getSecond()), joinTable.getSecond())
                        + " on " +
                        parenthese(visit(joinTable.getJoinExpression())));
    }

    public String visit(BaseTable table) {
        if (table instanceof JoinTable) {
            return visit((JoinTable) table);
        }

        return table.getName();
    }

    public <T> String visit(Field<T> field) {
        return field.getTableName() + Delimeter.POINT + field.getName();
    }
}
