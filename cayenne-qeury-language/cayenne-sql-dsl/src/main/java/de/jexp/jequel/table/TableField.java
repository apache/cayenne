package de.jexp.jequel.table;

import de.jexp.jequel.expression.AbstractExpression;
import de.jexp.jequel.expression.visitor.ExpressionVisitor;
import de.jexp.jequel.table.visitor.TableVisitor;

public class TableField<T> extends AbstractExpression implements Field<T> {
    private final Table table;

    private String name;
    private boolean primaryKey;

    protected TableField(Table table) {
        this(null, table);
    }

    protected TableField(String name, Table table) {
        this.name = name;
        this.table = table;
    }

    public FieldAlias<T> as(String alias) {
        return new FieldAlias<T>(this, alias);
    }

    public String toString() {
        return accept(TABLE_FORMAT);
    }

    public <R> R accept(TableVisitor<R> tableVisitor) {
        return tableVisitor.visit(this);
    }

    public <R> R accept(ExpressionVisitor<R> expressionVisitor) {
        throw new UnsupportedOperationException("accept only for TableVisitor");
    }

    public String getTableName() {
        return table.getTableName();
    }

    public Field resolve() {
        return table.resolve().getField(name);
    }

    void initName(String name) {
        if (this.name == null) {
            this.name = name;

            return;
        }

        throw new IllegalStateException("Name already set " + this);
    }

    @Override
    public String getName() {
        return name;
    }

    public <E extends TableField<T>> E primaryKey() {
        this.primaryKey = true;
        return (E) this;
    }

    @Override
    public boolean isPrimaryKey() {
        return primaryKey;
    }

    @Override
    public Table getTable() {
        return table;
    }

    public Field<T> foreignKey(Field<T> reference) {
        return new ForeignKey<T>(getTable(), reference);
    }
}
