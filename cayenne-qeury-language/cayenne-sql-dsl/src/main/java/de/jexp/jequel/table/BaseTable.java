package de.jexp.jequel.table;

import de.jexp.jequel.expression.Aliased;
import de.jexp.jequel.expression.RowListExpression;
import de.jexp.jequel.expression.visitor.ExpressionVisitor;
import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.table.types.BIGINT;
import de.jexp.jequel.table.types.INTEGER;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @param <A> - return type for alias operation
 */
public class BaseTable<A extends BaseTable> extends RowListExpression<A> implements Table, Aliased<A> {

    private static final String OID_COLUMN = "OID"; // TODO FieldType PK

    private final String tableName = getClass().getSimpleName().toUpperCase();

    private final Map<String, Field<?>> fields = new LinkedHashMap<String, Field<?>>();

    private String alias;
    private BaseTable aliasedTable;

    protected BaseTable() {
        super(Delimeter.COMMA);
    }

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }

    public String getAlias() {
        return alias;
    }

    public String getName() {
        return tableName;
    }

    public <K> TableField<K> field(int jdbcType) {
        return new TableField<K>(this, jdbcType);
    }

    protected <T> Field<T> foreignKey(Field<T> reference) {
        return new ForeignKey<T>(this, reference);
    }

    protected <T> Field<T> foreignKey(Class<? extends BaseTable> tableClass, String field) {
        return new ForeignKey<T>(this, new FieldReference<T>(tableClass, field));
    }

    protected <T> Field<T> foreignKey(Class<?> schemaClass, Class<? extends BaseTable> tableClass, String field) {
        return new ForeignKey<T>(this, new FieldReference<T>(schemaClass, tableClass, field));
    }

    public A as(String alias) {
        try {
            A table = (A) getClass().newInstance();
            table.setAlias(alias);
            table.setAliasedTable(this);
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

    public Field getForeignKey(BaseTable other) {
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

    public JoinTable join(BaseTable second) {
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

    public void setAliasedTable(BaseTable aliasedTable) {
        this.aliasedTable = aliasedTable;
    }

    public Field getOid() {
        return getField(OID_COLUMN);
    }

    public Field getField(String name) {
        return getFields().get(name.toUpperCase());
    }

    public Map<String, Field<?>> getFields() {
        if (fields.isEmpty()) {
            initFields();
        }
        return fields;
    }

    protected void initFields() {
        Class type = getClass();
        for (java.lang.reflect.Field instanceField : type.getFields()) {
            if (Field.class.isAssignableFrom(instanceField.getType())) {
                String fieldName = instanceField.getName();
                try {
                    Field field = (Field) instanceField.get(this);
                    if (field instanceof TableField) {
                        ((TableField) field).initName(fieldName);
                    }
                    fields.put(fieldName, field);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(String.format("Error accessing field %s in table %s", fieldName, this));
                }
            }
        }
        append(fields.values());
        // TODO replace with append at creation time, perhaps remove the fields table at all
        // and use the expressions list of superclass for locating fields
    }

    protected INTEGER integer() {
        return new INTEGER(this);
    }

    protected BIGINT bigint() {
        return new BIGINT(this);
    }

    protected TableField<String> character(int length) {
        return field(Types.CHAR);
    }

    protected TableField<String> string() {
        return field(Types.VARCHAR);
    }

    protected TableField<BigDecimal> numeric() {
        return field(Types.BIGINT);
    }

    protected TableField<Boolean> bool() {
        return field(Types.BOOLEAN);
    }

    protected TableField<Date> date() {
        return field(Types.DATE);
    }

    protected TableField<Timestamp> timestamp() {
        return field(Types.TIMESTAMP);
    }
}