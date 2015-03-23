package de.jexp.jequel.table;

import de.jexp.jequel.expression.Alias;
import de.jexp.jequel.expression.PathExpression;

public interface IColumn<JavaType> extends TablePart, PathExpression<IColumn>, Alias<IColumn<JavaType>> {

    ITable getTable();

    @Override
    String getName();

    boolean isPrimaryKey();

    boolean isMandatory();

    int getJdbcType();

}
