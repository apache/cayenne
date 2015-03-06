package de.jexp.jequel.table;

import de.jexp.jequel.expression.DelegatingFormat;

/**
 * @author mh14 @ jexp.de
 * @since 05.11.2007 01:33:27 (c) 2007 jexp.de
 */
public class TableExpressionFormat extends DelegatingFormat<TableFormat> implements TableFormat {
    public TableExpressionFormat() {
    }

    public TableExpressionFormat(String formatClassName) {
        super(formatClassName);
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