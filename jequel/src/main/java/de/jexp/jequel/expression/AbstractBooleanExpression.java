package de.jexp.jequel.expression;

import de.jexp.jequel.literals.Operator;

public abstract class AbstractBooleanExpression extends AbstractExpression implements BooleanExpression {
    public BooleanExpression and(BooleanExpression expression) {
        return createBinaryBooleanExpression(Operator.AND, expression);
    }

    public BooleanExpression or(BooleanExpression expression) {
        return createBinaryBooleanExpression(Operator.OR, expression);
    }

    public BooleanExpressionAlias as(String alias) {
        return new BooleanExpressionAlias(alias);
    }

    private class BooleanExpressionAlias extends DefaultExpressionAlias<BooleanExpression> implements BooleanExpression {
        public BooleanExpressionAlias(String alias) {
            super(AbstractBooleanExpression.this, alias);
        }

        public BooleanExpression and(BooleanExpression expression) {
            return getAliased().and(expression);
        }

        public BooleanExpression or(BooleanExpression expression) {
            return getAliased().or(expression);
        }
    }
}
