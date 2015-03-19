package de.jexp.jequel.table.types;

import de.jexp.jequel.expression.numeric.NumericBinaryExpression;
import de.jexp.jequel.expression.numeric.NumericExpression;
import de.jexp.jequel.expression.numeric.NumericLiteral;
import de.jexp.jequel.literals.Operator;
import de.jexp.jequel.table.Table;
import de.jexp.jequel.table.TableField;

import java.sql.Types;

import static de.jexp.jequel.literals.Operator.*;

public class NUMERIC extends TableField<Integer> implements NumericExpression {

    public NUMERIC(Table table) {
        this(null, table, Types.INTEGER);
    }

    public NUMERIC(String name, Table table) {
        this(name, table, Types.INTEGER);
    }

    public NUMERIC(String name, Table table, int jdbcType) {
        super(name, table, jdbcType);
    }

    @Override
    public NumericExpression plus(NumericExpression expression) {
        return exp(PLUS, expression);
    }

    @Override
    public NumericExpression plus(Number expression) {
        return exp(PLUS, expression);
    }

    @Override
    public NumericExpression minus(NumericExpression expression) {
        return exp(MINUS, expression);
    }

    @Override
    public NumericExpression minus(Number expression) {
        return exp(MINUS, expression);
    }

    @Override
    public NumericExpression times(NumericExpression expression) {
        return exp(TIMES, expression);
    }

    @Override
    public NumericExpression times(Number expression) {
        return exp(TIMES, expression);
    }

    @Override
    public NumericExpression by(NumericExpression expression) {
        return exp(BY, expression);
    }

    @Override
    public NumericExpression by(Number expression) {
        return exp(BY, expression);
    }

    protected NumericExpression exp(Operator operator, NumericExpression expression) {
        return new NumericBinaryExpression(this, operator, expression);
    }

    protected NumericExpression exp(Operator operator, Number number) {
        return exp(operator, new NumericLiteral(number));
    }
}
