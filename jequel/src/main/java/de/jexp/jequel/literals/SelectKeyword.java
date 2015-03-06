package de.jexp.jequel.literals;

public enum SelectKeyword implements SqlKeyword {
    SELECT,
    FROM,
    WHERE,
    ORDER_BY,
    HAVING,
    GROUP_BY;

    public String getSqlKeyword() {
        return null;
    }
}
