package de.jexp.jequel.table;

import de.jexp.jequel.expression.Alias;
import de.jexp.jequel.expression.Expression;

public interface Field<T> extends Expression, Alias<FieldAlias<T>>, TablePart {
    Field resolve();

    Field<T> primaryKey();

    boolean isPrimaryKey();

    Table getTable();
}
