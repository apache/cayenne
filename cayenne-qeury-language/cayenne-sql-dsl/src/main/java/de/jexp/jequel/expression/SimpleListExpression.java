package de.jexp.jequel.expression;

import de.jexp.jequel.expression.visitor.ExpressionVisitor;
import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.literals.SqlKeyword;

import java.util.ArrayList;
import java.util.Collection;

import static java.util.Arrays.asList;

/**
 * A list of expressions rendered separated by delimeter
 * a column or select list
 */
public class SimpleListExpression extends AbstractExpression implements CompoundExpression<Expression> {
    private final Collection<Expression> expressions = new ArrayList<Expression>();
    private final Delimeter delim;

    protected SimpleListExpression(Delimeter delim, Collection<Expression> expressions) {
        this.expressions.addAll(expressions);
        this.delim = delim;
    }

    protected SimpleListExpression(Delimeter delim, Expression... expressions) {
        this(delim, asList(expressions));
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> expressionVisitor) {
        return expressionVisitor.visit(this);
    }

    public void append(Collection<? extends Expression> expressions) {
        if (expressions == null || expressions.isEmpty()) {
            return;
        }
        this.expressions.addAll(expressions);
    }

    public void append(Expression... expressions) {
        if (expressions == null || expressions.length == 0) {
            return;
        }
        append(asList(expressions));
    }

    public Collection<Expression> getExpressions() {
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
}
