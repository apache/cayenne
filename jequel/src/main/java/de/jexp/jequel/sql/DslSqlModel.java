package de.jexp.jequel.sql;

import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.expression.*;
import de.jexp.jequel.Sql92Format;
import de.jexp.jequel.literals.SelectKeyword;

public interface DslSqlModel {

    class From {

    }

    class OrderBy {}

    class GroupBy {}

    class SelectPartColumnListExpression extends SimpleListExpression implements SelectPartExpression<Expression> {
        private final SelectKeyword selectKeyword;

        public SelectPartColumnListExpression(SelectKeyword selectKeyword) {
            this(selectKeyword, Delimeter.COMMA);
        }

        public SelectPartColumnListExpression(SelectKeyword selectKeyword, Delimeter delimeter, Expression... expressions) {
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

    interface SelectPartExpression<T extends Expression> extends AppendableExpression<T> {
        SqlExpressionFormat SQL_FORMAT = new SqlExpressionFormat(new Sql92Format());

        SelectKeyword getSelectKeyword();
    }

    class Where extends MutableBooleanExpression { }

    class Having extends MutableBooleanExpression { }


    class SqlExpressionFormat extends DelegatingFormat<SqlFormat> implements SqlFormat {

        public SqlExpressionFormat(SqlFormat format) {
            super(format);
        }

        public String visit(SelectPartColumnListExpression sqlPartColumnTupleExpression) {
            return formatAround(getFormat().visit(sqlPartColumnTupleExpression), sqlPartColumnTupleExpression);
        }

        public String visit(Where where) {
            return formatAround(getFormat().visit(where), where);
        }

        public String visit(Having having) {
            return formatAround(getFormat().visit(having), having);
        }
    }

    /**
     * @author mh14 @ jexp.de
     * @since 09.11.2007 00:21:49 (c) 2007 jexp.de
     */
    interface SqlFormat extends SqlVisitor<String>, Format {
    }

    /**
     * @author mh14 @ jexp.de
     * @since 05.11.2007 00:43:35 (c) 2007 jexp.de
     */
    interface SqlVisitor<R> {
        R visit(SelectPartColumnListExpression sqlPartColumnTupleExpression);

        R visit(Where where);

        R visit(Having having);
    }
}
