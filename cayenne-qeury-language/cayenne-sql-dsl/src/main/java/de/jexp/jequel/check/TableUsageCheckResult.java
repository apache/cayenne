package de.jexp.jequel.check;

import java.util.Collection;
import java.util.HashSet;

public class TableUsageCheckResult {
    private final Collection<String> unusedTables = new HashSet<String>(5);
    private final Collection<String> missingTables = new HashSet<String>(5);
    private final Collection<String> usedTables = new HashSet<String>(5);

    public TableUsageCheckResult(final Collection<String> unusedTables) {
        this.unusedTables.addAll(unusedTables);
    }

    public void addUsedTable(final String name) {
        if (usedTables.contains(name) || missingTables.contains(name)) return;
        if (unusedTables.remove(name)) usedTables.add(name);
        else missingTables.add(name);
    }

    public boolean isValid() {
        return unusedTables.isEmpty() && missingTables.isEmpty();
    }

    public Collection<String> getUnusedTables() {
        return unusedTables;
    }

    public Collection<String> getMissingTables() {
        return missingTables;
    }

    public Collection<String> getUsedTables() {
        return usedTables;
    }

    public void addUsedTables(final Iterable<String> usedTables) {
        for (final String usedTable : usedTables) {
            addUsedTable(usedTable);
        }
    }

    public String toString() {
        if (isValid()) return "Used Tables: " + usedTables;
        return "Error: Unused Tables: " + unusedTables + " Missing Tables: " + missingTables + " Used Tables: " + usedTables;
    }
}
