package org.apache.cayenne.access;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
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

    private ObjectId sourceId;
    private DbRelationship incomingArc;

    private DbEntity entity;

    DbArcId(ObjectId sourceId, DbRelationship incomingArc) {
        this.sourceId = sourceId;
        this.incomingArc = incomingArc;
    }

    DbEntity getEntity() {
        if (entity == null) {
            entity = (DbEntity) incomingArc.getTargetEntity();
        }

        return entity;
    }

    ObjectId getSourceId() {
        return sourceId;
    }

    DbRelationship getIncominArc() {
        return incomingArc;
    }

    @Override
    public int hashCode() {

        if (this.hashCode == 0) {
            HashCodeBuilder builder = new HashCodeBuilder(3, 5);
            builder.append(sourceId);
            builder.append(incomingArc.getName());
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
                .append(incomingArc.getName(), id.incomingArc.getName())
                .isEquals();
    }
}
