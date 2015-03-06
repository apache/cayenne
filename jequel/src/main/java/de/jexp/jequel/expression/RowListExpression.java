package de.jexp.jequel.expression;

import de.jexp.jequel.Delimeter;

import java.util.Collection;

/**
 * dynamic set of expressions, instance of Collection, used for in()
 */
public abstract class RowListExpression<A extends RowListExpression<A>> extends SimpleListExpression implements Aliased<A> {
    private String alias;

    protected RowListExpression(Delimeter delim, Collection<? extends Expression> expressions) {
        super(delim, expressions);
    }

    protected RowListExpression(Delimeter delim, Expression... expressions) {
        super(delim, expressions);
    }

    public String toString() {
        return this.<String>accept(EXPRESSION_FORMAT);
    }

    public boolean isParenthesed() {
        return true;
    }

    public A as(String alias) {
        this.alias = alias;
        return (A) this;
    }

    public A getAliased() {
        return (A) this;
    }

    public String getAlias() {
        return alias;
    }
}
