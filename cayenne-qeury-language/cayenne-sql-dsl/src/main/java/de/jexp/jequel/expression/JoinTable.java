package de.jexp.jequel.expression;

import de.jexp.jequel.sql.SqlDsl;

public class JoinTable extends Table<Table> {
    private final Table first;
    private final Table second;
    private Expression joinOnExpression;

    public JoinTable(Table first, Table second) {
        this.first = first;
        this.second = second;
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public <R> R accept(SqlDsl.SqlVisitor<R> sqlVisitor) {
        return sqlVisitor.visit(this);
    }

    public Table getFirst() {
        return first;
    }

    public Table getSecond() {
        return second;
    }

    public IColumn getOid() {
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

        IColumn foreignKey = second.getForeignKey(first);
        if (foreignKey != null) {
            joinOnExpression = first.getOid().eq(foreignKey);
        } else {
            joinOnExpression = second.getOid().eq(first.getForeignKey(second));
        }
        return joinOnExpression;
    }
}
