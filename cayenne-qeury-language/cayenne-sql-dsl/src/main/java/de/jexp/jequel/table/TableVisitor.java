package de.jexp.jequel.table;

public interface TableVisitor<R> {
    <T> R visit(Field<T> field);

    R visit(JoinTable joinTable);

    R visit(BaseTable table);
}
