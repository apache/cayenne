package de.jexp.jequel.expression;

import de.jexp.jequel.sql.SqlModel;

import java.util.Map;

public interface ITable<T extends ITable> extends PathExpression<T>, TablePart, SqlModel.FromSource<T> {

    IColumn getField(String name);

    Map<String, IColumn> getFields();
}
