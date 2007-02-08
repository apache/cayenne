package org.objectstyle.petstore.dao.impl;

import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.query.ParameterizedQuery;

/**
 * @author Andrus Adamchik
 */
public abstract class CayenneDao {

    protected static final int PAGE_SIZE = 4;

    /**
     * Returns thread-bound DataContext.
     */
    protected DataContext getDataContext() {
        return DataContext.getThreadDataContext();
    }

    /**
     * Fetches a single object matching a named query and a set of parameters.
     */
    protected Object findObject(String queryName, Map parameters) {
        DataContext context = getDataContext();
        ParameterizedQuery q = (ParameterizedQuery) context
                .getEntityResolver()
                .lookupQuery(queryName);

        return DataObjectUtils.objectForQuery(context, q.createQuery(parameters));
    }

    /**
     * Returns a list of objects for the named query.
     */
    protected List findObjects(String queryName, Map parameters) {
        return getDataContext().performQuery(queryName, parameters, false);
    }
}
