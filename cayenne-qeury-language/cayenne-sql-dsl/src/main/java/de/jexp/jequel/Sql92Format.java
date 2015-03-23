package de.jexp.jequel;

import de.jexp.jequel.expression.*;
import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.literals.SelectKeyword;
import de.jexp.jequel.sql.Sql;
import de.jexp.jequel.sql.SqlDsl;
import de.jexp.jequel.sql.SqlModel.From;
import de.jexp.jequel.sql.SqlModel.Having;
import de.jexp.jequel.sql.SqlModel.Select;
import de.jexp.jequel.sql.SqlModel.SelectPartColumnListExpression;
import de.jexp.jequel.sql.SqlModel.Where;
import de.jexp.jequel.expression.Table;
import de.jexp.jequel.expression.IColumn;
import de.jexp.jequel.expression.JoinTable;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.join;

public class Sql92Format extends Sql92ExpressionVisitor implements SqlDsl.SqlVisitor<String> {

    protected final Sql92ExpressionVisitor expVisitor = new Sql92ExpressionVisitor();

    protected <T> String visit(T constantValue) {
        return constantValue.toString();
    }

    protected String visitIn(Expression first, Expression second) {
        if (second instanceof Sql) {
            return first.accept(expVisitor) + " in " + second.accept(expVisitor);
        }

        return expVisitor.visitIn(first, second);
    }

    public String visit(SelectPartColumnListExpression sqlPartColumnTupleExpression) {
        if (sqlPartColumnTupleExpression.hasValues()) {
            return expVisitor.visit(sqlPartColumnTupleExpression.getSelectKeyword()) + " " + visit((SimpleListExpression) sqlPartColumnTupleExpression);
        }

        return "";
    }

    public String visitSearchCondition(SelectKeyword keyword, SearchCondition searchCondition) {
        if (searchCondition.getBooleanExpression() == null) {
            return "";
        }

        String boolExpr = searchCondition.getBooleanExpression().accept(expVisitor);
        if (isBlank(boolExpr)) {
            return "";
        }

        return expVisitor.visit(keyword) + " " + boolExpr;
    }

    public String visit(Select select) {
        if (!select.hasValues()) {
            return "select *";
        }

        String res = "select ";
        List<String> ee = new ArrayList<String>();
        for (Expression expression : select.getExpressions()) {
            if (expression instanceof SqlDsl.SqlVisitable) {
                ee.add(((SqlDsl.SqlVisitable) expression).accept(this));
            } else {
                ee.add(expression.accept(this));
            }
        }
        return res + join(ee, Delimeter.COMMA.getSqlKeyword());
    }

    public String visit(Where where) {
        return visitSearchCondition(SelectKeyword.WHERE, where);
    }

    public String visit(Having having) {
        return visitSearchCondition(SelectKeyword.HAVING, having);
    }

    @Override
    public String visit(From from) {
        return "from " + joinNotNull(Delimeter.COMMA, from.getSources());
    }

    @Override
    public String visit(Table aTable) {
        return (String) aTable.accept(expVisitor);
    }

    public String visit(JoinTable joinTable) {
        return joinTable.getFirst().accept(this)
                + " join " + joinTable.getSecond().accept(this)
                + " on " + parenthese(visit(joinTable.getJoinExpression()));
    }

    private String parenthese(Object visit) {
        return "(" + visit + ")";
    }

    public <T> String visit(IColumn column) {
        return column.getTableName() + Delimeter.POINT + column.getName();
    }


    @Override
    public <E extends Expression> String visit(Aliased<E> expression) {
        String res = expression.getAliased().accept(expVisitor);
        if (isBlank(expression.getAlias())) {
            return res;
        }

        return res + " as " + expression.getAlias();
    }

    @Override
    public String visit(Sql sql) {
        return joinNotNull(
                Delimeter.SPACE,

                sql.getSelect(),
                sql.getFrom(),
                sql.getWhere(),
                sql.getGroupBy(),
                sql.getHaving(),
                sql.getOrderBy()
        );
    }

    private String joinNotNull(Delimeter space, SqlDsl.SqlVisitable ... visitables) {
        return joinNotNull(space, asList(visitables));
    }

    private String joinNotNull(Delimeter space, Iterable<? extends SqlDsl.SqlVisitable> visitables) {
        ArrayList<String> list = new ArrayList<String>();
        for (SqlDsl.SqlVisitable visitable : visitables) {
            String expStr = visitable.accept(this);
            if (!isBlank(expStr)) {
                list.add(expStr);
            }
        }

        return StringUtils.join(list, space.getSqlKeyword());
    }
}
