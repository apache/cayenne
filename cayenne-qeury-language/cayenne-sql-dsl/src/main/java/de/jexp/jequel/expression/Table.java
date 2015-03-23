package de.jexp.jequel.expression;

import de.jexp.jequel.sql.SqlDsl;
import de.jexp.jequel.expression.types.BIGINT;
import de.jexp.jequel.expression.types.INTEGER;

import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 *
 * @param <A> - return type for alias operation
 */
public class Table<A extends Table> extends AbstractExpression implements ITable<A>, Aliased<A> {

    private static final String OID_COLUMN = "OID"; // TODO FieldType PK

    private final String tableName = getClass().getSimpleName().toUpperCase();

    private final Map<String, IColumn> fields = new LinkedHashMap<String, IColumn>();

    private String alias;

    public String toString() {
        return accept(EXPRESSION_FORMAT);
    }

    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }

    public A as(String alias) {
        try {
            A a = (A) getClass().newInstance();
            a.setAlias(alias);
            a.factory(factory());

            return a;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getName() {
        return isBlank(alias) ? tableName : alias;
    }

    public Column field(int jdbcType) {
        return new Column(this, jdbcType);
    }

    protected <T> IColumn foreignKey(IColumn reference) {
        return new ForeignKey<T>(this, reference);
    }

    protected IColumn foreignKey(Class<? extends Table> tableClass, String field) {
        return new ForeignKey(this, new FieldReference(tableClass, field));
    }

    protected <T> IColumn foreignKey(Class<?> schemaClass, Class<? extends Table> tableClass, String field) {
        return new ForeignKey<T>(this, new FieldReference<T>(schemaClass, tableClass, field));
    }

    public String getTableName() {
        return tableName;
    }

    public IColumn getForeignKey(Table other) {
        return getForeignKey(other.getOid());
    }

    private IColumn getForeignKey(IColumn pkColumn) {
        String pkTable = pkColumn.getTable().getTableName();

        for (IColumn column : getFields().values()) {
            if (column instanceof ForeignKey) {
                ForeignKey key = (ForeignKey) column;
                if (key.getColumn().getTableName().equals(pkTable) && key.references(pkColumn)) {
                    return column;
                }
            }
        }
        return null;
    }

    public JoinTable join(Table second) {
        return new JoinTable(this, second);
    }

    public IColumn getOid() {
        return getField(OID_COLUMN);
    }

    @Override
    public IColumn getField(String name) {
        return getFields().get(name.toUpperCase());
    }

    @Override
    public Map<String, IColumn> getFields() {
        if (fields.isEmpty()) {
            initFields();
        }
        return fields;
    }

    protected void initFields() {
        Class type = getClass();
        for (java.lang.reflect.Field instanceField : type.getFields()) {
            if (IColumn.class.isAssignableFrom(instanceField.getType())) {
                String fieldName = instanceField.getName();
                try {
                    IColumn column = (IColumn) instanceField.get(this);
                    if (column instanceof Column) {
                        ((Column) column).initName(fieldName);
                    }
                    fields.put(fieldName, column);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(String.format("Error accessing field %s in table %s", fieldName, this));
                }
            }
        }
    }

    protected INTEGER integer() {
        return new INTEGER(this);
    }

    protected BIGINT bigint() {
        return new BIGINT(this);
    }

    protected Column character(int length) {
        return field(Types.CHAR);
    }

    protected Column string() {
        return field(Types.VARCHAR);
    }

    protected Column numeric() {
        return field(Types.BIGINT);
    }

    protected Column bool() {
        return field(Types.BOOLEAN);
    }

    protected Column date() {
        return field(Types.DATE);
    }

    protected Column timestamp() {
        return field(Types.TIMESTAMP);
    }

    @Override
    public String getValue() {
        return tableName;
    }

    @Override
    public List<PathExpression> columns() {
        return new ArrayList<PathExpression>(fields.values());
    }

    @Override
    public <R> R accept(SqlDsl.SqlVisitor<R> sqlVisitor) {
        return sqlVisitor.visit((Aliased<? extends Expression>) this);
    }

    protected void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public A getAliased() {
        return (A) this;
    }

    @Override
    public String getAlias() {
        return alias;
    }
}