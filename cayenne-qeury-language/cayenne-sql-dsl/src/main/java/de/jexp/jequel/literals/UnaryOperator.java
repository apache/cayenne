package de.jexp.jequel.literals;

public enum UnaryOperator implements SqlKeyword {
    /* boolean result */
    NOT,
    EXISTS,
    NOT_EXISTS,

    /* aggregation numeric functions */
    SUM,
    COUNT,
    AVG,

    /* aggregation ordering functions */
    MIN,
    MAX,

    /* numeric functions */
    ROUND,
    TO_NUMBER;

    public String getSqlKeyword() {
        return null;
    }
}