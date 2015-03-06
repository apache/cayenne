package de.jexp.jequel.sql;

import de.jexp.jequel.expression.BooleanExpression;
import de.jexp.jequel.expression.MutableBooleanExpression;
import de.jexp.jequel.literals.SelectKeyword;

/**
 * @author mh14 @ jexp.de
 * @since 25.10.2007 01:03:54 (c) 2007 jexp.de
 */
public class SelectPartMutableBooleanExpression extends MutableBooleanExpression implements SelectPartExpression<BooleanExpression> {
    private final SelectKeyword selectKeyword;

    public SelectPartMutableBooleanExpression(SelectKeyword selectKeyword) {
        super(null);
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

    public void append(BooleanExpression... expressions) {
        for (BooleanExpression expression : expressions) {
            and(expression);
        }
    }
}
