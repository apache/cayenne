package de.jexp.jequel.expression;

import de.jexp.jequel.Delimeter;
import de.jexp.jequel.literals.SqlKeyword;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * A list of expressions rendered separated by delimeter
 * a column or select list
 */
public class SimpleListExpression <E extends Expression> extends AbstractExpression implements CompoundExpression<E> {
    private final Collection<E> expressions = new ArrayList<E>();
    private final Delimeter delim;

    protected SimpleListExpression(Delimeter delim, Collection<E> expressions) {
        this.expressions.addAll(expressions);
        this.delim = delim;
    }

    protected SimpleListExpression(Delimeter delim, E... expressions) {
        this(delim, Arrays.asList(expressions));
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> expressionVisitor) {
        return expressionVisitor.visit(this);
    }

    protected void append(Collection<E> expressions) {
        if (expressions == null || expressions.isEmpty()) {
            return;
        }
        this.expressions.addAll(expressions);
    }

    public void append(E... expressions) {
        if (expressions == null || expressions.length == 0) {
            return;
        }
        append(Arrays.asList(expressions));
    }

    public Collection<E> getExpressions() {
        return expressions;
    }

    public boolean hasValues() {
        return !expressions.isEmpty();
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
        for (Expression expression : getExpressions()) {
            expressionProcessor.process(expression);
        }
    }

    public SqlKeyword getDelimeter() {
        return delim;
    }

    public boolean isAtomic() {
        return false;
    }
}
