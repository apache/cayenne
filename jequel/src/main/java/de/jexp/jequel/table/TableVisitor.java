package de.jexp.jequel.table;

/**
 * @author mh14 @ jexp.de
 * @since 05.11.2007 00:45:23 (c) 2007 jexp.de
 */
public interface TableVisitor<R> {
    <T> R visit(Field<T> field);

    R visit(JoinTable joinTable);

    R visit(BaseTable table);
}
