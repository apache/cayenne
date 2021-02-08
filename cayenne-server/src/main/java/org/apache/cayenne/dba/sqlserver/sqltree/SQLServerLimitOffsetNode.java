package org.apache.cayenne.dba.sqlserver.sqltree;

import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;

/**
 * @since 4.2
 */
public class SQLServerLimitOffsetNode extends LimitOffsetNode {

    public SQLServerLimitOffsetNode(int limit, int offset) {
        // Per SQLServer documentation: "To retrieve all rows from a certain offset up to the end of the result set,
        // you can use some large number for the second parameter."
        super(limit == 0 && offset > 0 ? Integer.MAX_VALUE : limit, offset);
    }

    @Override
    public QuotingAppendable append(QuotingAppendable buffer) {
        if(limit == 0 && offset == 0) {
            return buffer;
        }
        return buffer.append(" OFFSET ").append(offset).append(" ROWS FETCH NEXT ").append(limit).append(" ROWS ONLY ");
    }

    @Override
    public Node copy() {
        return new SQLServerLimitOffsetNode(limit, offset);
    }

}
