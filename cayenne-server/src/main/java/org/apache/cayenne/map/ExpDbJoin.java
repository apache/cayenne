package org.apache.cayenne.map;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.util.ToStringBuilder;
import org.apache.cayenne.util.XMLEncoder;

public class ExpDbJoin extends DbJoin {

    protected Expression joinExpression;

    public ExpDbJoin() {
        super();
    }

    public ExpDbJoin(DbRelationship dbRelationship) {
        super(dbRelationship);
    }

    public ExpDbJoin(DbRelationship relationship, Expression joinExpression) {
        super(relationship);

        this.joinExpression = joinExpression;
    }

    @Override
    public ExpDbJoin createReverseJoin() {
        return this;
    }

    public void setJoinExpression(Expression joinExpression) {
        this.joinExpression = joinExpression;
    }

    public Expression getJoinExpression() {
        return joinExpression;
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("db-join")
                .attribute("joinExp", getJoinExpression().toString())
                .end();
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("expression", getJoinExpression().toString());
        return builder.toString();
    }
}
