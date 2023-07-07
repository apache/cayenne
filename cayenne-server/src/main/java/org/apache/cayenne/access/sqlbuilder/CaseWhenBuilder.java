package org.apache.cayenne.access.sqlbuilder;

import org.apache.cayenne.access.sqlbuilder.sqltree.CaseNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ElseNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.WhenNode;

import java.util.ArrayList;
import java.util.List;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.aliased;

class CaseWhenBuilder implements NodeBuilder {

    protected final Node root;
    protected List<Node> nodes;

    public CaseWhenBuilder() {
        this.root = new CaseNode();
        this.nodes = new ArrayList<>();
    }


    public WhenBuilder when (NodeBuilder... params) {
        for(NodeBuilder next : params) {
            WhenNode whenNode = new WhenNode();
            whenNode.addChild(next.build());
            nodes.add(whenNode);
        }
        return new WhenBuilder(this);
    }

    public CaseWhenBuilder elseResult(NodeBuilder result) {
        ElseNode elseNode = new ElseNode(result);
        elseNode.addChild(result.build());
        nodes.add(elseNode);
        return this;
    }

    public NodeBuilder as(String alias) {
        return aliased(this, alias);
    }

    @Override
    public Node build() {
        for (Node node : nodes) {
            root.addChild(node);
        }
        return root;
    }

    List<Node> getNodes() {
        return nodes;
    }
}