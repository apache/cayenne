package de.jexp.jequel.table;

import de.jexp.jequel.expression.DefaultExpressionAlias;
import de.jexp.jequel.expression.visitor.ExpressionVisitor;

public class FieldAlias<T> extends DefaultExpressionAlias<Field<T>> implements Field<T> {
    public FieldAlias(Field<T> aliased, String alias) {
        super(aliased, alias);
    }

    public String getTableName() {
        return getAliased().getTableName();
    }

    public Field resolve() {
        return getAliased().resolve();
    }

    public String getName() {
        return getAliased().getName();
    }

    public boolean isPrimaryKey() {
        return getAliased().isPrimaryKey();
    }

    @Override
    public boolean isMandatory() {
        return getAliased().isMandatory();
    }

    public Table getTable() {
        return getAliased().getTable();
    }

    @Override
    public int getJdbcType() {
        return getAliased().getJdbcType();
    }

    // TODO return Field<T>
    public FieldAlias<T> as(String alias) {
        return new FieldAlias<T>(getAliased(), alias);
    }

    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit((Field<?>) this);
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }
}
