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

import java.util.Iterator;
import java.util.Map;

/**
 * Simple JSON encoder with very basic functionality that is required by this library.
 */
class SimpleJsonEncoder {

    private static final char OPEN_BRACE = '{';
    private static final char CLOSE_BRACE = '}';
    private static final char QUOTE = '"';

    /**
     * Underlying writer.
     */
    private final StringBuilder appendable;

    /**
     * Flag to determine if a comma has to be added on next append execution.
     */
    private boolean started;

    /**
     * Flag set when JSON object is closed by curly brace.
     */
    private boolean closed;

    SimpleJsonEncoder(final StringBuilder appendable) {
        this.appendable = appendable;
        append(OPEN_BRACE);
    }

    private SimpleJsonEncoder append(final char c) {
        appendable.append(c);
        return this;
    }

    private SimpleJsonEncoder append(final Object str) {
        appendable.append(str.toString());
        return this;
    }

    /**
     * Append field with quotes and escape characters added, if required.
     *
     * @return this
     */
    SimpleJsonEncoder appendToJSON(final String key, final Object value) {
        ensureOpen();
        if (value != null) {
            appendKey(key);
            appendValue(value);
        }
        return this;
    }

    /**
     * Append fields with quotes and escape characters added, if required.
     *
     * @return this
     */
    SimpleJsonEncoder appendToJSON(final String key, final Map<String, Object> values) {
        ensureOpen();
        if (values == null) {
            return this;
        }

        appendKey(key);

        append(OPEN_BRACE);

        for (Iterator<Map.Entry<String, Object>> it = values.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<String, Object> entry = it.next();
            if (entry.getValue() != null) {
                appendKey(entry.getKey(), false);
                appendValue(entry.getValue());
                if (it.hasNext()) {
                    append(',');
                }
            }
        }

        append(CLOSE_BRACE);

        return this;
    }

    private void appendValue(final Object value) {
        if (value instanceof Number) {
            append(value.toString());
        } else {
            append(QUOTE).append(escapeString(value.toString())).append(QUOTE);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Encoder already closed");
        }
    }

    private void appendKey(final String key) {
        appendKey(key, started);
        started = true;
    }

    private void appendKey(final String key, final boolean prependComma) {
        if (prependComma) {
            append(',');
        }
        append(QUOTE).append(escapeString(key)).append(QUOTE).append(':');
    }

    /**
     * Escape characters in string, if required per RFC-7159 (JSON).
     *
     * @param str string to be escaped.
     * @return escaped string.
     */
    @SuppressWarnings("checkstyle:cyclomaticcomplexity")
    private static String escapeString(final String str) {
        final StringBuilder sb = new StringBuilder(str.length());

        for (final char ch : str.toCharArray()) {
            switch (ch) {
                case QUOTE:
                case '\\':
                case '/':
                    sb.append('\\');
                    sb.append(ch);
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(ch < ' ' ? escapeCharacter(ch) : ch);
            }
        }

        return sb.toString();
    }

    /**
     * Escapes character to unicode string representation (&#92;uXXXX).
     *
     * @param ch character to be escaped.
     * @return escaped representation of character.
     */
    @SuppressWarnings("checkstyle:magicnumber")
    private static String escapeCharacter(final char ch) {
        final String prefix;

        if (ch < 0x10) {
            prefix = "000";
        } else if (ch < 0x100) {
            prefix = "00";
        } else if (ch < 0x1000) {
            prefix = "0";
        } else {
            prefix = "";
        }

        return "\\u" + prefix + Integer.toHexString(ch);
    }

    public void end() {
        if (closed) {
            return;
        }

        append(CLOSE_BRACE);
        closed = true;
    }

}
