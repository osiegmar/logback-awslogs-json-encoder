/*
 * Logback awslogs JSON encoder.
 * Copyright (C) 2018 Oliver Siegmar
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

class AwsJsonLogEncoderTest {

    private static final String LOGGER_NAME = AwsJsonLogEncoderTest.class.getCanonicalName();

    private final AwsJsonLogEncoder encoder = new AwsJsonLogEncoder();

    AwsJsonLogEncoderTest() {
        encoder.setContext(new LoggerContext());
    }

    @Test
    void simple() {
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = produce(simpleLoggingEvent(logger, null));
        basicValidation(logMsg);
    }

    private String produce(final ILoggingEvent event) {
        return new String(encoder.encode(event), StandardCharsets.UTF_8);
    }

    @Test
    void nestedExceptionShouldNotFail() {
        encoder.setIncludeRootCauseData(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = produce(simpleLoggingEvent(logger,
            new IOException(new IOException(new IOException()))));

        assertNotNull(logMsg);
    }

    private LoggingEvent simpleLoggingEvent(final Logger logger, final Throwable e) {
        return new LoggingEvent(
            LOGGER_NAME,
            logger,
            Level.DEBUG,
            "message {}",
            e,
            new Object[]{1});
    }

    private void basicValidation(final String jsonNode) {
        assertThatJson(jsonNode).and(
            j -> j.node("timestamp").isNotNull(),
            j -> j.node("level").isEqualTo("DEBUG"),
            j -> j.node("logger").isEqualTo(LOGGER_NAME),
            j -> j.node("thread").isNotNull(),
            j -> j.node("message").isEqualTo("message 1")
        );
    }

    @Test
    void exception() {
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg;
        try {
            throw new IllegalArgumentException("Example Exception");
        } catch (final IllegalArgumentException e) {
            logMsg = produce(new LoggingEvent(
                LOGGER_NAME,
                logger,
                Level.DEBUG,
                "message {}",
                e,
                new Object[]{1}));
        }

        basicValidation(logMsg);

        assertThatJson(logMsg).and(
            j -> j.node("full_message").isString().startsWith(
                "message 1\n"
                + "java.lang.IllegalArgumentException: Example Exception\n"
                + "\tat de.siegmar.logbackawslogsjsonencoder.AwsJsonLogEncoderTest"
                + ".exception(AwsJsonLogEncoderTest.java:")
        );
    }

    @Test
    void complex() {
        encoder.setIncludeRawMessage(true);
        encoder.addStaticField("foo:bar");
        encoder.setIncludeCallerData(true);
        encoder.setIncludeRootCauseData(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final LoggingEvent event = simpleLoggingEvent(logger, null);

        final Map<String, String> mdcMap = new HashMap<>();
        mdcMap.put("mdc_key", "mdc_value");
        mdcMap.put("mdc_key_nullvalue", null);
        event.setMDCPropertyMap(mdcMap);

        final String logMsg = produce(event);

        basicValidation(logMsg);

        assertThatJson(logMsg).and(
            j -> j.node("level").isEqualTo("DEBUG"),
            j -> j.node("static_fields.foo").isEqualTo("bar"),
            j -> j.node("mdc.mdc_key").isEqualTo("mdc_value"),
            j -> j.node("raw_message").isEqualTo("message {}"),
            j -> j.node("_exception").isAbsent()
        );
    }

    @Test
    void rootExceptionTurnedOff() {
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg;
        try {
            throw new IOException("Example Exception");
        } catch (final IOException e) {
            logMsg = produce(simpleLoggingEvent(logger, e));
        }

        assertThatJson(logMsg).node("exception").isAbsent();
    }

    @Test
    void noRootException() {
        encoder.setIncludeRootCauseData(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = produce(simpleLoggingEvent(logger, null));

        assertThatJson(logMsg).node("exception").isAbsent();
    }

    @Test
    void rootExceptionWithoutCause() {
        encoder.setIncludeRootCauseData(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg;
        try {
            throw new IOException("Example Exception");
        } catch (final IOException e) {
            logMsg = produce(simpleLoggingEvent(logger, e));
        }

        basicValidation(logMsg);

        assertThatJson(logMsg).node("root_exception_data").and(
            j -> j.node("root_cause_class_name").isEqualTo("java.io.IOException"),
            j -> j.node("root_cause_message").isEqualTo("Example Exception")
        );
    }

    @Test
    void rootExceptionWithCause() {
        encoder.setIncludeRootCauseData(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg;
        try {
            throw new IOException("Example Exception",
                new IllegalStateException("Example Exception 2"));
        } catch (final IOException e) {
            logMsg = produce(simpleLoggingEvent(logger, e));
        }

        basicValidation(logMsg);

        assertThatJson(logMsg).node("root_exception_data").and(
            j -> j.node("root_cause_class_name").isEqualTo("java.lang.IllegalStateException"),
            j -> j.node("root_cause_message").isEqualTo("Example Exception 2")
        );
    }

}
