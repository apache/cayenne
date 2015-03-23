package de.jexp.jequel.expression;

public class ForeignKey<T> extends Column<T> {
    private IColumn<T> column;
    private FieldReference<T> reference;

    ForeignKey(ITable table, IColumn<T> column) {
        super(table, column.getJdbcType());
        this.column = column;
    }

    public ForeignKey(ITable table, FieldReference<T> reference) {
        super(table, Integer.MIN_VALUE); // TODO find type here
        this.reference = reference;
    }

    public IColumn<T> getColumn() {
        if (column != null) {
            return column;
        }
        column = reference.resolve();
        return column;
    }

    public boolean references(IColumn<T> other) {
        return getColumn().equals(other);
    }
}
