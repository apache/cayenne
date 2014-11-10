package org.apache.cayenne.access;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.inheritance.BaseEntity;
import org.apache.cayenne.testdo.inheritance.RelatedEntity;
import org.apache.cayenne.testdo.inheritance.SubEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(ServerCase.INHERITANCE_PROJECT)
public class EntityInheritanceIT extends ServerCase {

    @Inject
    private DataContext context;

    /**
     * Test for CAY-1008: Reverse relationships may not be correctly set if inheritance is
     * used.
     */
    @Test
    public void testCAY1008() {
        RelatedEntity related = context.newObject(RelatedEntity.class);

        BaseEntity base = context.newObject(BaseEntity.class);
        base.setToRelatedEntity(related);

        assertEquals(1, related.getBaseEntities().size());
        assertEquals(0, related.getSubEntities().size());

        SubEntity sub = context.newObject(SubEntity.class);
        sub.setToRelatedEntity(related);

        assertEquals(2, related.getBaseEntities().size());

        // TODO: andrus 2008/03/28 - this fails...
        // assertEquals(1, related.getSubEntities().size());
    }

    /**
     * Test for CAY-1009: Bogus runtime relationships can mess up commit.
     */
    @Test
    public void testCAY1009() {

        // We should have only one relationship. DirectToSubEntity -> SubEntity.

        // this fails as a result of 'EntityResolver().applyObjectLayerDefaults()'
        // creating incorrect relationships
        // assertEquals(1, context
        // .getEntityResolver()
        // .getObjEntity("DirectToSubEntity")
        // .getRelationships()
        // .size());

        // We should still just have the one mapped relationship, but we in fact now have
        // two:
        // DirectToSubEntity -> BaseEntity and DirectToSubEntity -> SubEntity.

        // TODO: andrus 2008/03/28 - this fails...
        // assertEquals(1, context.getEntityResolver().getObjEntity("DirectToSubEntity")
        // .getRelationships().size());
        //
        // DirectToSubEntity direct = context.newObject(DirectToSubEntity.class);
        //
        // SubEntity sub = context.newObject(SubEntity.class);
        // sub.setToDirectToSubEntity(direct);
        //
        // assertEquals(1, direct.getSubEntities().size());
        //
        // context.deleteObject(sub);
        // assertEquals(0, direct.getSubEntities().size());
    }

}
