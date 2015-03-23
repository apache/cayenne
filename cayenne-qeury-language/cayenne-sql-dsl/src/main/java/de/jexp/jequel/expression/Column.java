package de.jexp.jequel.expression;

import de.jexp.jequel.sql.SqlDsl;

public class Column<JavaType> extends AbstractExpression implements IColumn<JavaType> {

    private final ITable table;
    private final int jdbcType;

    private String name;

    private boolean primaryKey;
    private boolean mandatory;

    protected Column(ITable table, int jdbcType) {
        this(null, table, jdbcType);
    }

    protected Column(String name, ITable table, int jdbcType) {
        this.name = name;
        this.table = table;
        this.jdbcType = jdbcType;

        factory(EXPRESSIONS_FACTORY);
    }

    public IColumn<JavaType> as(String alias) {
        return new AliasedColumn<JavaType>(this, alias);
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }

    public String getTableName() {
        return table.getName();
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

    public <E extends Column> E primaryKey() {
        this.primaryKey = true;
        return (E) this;
    }

    public <E extends Column> E mandatory() {
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
    public ITable getTable() {
        return table;
    }

    public IColumn<JavaType> foreignKey(IColumn reference) {
        return new ForeignKey<JavaType>(getTable(), reference);
    }

    @Override
    public String getValue() {
        return getTableName() + "." + getName();
    }

    @Override
    public <R> R accept(SqlDsl.SqlVisitor<R> sqlVisitor) {
        return sqlVisitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Column column = (Column) o;

        if (jdbcType != column.jdbcType) return false;
        if (mandatory != column.mandatory) return false;
        if (primaryKey != column.primaryKey) return false;
        if (!name.equals(column.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = jdbcType;
        result = 31 * result + name.hashCode();
        result = 31 * result + (primaryKey ? 1 : 0);
        result = 31 * result + (mandatory ? 1 : 0);
        return result;
    }
}
