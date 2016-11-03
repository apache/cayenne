package org.apache.cayenne.dba.sybase;

import org.apache.cayenne.access.translator.select.DefaultSelectTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

public class SybaseSelectTranslator extends DefaultSelectTranslator {
	/**
	 * @since 4.0
	 */
	public SybaseSelectTranslator(Query query, DbAdapter adapter, EntityResolver entityResolver) {
		super(query, adapter, entityResolver);
	}

	@Override
	protected void appendLimitAndOffsetClauses(StringBuilder buffer) {

		int limit = queryMetadata.getFetchLimit();
		int offset = queryMetadata.getFetchOffset();

		if (limit > 0) {
			String sql = buffer.toString();

			// If contains distinct insert top limit after
			if (sql.startsWith("SELECT DISTINCT ")) {
				buffer.replace(0, 15, "SELECT DISTINCT TOP " + (offset + limit));

			} else {
				buffer.replace(0, 6, "SELECT TOP " + (offset + limit));
			}
		}
	}
}
