package de.jexp.jequel.table;

import de.jexp.jequel.expression.BooleanExpression;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.LiteralExpression;
import de.jexp.jequel.expression.visitor.ExpressionVisitor;

public class JoinTable extends BaseTable<BaseTable> {
    private final BaseTable first;
    private final BaseTable second;
    private Expression joinOnExpression;

    public JoinTable(BaseTable first, BaseTable second) {
        this.first = first;
        this.second = second;
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit((LiteralExpression<? extends Object>) this);
    }


    public BaseTable getFirst() {
        return first;
    }

    public BaseTable getSecond() {
        return second;
    }

    public Field getOid() {
        return second.getOid(); // first.getOid() // TODO speculative
    }

    public JoinTable on(BooleanExpression expression) {
        joinOnExpression = expression;
        return this;
    }

    public Expression getJoinExpression() {
        if (joinOnExpression != null) {
            return joinOnExpression;
        }

        Field foreignKey = second.getForeignKey(first);
        if (foreignKey != null) {
            joinOnExpression = first.getOid().eq(foreignKey);
        } else {
            joinOnExpression = second.getOid().eq(first.getForeignKey(second));
        }
        return joinOnExpression;
    }
}
