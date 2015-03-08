package de.jexp.jequel.literals;

public enum SelectKeyword implements SqlKeyword {
    SELECT,
    FROM,
    WHERE,
    ORDER_BY("orderBy"),
    HAVING,
    GROUP_BY("groupBy");

    private final String name;

    SelectKeyword() {
        this(null);
    }
    SelectKeyword(String name) {
        this.name = name;
    }


    public String getSqlKeyword() {
        return null;
    }

    public String javaName() {
        if (this.name != null) {
            return name;
        }

        return this.toString().toLowerCase();
    }
}
