/*
 * Logback awslogs JSON encoder.
 * Copyright (C) 2023 Oliver Siegmar
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.siegmar.logbackawslogsjsonencoder;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.slf4j.event.KeyValuePair;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AwsJsonLogEncoderTest {

    private static final String LOGGER_NAME = AwsJsonLogEncoderTest.class.getCanonicalName();

    private final AwsJsonLogEncoder encoder = new AwsJsonLogEncoder();

    AwsJsonLogEncoderTest() {
        encoder.setContext(new LoggerContext());
    }

    @Test
    void defaultConfig() {
        encoder.start();

        //language=JSON5
        final String expectedJson =
            "{"
            + "timestamp: '${json-unit.any-number}',"
            + "level: 'DEBUG',"
            + "thread: 'Test worker',"
            + "logger: 'de.siegmar.logbackawslogsjsonencoder.AwsJsonLogEncoderTest',"
            + "message: 'message 1',"
            + "markers: {"
            + "    'foo': 1,"
            + "    'bar': 1"
            + "},"
            + "mdc: {"
            + "    foo: 'bar',"
            + "    baz: null"
            + "},"
            + "keyValues: {"
            + "    foo: 'bar',"
            + "    bar: null,"
            + "    'null': 'bar'"
            + "}}";
        assertThatJson(fullLog()).isEqualTo(json(expectedJson));
    }

    @Test
    void disableAll() {
        setupAllDisabledEncoder(c -> {
        });
        assertThatJson(fullLog()).isEqualTo("{}");
    }

    @Test
    void timestamp() {
        setupAllDisabledEncoder(c -> c.setIncludeTimestamp(true));
        assertThatJson(fullLog())
            .isEqualTo(json("{'timestamp': '${json-unit.any-number}'}"));
    }

    @Test
    void nanoseconds() {
        setupAllDisabledEncoder(c -> c.setIncludeNanoseconds(true));
        assertThatJson(fullLog())
            .isEqualTo(json("{'nanoseconds': '${json-unit.any-number}'}"));
    }

    @Test
    void sequenceNumber() {
        setupAllDisabledEncoder(c -> c.setIncludeSequenceNumber(true));
        assertThatJson(fullLog())
            .isEqualTo(json("{'sequenceNumber': '${json-unit.any-number}'}"));
    }

    @Test
    void levelName() {
        setupAllDisabledEncoder(c -> c.setIncludeLevelName(true));
        assertThatJson(fullLog())
            .isEqualTo(json("{'level': 'DEBUG'}"));
    }

    @Test
    void threadName() {
        setupAllDisabledEncoder(c -> c.setIncludeThreadName(true));
        assertThatJson(fullLog())
            .isEqualTo(json("{'thread': 'Test worker'}"));
    }

    @Test
    void loggerName() {
        setupAllDisabledEncoder(c -> c.setIncludeLoggerName(true));
        assertThatJson(fullLog())
            .isEqualTo(json("{'logger': 'de.siegmar.logbackawslogsjsonencoder.AwsJsonLogEncoderTest'}"));
    }

    @Test
    void formattedMessage() {
        setupAllDisabledEncoder(c -> c.setIncludeFormattedMessage(true));
        assertThatJson(fullLog())
            .isEqualTo(json("{'message': 'message 1'}"));
    }

    @Test
    void rawMessage() {
        setupAllDisabledEncoder(c -> c.setIncludeRawMessage(true));
        assertThatJson(fullLog())
            .isEqualTo(json("{'rawMessage': 'message {}'}"));
    }

    @Test
    void stacktrace() {
        setupAllDisabledEncoder(c -> c.setIncludeStacktrace(true));

        final String logMsg;
        try {
            throw new IllegalArgumentException("Example Exception");
        } catch (final IllegalArgumentException e) {
            logMsg = fullLog(e);
        }

        assertThatJson(logMsg).and(
            j -> j.node("stacktrace").isString().startsWith(
                "java.lang.IllegalArgumentException: Example Exception\n"
                + "\tat de.siegmar.logbackawslogsjsonencoder.AwsJsonLogEncoderTest.stacktrace(AwsJsonLogEncoderTest")
        );
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Test
    void rootCause() {
        setupAllDisabledEncoder(c -> c.setIncludeRootCause(true));

        final String logMsg;
        try {
            throw new RuntimeException(new IllegalArgumentException("Example Exception"));
        } catch (final RuntimeException e) {
            logMsg = fullLog(e);
        }

        //language=JSON5
        final String expectedJson =
            "{rootCause: {"
            + "class: 'java.lang.IllegalArgumentException',"
            + "message: 'Example Exception'"
            + "}}";

        assertThatJson(logMsg).isEqualTo(json(expectedJson));
    }

    @Test
    void marker() {
        setupAllDisabledEncoder(c -> c.setIncludeMarker(true));
        assertThatJson(fullLog())
            .isEqualTo(json("{'markers': {'foo': 1, 'bar': 1}}"));
    }

    @Test
    void mdc() {
        setupAllDisabledEncoder(c -> c.setIncludeMdc(true));
        assertThatJson(fullLog())
            .isEqualTo(json("{'mdc': {'foo': 'bar', 'baz': null}}"));
    }

    @Test
    void keyValues() {
        setupAllDisabledEncoder(c -> c.setIncludeKeyValues(true));
        assertThatJson(fullLog())
            .isEqualTo(json("{'keyValues': {'foo': 'bar', 'bar': null, 'null': 'bar'}}"));
    }

    @Test
    void caller() {
        setupAllDisabledEncoder(c -> c.setIncludeCaller(true));
        //language=JSON5
        final String expectedJson =
            "{caller: {"
            + "    file: 'NativeMethodAccessorImpl.java',"
            + "    line: -2,"
            + "    class: 'jdk.internal.reflect.NativeMethodAccessorImpl',"
            + "    method: 'invoke0'"
            + "}}";
        assertThatJson(fullLog())
            .isEqualTo(json(expectedJson));
    }

    @Test
    void staticFields() {
        setupAllDisabledEncoder(c -> c.addStaticField("foo:bar"));
        assertThatJson(fullLog())
            .isEqualTo(json("{'staticFields': {'foo': 'bar'}}"));
    }

    @Test
    void customMapper() {
        setupAllDisabledEncoder(c -> c.addCustomMapper(new MyCustomMapper()));
        assertThatJson(fullLog())
            .isEqualTo(json("{'custom': 'bar'}"));
    }

    @Test
    void complex() {
        setupAllEnabledEncoder(c -> c.addStaticField("foo:bar"));

        //language=JSON5
        final String expectedJson =
            "{"
            + "    sequenceNumber: 0,"
            + "    timestamp: '${json-unit.any-number}',"
            + "    nanoseconds: '${json-unit.any-number}',"
            + "    level: 'DEBUG',"
            + "    thread: 'Test worker',"
            + "    logger: 'de.siegmar.logbackawslogsjsonencoder.AwsJsonLogEncoderTest',"
            + "    message: 'message 1',"
            + "    rawMessage: 'message {}',"
            + "    staticFields: {"
            + "        foo: 'bar'"
            + "    },"
            + "    markers: {"
            + "        foo: 1,"
            + "        bar: 1"
            + "    },"
            + "    mdc: {"
            + "        foo: 'bar',"
            + "        baz: null"
            + "    },"
            + "    keyValues: {"
            + "        foo: 'bar',"
            + "        bar: null,"
            + "        'null': 'bar'"
            + "    },"
            + "    caller: {"
            + "        file: 'NativeMethodAccessorImpl.java',"
            + "        line: -2,"
            + "        class: 'jdk.internal.reflect.NativeMethodAccessorImpl',"
            + "        method: 'invoke0'"
            + "    }"
            + "}";

        assertThatJson(fullLog()).isEqualTo(json(expectedJson));
    }

    private void setupAllDisabledEncoder(final Consumer<AwsJsonLogEncoder> customize) {
        encoder.setIncludeTimestamp(false);
        encoder.setIncludeNanoseconds(false);
        encoder.setIncludeSequenceNumber(false);
        encoder.setIncludeLevelName(false);
        encoder.setIncludeThreadName(false);
        encoder.setIncludeLoggerName(false);
        encoder.setIncludeFormattedMessage(false);
        encoder.setIncludeRawMessage(false);
        encoder.setIncludeStacktrace(false);
        encoder.setIncludeRootCause(false);
        encoder.setIncludeMarker(false);
        encoder.setIncludeMdc(false);
        encoder.setIncludeKeyValues(false);
        encoder.setIncludeCaller(false);
        customize.accept(encoder);
        encoder.start();
    }

    private void setupAllEnabledEncoder(final Consumer<AwsJsonLogEncoder> customize) {
        encoder.setIncludeTimestamp(true);
        encoder.setIncludeNanoseconds(true);
        encoder.setIncludeSequenceNumber(true);
        encoder.setIncludeLevelName(true);
        encoder.setIncludeThreadName(true);
        encoder.setIncludeLoggerName(true);
        encoder.setIncludeFormattedMessage(true);
        encoder.setIncludeRawMessage(true);
        encoder.setIncludeStacktrace(true);
        encoder.setIncludeRootCause(true);
        encoder.setIncludeMarker(true);
        encoder.setIncludeMdc(true);
        encoder.setIncludeKeyValues(true);
        encoder.setIncludeCaller(true);
        customize.accept(encoder);
        encoder.start();
    }

    private String fullLog() {
        return fullLog(null);
    }

    private String fullLog(final Exception e) {
        return dummyLog(c -> {
            c.addMarker(MarkerFactory.getMarker("foo"));
            c.addMarker(MarkerFactory.getMarker("bar"));

            final Map<String, String> mdcMap = new LinkedHashMap<>();
            mdcMap.put("foo", "bar");
            mdcMap.put("baz", null);
            c.setMDCPropertyMap(mdcMap);

            c.setKeyValuePairs(List.of(
                new KeyValuePair("foo", "bar"),
                new KeyValuePair("bar", null),
                new KeyValuePair(null, "bar")
            ));
        }, e);
    }

    private String dummyLog(final Consumer<LoggingEvent> customize, final Throwable e) {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);
        final LoggingEvent event = new LoggingEvent(
            LOGGER_NAME,
            logger,
            Level.DEBUG,
            "message {}",
            e,
            new Object[]{1});

        customize.accept(event);
        return new String(encoder.encode(event), StandardCharsets.UTF_8);
    }

}
