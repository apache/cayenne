/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.access.types;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLExec;
import org.apache.cayenne.testdo.datetime.LocalDateTestEntity;
import org.apache.cayenne.testdo.datetime.LocalDateTimeTestEntity;
import org.apache.cayenne.testdo.datetime.LocalTimeTestEntity;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.unit.dba.TestDbAdapter;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that java.time values are stored and read as wall-clock values regardless of the JVM default time zone.
 * Every case is checked against the DB's own string representation of the column (not just a Cayenne round trip),
 * and against a SQL literal inserted without JDBC parameter binding, so that a symmetric shift on write and read
 * can't hide.
 */
public class DateTimeTypesTimeZoneIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.DATE_TIME_PROJECT);

    private static final String DEFAULT_ZONE = TimeZone.getDefault().getID();

    /**
     * Zones with distinct failure modes: no offset; US DST gap at 02:00 on 2021-03-14 and overlap at 01:00-02:00 on
     * 2021-11-07; a half-hour offset; a +14 offset that pushes UTC-based date math to the next day; a southern
     * hemisphere zone whose DST gap fell on midnight (2018-11-04 00:00 didn't exist). Zones are switched in-process,
     * so drivers that cache the JVM default zone at connect time see a JVM zone different from their session zone.
     * Override with -Dcayenne.test.timezones=Z1,Z2 (paired with -Duser.timezone) to test a single zone in a JVM
     * started in that zone.
     */
    private static final List<String> ZONES = zones();

    private static List<String> zones() {
        String override = System.getProperty("cayenne.test.timezones");
        if (override != null) {
            return List.of(override.split(","));
        }

        // the JVM zone goes first: it is the only zone that connections opened before the switch agree with
        return Stream.concat(
                        Stream.of(DEFAULT_ZONE),
                        Stream.of("UTC", "America/New_York", "Asia/Kolkata", "Pacific/Kiritimati", "America/Sao_Paulo"))
                .distinct()
                .toList();
    }

    private static final List<LocalDateTime> DATE_TIMES = List.of(
            LocalDateTime.of(2021, 3, 14, 2, 35, 0),
            LocalDateTime.of(2021, 11, 7, 1, 30, 0),
            LocalDateTime.of(2018, 11, 4, 0, 30, 0),
            LocalDateTime.of(2021, 6, 15, 13, 45, 30),
            LocalDateTime.of(1999, 12, 31, 23, 59, 59));

    private static final List<LocalDate> DATES = List.of(
            LocalDate.of(2021, 3, 14),
            LocalDate.of(2018, 11, 4),
            LocalDate.of(1970, 1, 1),
            LocalDate.of(2000, 2, 29),
            LocalDate.of(2021, 12, 31));

    private static final List<LocalTime> TIMES = List.of(
            LocalTime.of(0, 0, 0),
            LocalTime.of(2, 35, 0),
            LocalTime.of(12, 0, 0),
            LocalTime.of(23, 59, 59),
            LocalTime.of(13, 45, 30, 123_000_000));

    private static final DateTimeFormatter DATE_TIME_LITERAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_LITERAL = DateTimeFormatter.ofPattern("HH:mm:ss[.SSS]");

    /**
     * Parses the driver's string form of a timestamp column: any fraction length, and an optional zone offset that
     * PostgreSQL appends for "timestamp with time zone" (the offset is ignored; the wall-clock part is compared).
     */
    private static final DateTimeFormatter DB_TIMESTAMP = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalStart().appendPattern("xxx").optionalEnd()
            .optionalStart().appendPattern("x").optionalEnd()
            .toFormatter();

    private DataContext context;

    @BeforeEach
    public void before() throws SQLException {
        context = env.context();
        env.table("LOCAL_DATE_TEST").deleteAll();
        env.table("LOCAL_DATETIME_TEST").deleteAll();
        env.table("LOCAL_TIME_TEST").deleteAll();
    }

    public static Stream<Arguments> dateTimeCases() {
        return cases(DATE_TIMES);
    }

    public static Stream<Arguments> dateCases() {
        return cases(DATES);
    }

    public static Stream<Arguments> timeCases() {
        return cases(TIMES);
    }

    private static Stream<Arguments> cases(List<?> values) {
        return ZONES.stream().flatMap(z -> values.stream().map(v -> Arguments.of(z, v)));
    }

    // ---- LocalDateTime

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("dateTimeCases")
    public void localDateTime_write(String zone, LocalDateTime value) throws Exception {
        assumeRepresentable(zone, value);
        inZone(zone, () -> {
            LocalDateTimeTestEntity o = context.newObject(LocalDateTimeTestEntity.class);
            o.setTimestamp(value);
            context.commitChanges();

            String stored = dbString("select TimestampField from LOCAL_DATETIME_TEST");
            assertEquals(value, LocalDateTime.from(DB_TIMESTAMP.parse(stored)), "DB value: " + stored);
        });
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("dateTimeCases")
    public void localDateTime_read(String zone, LocalDateTime value) throws Exception {
        assumeRepresentable(zone, value);
        inZone(zone, () -> {
            insertLiteral("LOCAL_DATETIME_TEST", "TimestampField",
                    env.testDbAdapter().timestampLiteral(DATE_TIME_LITERAL.format(value)));

            LocalDateTimeTestEntity o = ObjectSelect.query(LocalDateTimeTestEntity.class).selectOne(context);
            assertEquals(value, o.getTimestamp());

            LocalDateTime scalar = ObjectSelect.query(LocalDateTimeTestEntity.class)
                    .column(LocalDateTimeTestEntity.TIMESTAMP)
                    .selectOne(context);
            assertEquals(value, scalar);
        });
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("dateTimeCases")
    public void localDateTime_whereBinding(String zone, LocalDateTime value) throws Exception {
        assumeRepresentable(zone, value);
        inZone(zone, () -> {
            insertLiteral("LOCAL_DATETIME_TEST", "TimestampField",
                    env.testDbAdapter().timestampLiteral(DATE_TIME_LITERAL.format(value)));

            LocalDateTimeTestEntity o = ObjectSelect.query(LocalDateTimeTestEntity.class)
                    .where(LocalDateTimeTestEntity.TIMESTAMP.eq(value))
                    .selectOne(context);
            assertNotNull(o, "no row matched the bound LocalDateTime");
        });
    }

    // ---- LocalDate

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("dateCases")
    public void localDate_write(String zone, LocalDate value) throws Exception {
        inZone(zone, () -> {
            LocalDateTestEntity o = context.newObject(LocalDateTestEntity.class);
            o.setDate(value);
            context.commitChanges();

            String stored = dbString("select DateField from LOCAL_DATE_TEST");
            assertEquals(value, parseDbDate(stored), "DB value: " + stored);
        });
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("dateCases")
    public void localDate_read(String zone, LocalDate value) throws Exception {
        inZone(zone, () -> {
            insertLiteral("LOCAL_DATE_TEST", "DateField", env.testDbAdapter().dateLiteral(value.toString()));

            LocalDateTestEntity o = ObjectSelect.query(LocalDateTestEntity.class).selectOne(context);
            assertEquals(value, o.getDate());

            LocalDate scalar = ObjectSelect.query(LocalDateTestEntity.class)
                    .column(LocalDateTestEntity.DATE)
                    .selectOne(context);
            assertEquals(value, scalar);
        });
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("dateCases")
    public void localDate_whereBinding(String zone, LocalDate value) throws Exception {
        inZone(zone, () -> {
            insertLiteral("LOCAL_DATE_TEST", "DateField", env.testDbAdapter().dateLiteral(value.toString()));

            LocalDateTestEntity o = ObjectSelect.query(LocalDateTestEntity.class)
                    .where(LocalDateTestEntity.DATE.eq(value))
                    .selectOne(context);
            assertNotNull(o, "no row matched the bound LocalDate");
        });
    }

    // ---- LocalTime

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("timeCases")
    public void localTime_write(String zone, LocalTime value) throws Exception {
        inZone(zone, () -> {
            LocalTimeTestEntity o = context.newObject(LocalTimeTestEntity.class);
            o.setTime(value);
            context.commitChanges();

            String stored = dbString("select TimeField from LOCAL_TIME_TEST");
            assertEquals(expectedTime(value), parseDbTime(stored), "DB value: " + stored);
        });
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("timeCases")
    public void localTime_read(String zone, LocalTime value) throws Exception {
        inZone(zone, () -> {
            insertLiteral("LOCAL_TIME_TEST", "TimeField", env.testDbAdapter().timeLiteral(timeLiteral(value)));

            LocalTimeTestEntity o = ObjectSelect.query(LocalTimeTestEntity.class).selectOne(context);
            assertEquals(expectedTime(value), o.getTime());

            LocalTime scalar = ObjectSelect.query(LocalTimeTestEntity.class)
                    .column(LocalTimeTestEntity.TIME)
                    .selectOne(context);
            assertEquals(expectedTime(value), scalar);
        });
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("timeCases")
    public void localTime_whereBinding(String zone, LocalTime value) throws Exception {
        inZone(zone, () -> {
            insertLiteral("LOCAL_TIME_TEST", "TimeField", env.testDbAdapter().timeLiteral(timeLiteral(value)));

            LocalTimeTestEntity o = ObjectSelect.query(LocalTimeTestEntity.class)
                    .where(LocalTimeTestEntity.TIME.eq(expectedTime(value)))
                    .selectOne(context);
            assertNotNull(o, "no row matched the bound LocalTime");
        });
    }

    // ---- helpers

    /**
     * Skips the cases that the DB can't satisfy by design (see {@link TestDbAdapter#timestampIsInstant()} and
     * {@link TestDbAdapter#supportsDstGapTimestamps()}).
     */
    private static void assumeRepresentable(String zone, LocalDateTime value) {
        TestDbAdapter adapter = env.testDbAdapter();

        // an instant column is interpreted in the session zone, fixed at connect time, so only the JVM zone applies
        Assumptions.assumeTrue(!adapter.timestampIsInstant() || zone.equals(DEFAULT_ZONE),
                "TIMESTAMP is an instant on this DB, zone switched after connect is not applicable");

        boolean inGap = ZoneId.of(zone).getRules().getValidOffsets(value).isEmpty();
        Assumptions.assumeTrue(!inGap || adapter.supportsDstGapTimestamps(),
                "DST gap value not representable on this DB");
    }

    private LocalTime expectedTime(LocalTime value) {
        return env.testDbAdapter().supportsPreciseTime() ? value : value.truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * Seconds fraction is included only when non-zero and supported by the DB: Derby rejects a fraction in TIME
     * literals.
     */
    private String timeLiteral(LocalTime value) {
        LocalTime expected = expectedTime(value);
        return expected.getNano() == 0
                ? DateTimeFormatter.ISO_LOCAL_TIME.format(expected.withNano(0))
                : TIME_LITERAL.format(expected);
    }

    /**
     * Oracle has no DATE-only or TIME-only types, so both come back as a full timestamp string.
     */
    private static LocalDate parseDbDate(String stored) {
        return stored.length() > 10
                ? LocalDateTime.from(DB_TIMESTAMP.parse(stored)).toLocalDate()
                : LocalDate.parse(stored);
    }

    private static LocalTime parseDbTime(String stored) {
        return stored.indexOf('-') > 0
                ? LocalDateTime.from(DB_TIMESTAMP.parse(stored)).toLocalTime()
                : LocalTime.parse(stored);
    }

    private void insertLiteral(String table, String column, String literal) {
        SQLExec.query("insert into " + table + " (ID, " + column + ") values (1, " + literal + ")").execute(context);
    }

    /**
     * Reads the single value of a query as the driver's own string form, bypassing all Cayenne type conversion.
     */
    private String dbString(String sql) throws SQLException {
        try (Connection c = env.dataNode().getDataSource().getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            String s = rs.getString(1);
            // HSQLDB does not zero-pad the hour in the TIME string form
            return s.indexOf(':') == 1 ? "0" + s : s;
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static void inZone(String zone, ThrowingRunnable r) throws Exception {
        TimeZone defaultTz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        try {
            r.run();
        } finally {
            TimeZone.setDefault(defaultTz);
        }
    }
}
