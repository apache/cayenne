package de.jexp.jequel.table;

public abstract class BaseTable<T extends BaseTable> extends AbstractTable {

    private final String tableName = getClass().getSimpleName().toUpperCase();
    private String alias;

    protected BaseTable aliasedTable;

    protected BaseTable() {
    }

    public String toString() {
        return accept(TABLE_FORMAT);
    }

    public <R> R accept(TableVisitor<R> tableVisitor) {
        return tableVisitor.visit(this);
    }


    public String getAlias() {
        return alias;
    }

    public String getName() {
        return tableName;
    }

    public <K> Field<K> field(Class<K> type) {
        return new TableField<K>(this);
    }

    protected <T> Field<T> foreignKey(Field<T> reference) {
        return new ForeignKey<T>(this, reference);
    }

    protected <T> Field<T> foreignKey(Class<? extends BaseTable<?>> tableClass, String field) {
        return new ForeignKey<T>(this, new FieldReference<T>(tableClass, field));
    }

    protected <T> Field<T> foreignKey(Class<?> schemaClass, Class<? extends BaseTable<?>> tableClass, String field) {
        return new ForeignKey<T>(this, new FieldReference<T>(schemaClass, tableClass, field));
    }

    public T as(String alias) {
        try {
            //noinspection unchecked
            T table = ((Class<T>) getClass()).newInstance();
            table.setAlias(alias);
            table.aliasedTable = this;
            return table;
        } catch (InstantiationException e) {
            throw new RuntimeException(String.format("Error creating table %s with Alias %s", getClass(), alias), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.format("Error creating table %s with Alias %s", getClass(), alias), e);
        }
    }

    public String getTableName() {
        return getAlias() != null ? getAlias() : tableName;
    }

    public Field getForeignKey(BaseTable<? extends BaseTable> other) {
        return getForeignKey(other.getOid());
    }

    private Field getForeignKey(Field pkField) {
        Field resolve = pkField.resolve();
        for (Field field : getFields().values()) {
            if (field instanceof ForeignKey && ((ForeignKey) field).references(resolve)) {
                return field;
            }
        }
        return null;
    }

    public JoinTable join(BaseTable<? extends BaseTable> second) {
        return new JoinTable(this, second);
    }

    public BaseTable resolve() {
        if (getAlias() == null) {
            return this;
        }
        return aliasedTable;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}