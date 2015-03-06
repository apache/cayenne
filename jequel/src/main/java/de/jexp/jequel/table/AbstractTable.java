package de.jexp.jequel.table;

import de.jexp.jequel.Delimeter;
import de.jexp.jequel.expression.RowListExpression;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractTable extends RowListExpression implements Table {
    private static final String OID_COLUMN = "OID"; // TODO FieldType PK

    private final Map<String, Field<?>> fields = new LinkedHashMap<String, Field<?>>();

    protected AbstractTable() {
        super(Delimeter.COMMA);
    }

    // todo add themselves to the map on creation (without name for order)
    public abstract <T> Field<T> field(Class<T> type);

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

    protected Field<Integer> integer() {
        return field(Integer.class);
    }

    protected Field<String> string() {
        return field(String.class);
    }

    protected Field<BigDecimal> numeric() {
        return field(BigDecimal.class);
    }

    protected Field<Boolean> bool() {
        return field(Boolean.class);
    }

    protected Field<Date> date() {
        return field(Date.class);
    }

    protected Field<Timestamp> timestamp() {
        return field(Timestamp.class);
    }

    public boolean isParenthesed() {
        return false;
    }
}
