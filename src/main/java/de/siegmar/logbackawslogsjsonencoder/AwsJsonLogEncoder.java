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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.encoder.EncoderBase;

/**
 * Logback encoder that produces JSON that is read by CloudWatch Logs Insights.
 */
public class AwsJsonLogEncoder extends EncoderBase<ILoggingEvent> {

    private static final int INITIAL_BUFFER_SIZE = 256;

    private final Map<String, Object> staticFields = new LinkedHashMap<>();
    private final List<BiConsumer<SimpleJsonEncoder, ILoggingEvent>> mappers = new ArrayList<>();
    private final List<BiConsumer<SimpleJsonEncoder, ILoggingEvent>> customMappers = new ArrayList<>();

    private boolean includeTimestamp = true;
    private boolean includeNanoseconds;
    private boolean includeSequenceNumber;
    private boolean includeLevelName = true;
    private boolean includeThreadName = true;
    private boolean includeLoggerName = true;
    private boolean includeFormattedMessage = true;
    private boolean includeRawMessage;
    private boolean includeStacktrace = true;
    private boolean includeRootCause;
    private boolean includeMarker = true;
    private boolean includeMdc = true;
    private boolean includeKeyValues = true;
    private boolean includeCaller;

    public Map<String, Object> getStaticFields() {
        return staticFields;
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public void addStaticField(final String staticField) {
        final String[] split = staticField.split(":", 2);
        if (split.length == 2) {
            addField(staticFields, split[0].trim(), split[1].trim());
        } else {
            addWarn("staticField must be in format key:value - rejecting '" + staticField + "'");
        }
    }

    public List<BiConsumer<SimpleJsonEncoder, ILoggingEvent>> getCustomMappers() {
        return customMappers;
    }

    public void addCustomMapper(final BiConsumer<SimpleJsonEncoder, ILoggingEvent> customMapper) {
        customMappers.add(customMapper);
    }

    public boolean isIncludeTimestamp() {
        return includeTimestamp;
    }

    public void setIncludeTimestamp(final boolean includeTimestamp) {
        this.includeTimestamp = includeTimestamp;
    }

    public boolean isIncludeNanoseconds() {
        return includeNanoseconds;
    }

    public void setIncludeNanoseconds(final boolean includeNanoseconds) {
        this.includeNanoseconds = includeNanoseconds;
    }

    public boolean isIncludeSequenceNumber() {
        return includeSequenceNumber;
    }

    public void setIncludeSequenceNumber(final boolean includeSequenceNumber) {
        this.includeSequenceNumber = includeSequenceNumber;
    }

    public boolean isIncludeLevelName() {
        return includeLevelName;
    }

    public void setIncludeLevelName(final boolean includeLevelName) {
        this.includeLevelName = includeLevelName;
    }

    public boolean isIncludeThreadName() {
        return includeThreadName;
    }

    public void setIncludeThreadName(final boolean includeThreadName) {
        this.includeThreadName = includeThreadName;
    }

    public boolean isIncludeLoggerName() {
        return includeLoggerName;
    }

    public void setIncludeLoggerName(final boolean includeLoggerName) {
        this.includeLoggerName = includeLoggerName;
    }

    public boolean isIncludeFormattedMessage() {
        return includeFormattedMessage;
    }

    public void setIncludeFormattedMessage(final boolean includeFormattedMessage) {
        this.includeFormattedMessage = includeFormattedMessage;
    }

    public boolean isIncludeRawMessage() {
        return includeRawMessage;
    }

    public void setIncludeRawMessage(final boolean includeRawMessage) {
        this.includeRawMessage = includeRawMessage;
    }

    public boolean isIncludeStacktrace() {
        return includeStacktrace;
    }

    public void setIncludeStacktrace(final boolean includeStacktrace) {
        this.includeStacktrace = includeStacktrace;
    }

    public boolean isIncludeRootCause() {
        return includeRootCause;
    }

    public void setIncludeRootCause(final boolean includeRootCause) {
        this.includeRootCause = includeRootCause;
    }

    public boolean isIncludeMarker() {
        return includeMarker;
    }

    public void setIncludeMarker(final boolean includeMarker) {
        this.includeMarker = includeMarker;
    }

    public boolean isIncludeMdc() {
        return includeMdc;
    }

    public void setIncludeMdc(final boolean includeMdc) {
        this.includeMdc = includeMdc;
    }

    public boolean isIncludeKeyValues() {
        return includeKeyValues;
    }

    public void setIncludeKeyValues(final boolean includeKeyValues) {
        this.includeKeyValues = includeKeyValues;
    }

    public boolean isIncludeCaller() {
        return includeCaller;
    }

    public void setIncludeCaller(final boolean includeCaller) {
        this.includeCaller = includeCaller;
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

    @SuppressWarnings({"checkstyle:NPathComplexity", "checkstyle:CyclomaticComplexity"})
    @Override
    public void start() {
        if (includeTimestamp) {
            mappers.add((json, event) -> json.append("timestamp", event.getTimeStamp()));
        }
        if (includeNanoseconds) {
            mappers.add((json, event) -> json.append("nanoseconds", event.getNanoseconds()));
        }
        if (includeSequenceNumber) {
            mappers.add((json, event) -> json.append("sequenceNumber", event.getSequenceNumber()));
        }
        if (includeLevelName) {
            mappers.add((json, event) -> json.append("level", event.getLevel().toString()));
        }
        if (includeThreadName) {
            mappers.add((json, event) -> json.append("thread", event.getThreadName()));
        }
        if (includeLoggerName) {
            mappers.add((json, event) -> json.append("logger", event.getLoggerName()));
        }
        if (includeFormattedMessage) {
            mappers.add((json, event) -> json.append("message", event.getFormattedMessage()));
        }
        if (includeRawMessage) {
            mappers.add((json, event) -> json.append("rawMessage", event.getMessage()));
        }
        if (includeMarker) {
            mappers.add((json, event) -> appendMarker(json, event.getMarkerList()));
        }
        if (includeMdc) {
            mappers.add((json, event) -> appendMdc(json, event.getMDCPropertyMap()));
        }
        if (includeKeyValues) {
            mappers.add((json, event) -> appendKeyValues(json, event.getKeyValuePairs()));
        }
        if (includeCaller) {
            mappers.add((json, event) -> appendCaller(json, event.getCallerData()));
        }
        if (includeStacktrace) {
            mappers.add((json, event) -> appendThrowable(json, event.getThrowableProxy()));
        }
        if (includeRootCause) {
            mappers.add((json, event) -> appendRootCause(json, event.getThrowableProxy()));
        }
        if (!staticFields.isEmpty()) {
            mappers.add((json, event) -> appendStaticFields(json, staticFields));
        }

        mappers.addAll(customMappers);

        super.start();
    }

    @Override
    public byte[] encode(final ILoggingEvent event) {
        final StringBuilder sb = new StringBuilder(INITIAL_BUFFER_SIZE);

        final var json = new SimpleJsonEncoder(sb);
        mappers.forEach(m -> m.accept(json, event));
        json.end();

        sb.append(System.lineSeparator());

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendMarker(final SimpleJsonEncoder json, final List<Marker> markerList) {
        if (markerList == null || markerList.isEmpty()) {
            return;
        }

        json.appendObject("markers", j ->
            markerList.forEach(marker -> j.append(marker.getName(), 1)));
    }

    private static void appendMdc(final SimpleJsonEncoder json, final Map<String, String> mdcProperties) {
        if (mdcProperties == null || mdcProperties.isEmpty()) {
            return;
        }

        json.appendObject("mdc", j ->
            mdcProperties.forEach(j::append));
    }

    private static void appendKeyValues(final SimpleJsonEncoder json, final List<KeyValuePair> keyValuePairs) {
        if (keyValuePairs == null || keyValuePairs.isEmpty()) {
            return;
        }

        json.appendObject("keyValues", j ->
            keyValuePairs.forEach(kvp -> j.append(kvp.key, kvp.value)));
    }

    @SuppressWarnings("PMD.UseVarargs")
    private static void appendCaller(final SimpleJsonEncoder json, final StackTraceElement[] stackTraceElements) {
        if (stackTraceElements == null || stackTraceElements.length == 0) {
            return;
        }

        final StackTraceElement first = stackTraceElements[0];
        json.appendObject("caller", j -> j
            .append("file", first.getFileName())
            .append("line", first.getLineNumber())
            .append("class", first.getClassName())
            .append("method", first.getMethodName()));
    }

    private static void appendThrowable(final SimpleJsonEncoder json, final IThrowableProxy throwableProxy) {
        if (throwableProxy == null) {
            return;
        }

        json.append("stacktrace", ThrowableProxyUtil.asString(throwableProxy));
    }

    private static void appendRootCause(final SimpleJsonEncoder json, final IThrowableProxy throwableProxy) {
        findRootException(throwableProxy).ifPresent(rootException ->
            json.appendObject("rootCause", j -> j
                .append("class", rootException.getClassName())
                .append("message", rootException.getMessage())));
    }

    private static Optional<IThrowableProxy> findRootException(final IThrowableProxy throwableProxy) {
        if (throwableProxy == null) {
            return Optional.empty();
        }

        IThrowableProxy rootCause = throwableProxy;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        return Optional.of(rootCause);
    }

    private static void appendStaticFields(final SimpleJsonEncoder json, final Map<String, Object> staticFields) {
        json.appendObject("staticFields", j ->
            staticFields.forEach(j::append));
    }

    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
    @Override
    public byte[] headerBytes() {
        return null;
    }

    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
    @Override
    public byte[] footerBytes() {
        return null;
    }

}
