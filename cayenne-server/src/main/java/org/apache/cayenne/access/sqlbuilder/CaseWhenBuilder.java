package org.apache.cayenne.access.sqlbuilder;

import org.apache.cayenne.access.sqlbuilder.sqltree.CaseNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ElseNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.WhenNode;

import java.util.ArrayList;
import java.util.List;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.aliased;

class CaseWhenBuilder implements NodeBuilder {

    private final Node root;
    private final List<NodeBuilder> nodeBuilders;
    private NodeBuilder elseBuilder;

    public CaseWhenBuilder() {
        this.root = new CaseNode();
        this.nodeBuilders = new ArrayList<>();
    }

    public WhenBuilder when(NodeBuilder param) {
        nodeBuilders.add(() -> {
            WhenNode whenNode = new WhenNode();
            whenNode.addChild(param.build());
            return whenNode;
        });
        return new WhenBuilder(this);
    }

    public CaseWhenBuilder elseResult(NodeBuilder result) {
        elseBuilder = () -> {
            ElseNode elseNode = new ElseNode();
            elseNode.addChild(result.build());
            return elseNode;
        };
        return this;
    }

    public NodeBuilder as(String alias) {
        return aliased(this, alias);
    }

    @Override
    public Node build() {
        for (NodeBuilder builder : nodeBuilders) {
            root.addChild(builder.build());
        }
        if (elseBuilder != null){
            root.addChild(elseBuilder.build());
        }
        return root;
    }

    List<NodeBuilder> getNodeBuilders() {
        return nodeBuilders;
    }
}