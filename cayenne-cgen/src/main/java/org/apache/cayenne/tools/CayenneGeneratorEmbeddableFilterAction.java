package org.apache.cayenne.tools;

import org.apache.cayenne.dbsync.filter.NameFilter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;

import java.util.ArrayList;
import java.util.Collection;

/**
 *  Performs embeddable filtering to build a collection of embedables that should be used in
 *  class generation.
 * @since 4.1
 */
class CayenneGeneratorEmbeddableFilterAction {

    private NameFilter nameFilter;

    Collection<Embeddable> getFilteredEmbeddables(DataMap mainDataMap) {
        Collection<Embeddable> embeddables = new ArrayList<>(mainDataMap.getEmbeddables());

        // filter out excluded entities...

        // note that unlike entity, embeddable is matched by class name as it doesn't
        // have a symbolic name...
        embeddables.removeIf(e -> !nameFilter.isIncluded(e.getClassName()));

        return embeddables;
    }

    public void setNameFilter(NameFilter nameFilter) {
        this.nameFilter = nameFilter;
    }
}
