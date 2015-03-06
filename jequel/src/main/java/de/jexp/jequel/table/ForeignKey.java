package de.jexp.jequel.table;

public class ForeignKey<T> extends TableField<T> {
    private Field<T> field;
    private FieldReference<T> reference;

    ForeignKey(Table table, Field<T> field) {
        super(table);
        this.field = field;
    }

    public ForeignKey(Table table, FieldReference<T> reference) {
        super(table);
        this.reference = reference;
    }

    public Field<T> getField() {
        if (field != null) {
            return field;
        }
        field = reference.resolve();
        return field;
    }

    public boolean references(Field<T> other) {
        return getField().equals(other);
    }
}
