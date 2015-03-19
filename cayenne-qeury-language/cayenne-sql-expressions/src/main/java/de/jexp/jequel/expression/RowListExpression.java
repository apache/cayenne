package de.jexp.jequel.expression;

import de.jexp.jequel.literals.Delimeter;

import java.util.Collection;

/**
 * dynamic set of expressions, instance of Collection, used for in()
 *
 * @param <A> - return type for alias operation
 */
public class RowListExpression<A extends RowListExpression> extends SimpleListExpression implements Aliased<A> {
    private String alias;

    protected RowListExpression(Delimeter delim, Collection<Expression> expressions) {
        super(delim, expressions);
    }

    protected RowListExpression(Delimeter delim, Expression... expressions) {
        super(delim, expressions);
    }

    public String toString() {
        return this.accept(EXPRESSION_FORMAT);
    }

    public RowListExpression as(String alias) {
        this.alias = alias;
        return this;
    }

    public A getAliased() {
        return (A) this;
    }

    public String getAlias() {
        return alias;
    }
}
