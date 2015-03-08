package de.jexp.jequel.table;

import de.jexp.jequel.expression.DelegatingFormat;

public class TableExpressionFormat extends DelegatingFormat<TableFormat> implements TableFormat {

    public TableExpressionFormat(TableFormat format) {
        super(format);
    }

    public <T> String visit(Field<T> field) {
        return formatAround(getFormat().visit(field), field);
    }

    public String visit(JoinTable joinTable) {
        return formatAround(getFormat().visit(joinTable), joinTable);
    }

    public String visit(BaseTable table) {
        return formatAround(getFormat().visit(table), table);
    }
}