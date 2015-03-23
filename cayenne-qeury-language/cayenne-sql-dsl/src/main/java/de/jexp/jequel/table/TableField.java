package de.jexp.jequel.table;

import de.jexp.jequel.expression.AbstractExpression;
import de.jexp.jequel.expression.DefaultExpressionsFactory;
import de.jexp.jequel.expression.visitor.ExpressionVisitor;

public class TableField<T> extends AbstractExpression implements
        Field<T> {

    private final Table table;
    private final int jdbcType;

    private String name;

    private boolean primaryKey;
    private boolean mandatory;

    protected TableField(Table table, int jdbcType) {
        this(null, table, jdbcType);
    }

    protected TableField(String name, Table table, int jdbcType) {
        this.name = name;
        this.table = table;
        this.jdbcType = jdbcType;

        factory(EXPRESSIONS_FACTORY);
    }

    public FieldAlias<T> as(String alias) {
        return new FieldAlias<T>(this, alias);
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
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

    public <E extends TableField<T>> E mandatory() {
        this.mandatory = true;
        return (E) this;
    }

    @Override
    public boolean isPrimaryKey() {
        return primaryKey;
    }

    @Override
    public int getJdbcType() {
        return jdbcType;
    }

    @Override
    public boolean isMandatory() {
        return mandatory;
    }

    @Override
    public Table getTable() {
        return table;
    }

    public Field<T> foreignKey(Field<T> reference) {
        return new ForeignKey<T>(getTable(), reference);
    }

    @Override
    public String getValue() {
        return getTableName() + "." + getName();
    }
}
