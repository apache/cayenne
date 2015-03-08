package de.jexp.jequel.check;

import de.jexp.jequel.expression.Expression;

public abstract class CheckSpec {
    private boolean satisfied;
    private boolean violated;

    public abstract void check(Expression expression);

    protected void setSatisfied() {
        satisfied = true;
    }

    public void setViolated() {
        this.violated = true;
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public boolean isViolated() {
        return violated;
    }
}
