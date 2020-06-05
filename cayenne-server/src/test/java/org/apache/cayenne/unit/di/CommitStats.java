package org.apache.cayenne.unit.di;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.DataChannelSyncFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.graph.GraphDiff;
import org.junit.rules.ExternalResource;

import java.util.function.Supplier;

public class CommitStats implements DataChannelSyncFilter {

    private int commitCount;
    private Supplier<DataDomain> dataDomain;

    public CommitStats(Supplier<DataDomain> dataDomain) {
        this.dataDomain = dataDomain;
    }

    public void before() {
        dataDomain.get().addSyncFilter(this);
        commitCount = 0;
    }

    public void after() {
        dataDomain.get().removeSyncFilter(this);
    }

    @Override
    public GraphDiff onSync(
            ObjectContext originatingContext,
            GraphDiff changes,
            int syncType,
            DataChannelSyncFilterChain filterChain) {

        switch (syncType) {
            case DataChannel.FLUSH_CASCADE_SYNC:
                commitCount++;
                break;
        }

        return filterChain.onSync(originatingContext, changes, syncType);
    }

    public int getCommitCount() {
        return commitCount;
    }
}
