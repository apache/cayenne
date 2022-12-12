package org.apache.cayenne.map;

import org.apache.cayenne.reflect.PersistentDescriptor;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class DefaultEntityResultSegmentTest {
    private final List<String> expectedColumnPath = List.of("key1", "key2");

    private final Map<String, String> fields =
            new ConcurrentHashMap<>(Map.of("key1", "value1", "key2", "value2"));

    private final DefaultEntityResultSegment resultSegment =
            new DefaultEntityResultSegment(new PersistentDescriptor(), fields, fields.size());

    @Test
    public void testGetColumnPath() {
        List<String> actualColumnPath = fields.values()
                .stream()
                .map(resultSegment::getColumnPath)
                .collect(Collectors.toList());

        assertEquals(expectedColumnPath.size(), actualColumnPath.size());

        IntStream.range(0, actualColumnPath.size())
                .forEach(i -> assertEquals(expectedColumnPath.get(i), actualColumnPath.get(i)));
    }

}