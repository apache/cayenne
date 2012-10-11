package org.apache.cayenne.access;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.util.EqualsBuilder;
import org.apache.cayenne.util.HashCodeBuilder;

/**
 * An id similar to ObjectId that identifies a DbEntity snapshot for implicit
 * DbEntities of flattened attributes or relationships. Provides 'equals' and
 * 'hashCode' implementations adequate for use as a map key.
 * 
 * @since 3.2
 */
final class DbArcId {

    private int hashCode;

    private DbEntity entity;
    private ObjectId sourceId;
    private String incomingArc;

    DbArcId(DbEntity entity, ObjectId sourceId, String incomingArc) {
        this.entity = entity;
        this.sourceId = sourceId;
        this.incomingArc = incomingArc;
    }
    
    DbEntity getEntity() {
        return entity;
    }
    
    ObjectId getSourceId() {
        return sourceId;
    }
    
    String getIncominArc() {
        return incomingArc;
    }

    @Override
    public int hashCode() {

        if (this.hashCode == 0) {
            HashCodeBuilder builder = new HashCodeBuilder(3, 5);
            builder.append(sourceId);
            builder.append(incomingArc);
            this.hashCode = builder.toHashCode();
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof DbArcId)) {
            return false;
        }

        DbArcId id = (DbArcId) object;

        return new EqualsBuilder().append(sourceId, id.sourceId)
                .append(incomingArc, id.incomingArc).isEquals();
    }
}
