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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.LineReader;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

public class AwsJsonLogEncoderTest {

    private static final String LOGGER_NAME = AwsJsonLogEncoderTest.class.getCanonicalName();

    private AwsJsonLogEncoder encoder = new AwsJsonLogEncoder();

    @BeforeEach
    public void before() {
        encoder.setContext(new LoggerContext());
    }

    @Test
    public void simple() throws IOException {
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = produce(simpleLoggingEvent(logger, null));

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);

        final LineReader msg =
            new LineReader(new StringReader(jsonNode.get("message").textValue()));
        assertEquals("message 1", msg.readLine());
    }

    private String produce(final ILoggingEvent event) {
        return new String(encoder.encode(event), StandardCharsets.UTF_8);
    }

    @Test
    public void nestedExceptionShouldNotFail() {
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

    private void basicValidation(final JsonNode jsonNode) {
        assertNotNull(jsonNode.get("timestamp").textValue());
        assertEquals("DEBUG", jsonNode.get("level").textValue());
        assertEquals(LOGGER_NAME, jsonNode.get("logger").textValue());
        assertNotNull(jsonNode.get("thread").textValue());
        assertEquals("message 1", jsonNode.get("message").textValue());
    }

    @Test
    public void exception() throws IOException {
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

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);

        final LineReader msg =
            new LineReader(new StringReader(jsonNode.get("full_message").textValue()));

        assertEquals("message 1", msg.readLine());
        assertEquals("java.lang.IllegalArgumentException: Example Exception", msg.readLine());
        final String line = msg.readLine();
        assertTrue(line.matches(
            "^\tat de.siegmar.logbackawslogsjsonencoder.AwsJsonLogEncoderTest.exception"
                + "\\(AwsJsonLogEncoderTest.java:\\d+\\)$"), () -> "Unexpected line: " + line);
    }

    @Test
    public void complex() throws IOException {
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

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);
        assertEquals("DEBUG", jsonNode.get("level").textValue());
        assertEquals("bar", jsonNode.get("static_fields").get("foo").textValue());
        assertEquals("mdc_value", jsonNode.get("mdc").get("mdc_key").textValue());
        assertEquals("message {}", jsonNode.get("raw_message").textValue());
        assertNull(jsonNode.get("_exception"));
    }

    @Test
    public void rootExceptionTurnedOff() throws IOException {
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg;
        try {
            throw new IOException("Example Exception");
        } catch (final IOException e) {
            logMsg = produce(simpleLoggingEvent(logger, e));
        }

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);

        assertFalse(jsonNode.has("exception"));
    }

    @Test
    public void noRootException() throws IOException {
        encoder.setIncludeRootCauseData(true);
        encoder.start();

        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(LOGGER_NAME);

        final String logMsg = produce(simpleLoggingEvent(logger, null));

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);

        assertFalse(jsonNode.has("exception"));
    }

    @Test
    public void rootExceptionWithoutCause() throws IOException {
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

        final ObjectMapper om = new ObjectMapper();

        final JsonNode jsonNode = om.readTree(logMsg);

        final JsonNode exceptionData = jsonNode.get("root_exception_data");
        assertEquals("java.io.IOException", exceptionData.get("root_cause_class_name").textValue());
        assertEquals("Example Exception", exceptionData.get("root_cause_message").textValue());
    }

    @Test
    public void rootExceptionWithCause() throws IOException {
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

        final ObjectMapper om = new ObjectMapper();
        final JsonNode jsonNode = om.readTree(logMsg);
        basicValidation(jsonNode);

        final JsonNode exceptionData = jsonNode.get("root_exception_data");

        assertEquals("java.lang.IllegalStateException",
            exceptionData.get("root_cause_class_name").textValue());

        assertEquals("Example Exception 2",
            exceptionData.get("root_cause_message").textValue());
    }

}
