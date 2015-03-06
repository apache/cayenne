package de.jexp.jequel.format;

import de.jexp.jequel.Delimeter;
import de.jexp.jequel.expression.Aliased;
import de.jexp.jequel.expression.BinaryExpression;
import de.jexp.jequel.expression.BooleanBinaryExpression;
import de.jexp.jequel.expression.BooleanConstantExpression;
import de.jexp.jequel.expression.BooleanExpression;
import de.jexp.jequel.expression.BooleanUnaryExpression;
import de.jexp.jequel.expression.CompoundExpression;
import de.jexp.jequel.expression.ConstantExpression;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.ExpressionAlias;
import de.jexp.jequel.expression.ExpressionFormat;
import de.jexp.jequel.expression.MutableBooleanExpression;
import de.jexp.jequel.expression.NumericBinaryExpression;
import de.jexp.jequel.expression.NumericExpression;
import de.jexp.jequel.expression.NumericUnaryExpression;
import de.jexp.jequel.expression.ParamExpression;
import de.jexp.jequel.expression.RowListExpression;
import de.jexp.jequel.expression.SimpleListExpression;
import de.jexp.jequel.expression.StringExpression;
import de.jexp.jequel.expression.UnaryExpression;
import de.jexp.jequel.literals.NameUtils;
import de.jexp.jequel.literals.Operator;
import de.jexp.jequel.literals.SqlKeyword;
import de.jexp.jequel.sql.SelectPartColumnListExpression;
import de.jexp.jequel.sql.SelectPartMutableBooleanExpression;
import de.jexp.jequel.sql.Sql;
import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Field;
import de.jexp.jequel.table.ForeignKey;
import de.jexp.jequel.table.JoinTable;
import de.jexp.jequel.table.TableField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class Sql92Format implements ExpressionFormat {

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
        if (value instanceof SelectPartColumnListExpression) return visit((SelectPartColumnListExpression) value);
        if (value instanceof SelectPartMutableBooleanExpression)
            return visit((SelectPartMutableBooleanExpression) value);
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
        StringBuilder result = new StringBuilder();
        for (Iterator<String> iterator = strings.iterator(); iterator.hasNext();) {
            result.append(iterator.next());
            if (iterator.hasNext()) {
                result.append(delim);
            }
        }
        return result.toString();
    }

    protected String nameToOperator(SqlKeyword operator) {
        String constantName = operator.name();
        return NameUtils.constantNameToLowerCaseLiteral(constantName);
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
            return nameToOperator(operator);
        }
    }

    public String visit(SelectPartColumnListExpression sqlPartColumnTupleExpression) {
        if (sqlPartColumnTupleExpression.hasValues()) {
            return visit(sqlPartColumnTupleExpression.getSelectKeyword()) + " " + visit((SimpleListExpression) sqlPartColumnTupleExpression);
        }

        return "";
    }

    public String visit(MutableBooleanExpression mutableBooleanExpression) {
        return mutableBooleanExpression.hasValue() ? visit(mutableBooleanExpression.getBooleanExpression()) : "";
    }

    public String visit(SelectPartMutableBooleanExpression selectPartMutableBooleanExpression) {
        return selectPartMutableBooleanExpression.hasValue() ?
                visit(selectPartMutableBooleanExpression.getSelectKeyword()) + " " + visit((MutableBooleanExpression) selectPartMutableBooleanExpression) : "";
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
