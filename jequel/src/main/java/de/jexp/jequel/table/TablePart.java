package de.jexp.jequel.table;

public interface TablePart {
    TableExpressionFormat TABLE_FORMAT = new TableExpressionFormat();

    String getName();

    String getTableName();

    <R> R accept(TableVisitor<R> tableVisitor);
}
