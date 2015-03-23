package de.jexp.jequel.expression;

public interface IColumn<JavaType> extends TablePart, PathExpression<IColumn>, Alias<IColumn<JavaType>> {

    ITable getTable();

    @Override
    String getName();

    boolean isPrimaryKey();

    boolean isMandatory();

    int getJdbcType();

}
