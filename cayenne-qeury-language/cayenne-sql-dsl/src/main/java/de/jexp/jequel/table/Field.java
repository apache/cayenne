package de.jexp.jequel.table;

import de.jexp.jequel.expression.Alias;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.VariableExpression;

public interface Field<T> extends Alias<FieldAlias<T>>, TablePart , VariableExpression {
    Field resolve();

    Table getTable();

    int getJdbcType();

    @Override
    String getName();

    boolean isPrimaryKey();

    boolean isMandatory();
}
