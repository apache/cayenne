package de.jexp.jequel.sql;

import de.jexp.jequel.Delimeter;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.SimpleListExpression;
import de.jexp.jequel.literals.SelectKeyword;

public class SelectPartColumnListExpression <E extends Expression> extends SimpleListExpression<E> implements SelectPartExpression<E> {
    private final SelectKeyword selectKeyword;

    public SelectPartColumnListExpression(SelectKeyword selectKeyword) {
        this(selectKeyword, Delimeter.COMMA);
    }

    public SelectPartColumnListExpression(SelectKeyword selectKeyword, Delimeter delimeter, E... expressions) {
        super(delimeter, expressions);

        this.selectKeyword = selectKeyword;
    }

    public String toString() {
        return accept(SQL_FORMAT);
    }

    public <R> R accept(SqlVisitor<R> sqlVisitor) {
        return sqlVisitor.visit(this);
    }

    public SelectKeyword getSelectKeyword() {
        return selectKeyword;
    }


}