package de.jexp.jequel.expression;

import de.jexp.jequel.literals.SqlKeyword;

import java.util.Collection;

public interface CompoundExpression<E extends Expression> extends Expression {
    Collection<E> getExpressions();

    boolean hasValues();

    SqlKeyword getDelimeter();
}
