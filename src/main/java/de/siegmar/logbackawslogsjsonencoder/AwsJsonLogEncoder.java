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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Marker;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.encoder.EncoderBase;

/**
 * Logback encoder that produces JSON that is read by CloudWatch Logs Insights.
 */
public class AwsJsonLogEncoder extends EncoderBase<ILoggingEvent> {

    private static final int INITIAL_BUF_SIZE = 128;

    private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ";
    private static final String DEFAULT_MESSAGE_PATTERN = "%m%nopex";
    private static final String DEFAULT_FULL_MESSAGE_PATTERN = "%m%n";

    /**
     * Default timestamp format is in conjunction with {@code awslogs-datetime-format}
     * of {@code %Y-%m-%dT%H:%M:%S.%f%z`}.
     */
    private String dateTimeFormat = DEFAULT_DATETIME_FORMAT;

    /**
     * DateTimeFormatter that is configured with the pattern of {@code dateTimeFormat}.
     */
    private DateTimeFormatter dateTimeFormatter;

    /**
     * If true, the raw message (with argument placeholders) will be included, too. Default: false.
     */
    private boolean includeRawMessage;

    /**
     * If true, logback markers will be included, too. Default: true.
     */
    private boolean includeMarker = true;

    /**
     * If true, MDC keys/values will be included, too. Default: true.
     */
    private boolean includeMdcData = true;

    /**
     * If true, caller data (source file-, method-, class name and line) will be included, too.
     * Default: false.
     */
    private boolean includeCallerData;

    /**
     * If true, root cause exception of the exception passed with the log message will be
     * exposed in the exception field. Default: false.
     */
    private boolean includeRootCauseData;

    /**
     * Message format for messages without an exception. Default: `"%m%nopex"`.
     */
    private PatternLayout messageLayout;

    /**
     * Message format for messages with an exception. Default: `"%m%n"`.
     */
    private PatternLayout fullMessageLayout;

    /**
     * Additional, static fields to include. Defaults: none.
     */
    private Map<String, Object> staticFields = new HashMap<>();

    public String getDateTimeFormat() {
        return dateTimeFormat;
    }

    public void setDateTimeFormat(final String dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
    }

    public boolean isIncludeRawMessage() {
        return includeRawMessage;
    }

    public void setIncludeRawMessage(final boolean includeRawMessage) {
        this.includeRawMessage = includeRawMessage;
    }

    public boolean isIncludeMarker() {
        return includeMarker;
    }

    public void setIncludeMarker(final boolean includeMarker) {
        this.includeMarker = includeMarker;
    }

    public boolean isIncludeMdcData() {
        return includeMdcData;
    }

    public void setIncludeMdcData(final boolean includeMdcData) {
        this.includeMdcData = includeMdcData;
    }

    public boolean isIncludeCallerData() {
        return includeCallerData;
    }

    public void setIncludeCallerData(final boolean includeCallerData) {
        this.includeCallerData = includeCallerData;
    }

    public boolean isIncludeRootCauseData() {
        return includeRootCauseData;
    }

    public void setIncludeRootCauseData(final boolean includeRootCauseData) {
        this.includeRootCauseData = includeRootCauseData;
    }

    public PatternLayout getMessageLayout() {
        return messageLayout;
    }

    public void setMessageLayout(final PatternLayout messageLayout) {
        this.messageLayout = messageLayout;
    }

    public PatternLayout getFullMessageLayout() {
        return fullMessageLayout;
    }

    public void setFullMessageLayout(final PatternLayout fullMessageLayout) {
        this.fullMessageLayout = fullMessageLayout;
    }

    public Map<String, Object> getStaticFields() {
        return staticFields;
    }

    public void setStaticFields(final Map<String, Object> staticFields) {
        this.staticFields = Objects.requireNonNull(staticFields);
    }

    public void addStaticField(final String staticField) {
        final String[] split = staticField.split(":", 2);
        if (split.length == 2) {
            addField(staticFields, split[0].trim(), split[1].trim());
        } else {
            addWarn("staticField must be in format key:value - rejecting '" + staticField + "'");
        }
    }

    private void addField(final Map<String, Object> dst, final String key, final String value) {
        if (key.isEmpty()) {
            addWarn("staticField key must not be empty");
        } else if (dst.containsKey(key)) {
            addWarn("additional field with key '" + key + "' is already set");
        } else {
            dst.put(key, value);
        }
    }

