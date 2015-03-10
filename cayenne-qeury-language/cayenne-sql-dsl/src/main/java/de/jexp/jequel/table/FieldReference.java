package de.jexp.jequel.table;

public class FieldReference<T> {

    private final Class<?> schemaClass;
    private final Class<? extends BaseTable> tableClass;
    private final String fieldName;

    public FieldReference(Class<? extends BaseTable> tableClass, String fieldName) {
        this(tableClass.getDeclaringClass(), tableClass, fieldName);
    }

    public FieldReference(Class<?> schemaClass, Class<? extends BaseTable> tableClass, String fieldName) {
        this.schemaClass = schemaClass;
        this.tableClass = tableClass;
        this.fieldName = fieldName;
    }

    public Field<T> resolve() {
        String tableName = tableClass.getSimpleName();
        try {
            java.lang.reflect.Field tableField = schemaClass.getField(tableName);
            if (!tableClass.equals(tableField.getType())) {
                throw new IllegalArgumentException(tableName + " type " + tableField.getType() + " is no instance of " + tableClass.getName());
            }
            Table table = (Table) tableField.get(null);
            return table.getField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(tableName + " is no member of " + schemaClass.getName(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(tableName + " is not accessible in " + schemaClass.getName(), e);
        }
    }
}
