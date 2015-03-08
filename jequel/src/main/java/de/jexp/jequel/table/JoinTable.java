package de.jexp.jequel.table;

import de.jexp.jequel.expression.Expression;

public class JoinTable extends BaseTable<BaseTable> {
    private final BaseTable<? extends BaseTable> first;
    private final BaseTable<? extends BaseTable> second;
    private Expression joinExpression;

    public JoinTable(BaseTable<? extends BaseTable> first, BaseTable<? extends BaseTable> second) {
        this.first = first;
        this.second = second;
    }

    public String toString() {
        return accept(TABLE_FORMAT);
    }

    public <R> R accept(TableVisitor<R> tableVisitor) {
        return tableVisitor.visit(this);
    }


    public BaseTable<? extends BaseTable> getFirst() {
        return first;
    }

    public BaseTable<? extends BaseTable> getSecond() {
        return second;
    }

    public Field getOid() {
        return second.getOid(); // first.getOid() // TODO speculative
    }

    public JoinTable on(Expression expression) {
        joinExpression = expression;
        return this;
    }

    public Expression getJoinExpression() {
        if (joinExpression != null) return joinExpression;

        Field foreignKey = second.getForeignKey(first);
        if (foreignKey != null) {
            joinExpression = first.getOid().eq(foreignKey);
        } else {
            joinExpression = second.getOid().eq(first.getForeignKey(second));
        }
        return joinExpression;
    }
}