    @Override
    public void start() {
        dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat)
            .withZone(ZoneId.systemDefault());

        if (messageLayout == null) {
            messageLayout = buildPattern(DEFAULT_MESSAGE_PATTERN);
        }
        if (fullMessageLayout == null) {
            fullMessageLayout = buildPattern(DEFAULT_FULL_MESSAGE_PATTERN);
        }

        super.start();
    }

    private PatternLayout buildPattern(final String pattern) {
        final PatternLayout patternLayout = new PatternLayout();
        patternLayout.setContext(getContext());
        patternLayout.setPattern(pattern);
        patternLayout.start();
        return patternLayout;
    }

    @Override
    public byte[] encode(final ILoggingEvent event) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(INITIAL_BUF_SIZE);

        try (Writer appendable = new OutputStreamWriter(bos, StandardCharsets.UTF_8)) {
            try (SimpleJsonEncoder json = new SimpleJsonEncoder(appendable)) {
                final Instant timestamp = Instant.ofEpochMilli(event.getTimeStamp());

                json.appendToJSON("timestamp", dateTimeFormatter.format(timestamp))
                    .appendToJSON("level", event.getLevel().toString())
                    .appendToJSON("logger", event.getLoggerName())
                    .appendToJSON("thread", event.getThreadName())
                    .appendToJSON("message", messageLayout.doLayout(event));

                if (event.getThrowableProxy() != null) {
                    json.appendToJSON("full_message", fullMessageLayout.doLayout(event));
                }

                if (includeRawMessage) {
                    json.appendToJSON("raw_message", event.getMessage());
                }

                if (includeMarker) {
                    final Marker marker = event.getMarker();
                    if (marker != null) {
                        json.appendToJSON("marker", marker.getName());
                    }
                }

                if (includeMdcData) {
                    buildMdcData(event.getMDCPropertyMap())
                        .ifPresent(mdc -> json.appendToJSON("mdc", mdc));
                }

                if (includeCallerData) {
                    buildCallerData(event.getCallerData())
                        .ifPresent(cd -> json.appendToJSON("caller_data", cd));
                }

                if (includeRootCauseData) {
                    buildRootExceptionData(event.getThrowableProxy())
                        .ifPresent(red -> json.appendToJSON("root_exception_data", red));
                }

                if (!staticFields.isEmpty()) {
                    json.appendToJSON("static_fields", staticFields);
                }
            }

            appendable.append(System.lineSeparator());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return bos.toByteArray();
    }

    private Optional<Map<String, Object>> buildMdcData(final Map<String, String> mdcProperties) {
        if (mdcProperties == null || mdcProperties.isEmpty()) {
            return Optional.empty();
        }

        final Map<String, Object> additionalFields = new HashMap<>();
        for (final Map.Entry<String, String> entry : mdcProperties.entrySet()) {
            addField(additionalFields, entry.getKey(), entry.getValue());
        }

        return Optional.of(additionalFields);
    }

    private Optional<Map<String, Object>> buildCallerData(final StackTraceElement[] callerData) {
        if (callerData == null || callerData.length == 0) {
            return Optional.empty();
        }

        final StackTraceElement first = callerData[0];

        final Map<String, Object> callerDataMap = new HashMap<>(4);
        callerDataMap.put("source_file_name", first.getFileName());
        callerDataMap.put("source_method_name", first.getMethodName());
        callerDataMap.put("source_class_name", first.getClassName());
        callerDataMap.put("source_line_number", first.getLineNumber());

        return Optional.of(callerDataMap);
    }

    private Optional<Map<String, Object>> buildRootExceptionData(final IThrowableProxy throwableProxy) {
        final IThrowableProxy rootException = getRootException(throwableProxy);
        if (rootException == null) {
            return Optional.empty();
        }

        final Map<String, Object> exceptionDataMap = new HashMap<>(2);
        exceptionDataMap.put("root_cause_class_name", rootException.getClassName());
        exceptionDataMap.put("root_cause_message", rootException.getMessage());

        return Optional.of(exceptionDataMap);
    }

    private IThrowableProxy getRootException(final IThrowableProxy throwableProxy) {
        if (throwableProxy == null) {
            return null;
        }

        IThrowableProxy rootCause = throwableProxy;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        return rootCause;
    }

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

}
