package de.jexp.jequel.table;

import de.jexp.jequel.expression.Expression;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * @since 18.10.2007 09:31:18
 */
public class JoinTable extends BaseTable<BaseTable> {
    private final BaseTable<? extends BaseTable> first;
    private final BaseTable<? extends BaseTable> second;
    private Expression<Boolean> joinExpression;

    public JoinTable(final BaseTable<? extends BaseTable> first, final BaseTable<? extends BaseTable> second) {
        this.first = first;
        this.second = second;
    }

    public String toString() {
        return accept(TABLE_FORMAT);
    }

    public <R> R accept(final TableVisitor<R> tableVisitor) {
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

    public JoinTable on(final Expression<Boolean> expression) {
        joinExpression = expression;
        return this;
    }

    public Expression<Boolean> getJoinExpression() {
        if (joinExpression != null) return joinExpression;

        final Field foreignKey = second.getForeignKey(first);
        if (foreignKey != null) {
            joinExpression = first.getOid().eq(foreignKey);
        } else {
            joinExpression = second.getOid().eq(first.getForeignKey(second));
        }
        return joinExpression;
    }
}
