package de.jexp.jequel.table;

import de.jexp.jequel.Sql92Format;
import de.jexp.jequel.table.visitor.TableVisitor;

public interface TablePart {

    /* this expression format are used in expressins in toString method */
    TableExpressionFormat TABLE_FORMAT = new TableExpressionFormat(new Sql92Format());

    String getName();

    String getTableName();

    <R> R accept(TableVisitor<R> tableVisitor);
}
