package de.jexp.jequel.table;

import de.jexp.jequel.expression.DefaultExpressionAlias;

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

    public Field<T> primaryKey() {
        return getAliased().primaryKey();
    }

    public boolean isPrimaryKey() {
        return getAliased().isPrimaryKey();
    }

    public Table getTable() {
        return getAliased().getTable();
    }

    // TODO return Field<T>
    public FieldAlias<T> as(String alias) {
        return new FieldAlias<T>(getAliased(), alias);
    }

    public <R> R accept(TableVisitor<R> tableVisitor) {
        return tableVisitor.visit(this);
    }

    public String toString() {
        return accept(TABLE_FORMAT);
    }
}
