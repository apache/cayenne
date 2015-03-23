package de.jexp.jequel.table;

import de.jexp.jequel.expression.DefaultExpressionAlias;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.ExpressionAlias;
import de.jexp.jequel.expression.visitor.ExpressionVisitor;

public class FieldAlias<T> extends DefaultExpressionAlias<Field<T>> implements Field<T> {
    public FieldAlias(Field<T> aliased, String alias) {
        super(aliased, alias);
    }

    @Override
    public String getTableName() {
        return getAliased().getTableName();
    }

    @Override
    public Field resolve() {
        return getAliased().resolve();
    }

    @Override
    public String getName() {
        return getAliased().getName();
    }

    @Override
    public boolean isPrimaryKey() {
        return getAliased().isPrimaryKey();
    }

    @Override
    public boolean isMandatory() {
        return getAliased().isMandatory();
    }

    @Override
    public Table getTable() {
        return getAliased().getTable();
    }

    @Override
    public int getJdbcType() {
        return getAliased().getJdbcType();
    }

    // TODO return Field<T>
    @Override
    public FieldAlias<T> as(String alias) {
        return new FieldAlias<T>(getAliased(), alias);
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit((ExpressionAlias<? extends Expression>) this);
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    @Override
    public String getValue() {
        return getTableName() + "." + getAlias();
    }
}
