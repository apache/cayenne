package org.apache.cayenne.unit.jira;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.inheritance_flat.Group;
import org.apache.cayenne.testdo.inheritance_flat.Role;
import org.apache.cayenne.testdo.inheritance_flat.User;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class CAY_1379Test extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack("InheritanceFlatStack");
    }

    public void testGroupActions() throws Exception {

        DataContext context = createDataContext();

        User user = context.newObject(User.class);
        user.setName("test_user");

        Group group1 = context.newObject(Group.class);
        group1.setName("test_group1");

        Group group2 = context.newObject(Group.class);
        group2.setName("test_group2");

        group1.addToGroupMembers(user);
        group2.addToGroupMembers(group1);

        group2.getObjectContext().commitChanges();

        // Per CAY-1379 removing user and then refetching resulted in a FFE downstream
        group1.removeFromGroupMembers(user);
        Expression exp = ExpressionFactory.matchExp(Role.ROLE_GROUPS_PROPERTY, group2);
        SelectQuery query = new SelectQuery(Group.class, exp);
        context.performQuery(query);
        context.commitChanges();

        context.deleteObject(group1);
        context.deleteObject(group2);
        context.deleteObject(user);
        context.commitChanges();
    }
}
