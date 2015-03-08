package de.jexp.jequel;

import de.jexp.jequel.expression.*;
import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.literals.Operator;
import de.jexp.jequel.literals.SelectKeyword;
import de.jexp.jequel.literals.SqlKeyword;
import de.jexp.jequel.sql.DslSqlModel;
import de.jexp.jequel.sql.Sql;
import de.jexp.jequel.table.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static org.apache.commons.lang3.StringUtils.join;

public class Sql92Format implements ExpressionFormat, TableFormat, DslSqlModel.SqlFormat {

    protected String parenthese(String expressionString) {
        return "(" + expressionString + ")";
    }

    public String formatAround(String expressionString, Expression expression) {
        if (expression.isParenthesed()) {
            expressionString = parenthese(expressionString);
        }
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
        if (value instanceof NumericExpression) return visit((NumericExpression) value);
        if (value instanceof ParamExpression) return visit((ParamExpression) value);
        if (value instanceof DslSqlModel.SelectPartColumnListExpression) return visit((DslSqlModel.SelectPartColumnListExpression) value);
        if (value instanceof DslSqlModel.Where) return visit((DslSqlModel.Where) value);
        if (value instanceof DslSqlModel.Having) return visit((DslSqlModel.Having) value);
        if (value instanceof Sql) return visit((Sql) value);
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

    public String visit(NumericExpression numericExpression) {
        Number value = numericExpression.getValue();
        return value != null ? visit(value) : visit((Expression) numericExpression);
    }

    public String visit(BooleanConstantExpression booleanConstantExpression) {
        String literal = booleanConstantExpression.getLiteral();
        return literal != null ? literal : visit((BooleanExpression) booleanConstantExpression);
    }

    public String visit(StringExpression stringExpression) {
        return "'" + visit((ConstantExpression<String>) stringExpression) + "'";
    }

    public String visit(UnaryExpression unaryExpression) {
        return visit(unaryExpression.getOperator()) + parenthese(visit(unaryExpression.getFirst()));
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

    public String visit(BooleanBinaryExpression binaryExpression) {
        return visit(binaryExpression.getBinaryExpression());
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

    public String visit(DslSqlModel.SelectPartColumnListExpression sqlPartColumnTupleExpression) {
        if (sqlPartColumnTupleExpression.hasValues()) {
            return visit(sqlPartColumnTupleExpression.getSelectKeyword()) + " " + visit((SimpleListExpression) sqlPartColumnTupleExpression);
        }

        return "";
    }

    public String visit(MutableBooleanExpression mutableBooleanExpression) {
        return mutableBooleanExpression.hasValue() ? visit(mutableBooleanExpression.getBooleanExpression()) : "";
    }

    public String visit(DslSqlModel.Where where) {
        return where.hasValue() ?
                visit(SelectKeyword.WHERE) + " " + visit((MutableBooleanExpression) where) : "";
    }

    public String visit(DslSqlModel.Having having) {
        return having.hasValue() ?
                visit(SelectKeyword.HAVING) + " " + visit((MutableBooleanExpression) having) : "";
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
