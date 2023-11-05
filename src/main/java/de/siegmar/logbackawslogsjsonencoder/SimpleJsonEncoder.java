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

import java.util.function.Consumer;

/**
 * Simple JSON encoder with very basic functionality that is required by this library.
 */
public class SimpleJsonEncoder {

    private static final int JSON_MAX_DEPTH = 8;
    private static final char OPEN_BRACE = '{';
    private static final char CLOSE_BRACE = '}';
    private static final char QUOTE = '"';
    private static final char COLON = ':';
    private static final char COMMA = ',';
    private static final String NULL = "null";

    /**
     * Underlying writer.
     */
    private final StringBuilder sb;

    /**
     * Flag to determine if a comma has to be added on next append execution.
     */
    private final boolean[] prependComma = new boolean[JSON_MAX_DEPTH];
    private int currentDepth;

    SimpleJsonEncoder(final StringBuilder sb) {
        this.sb = sb;
        sb.append(OPEN_BRACE);
    }

    /**
     * Append field to this JSON object.
     *
     * @param key the key of the JSON element to add
     * @param value the value of the JSON element to add
     *
     * @return this
     */
    public SimpleJsonEncoder append(final String key, final Object value) {
        return appendKey(key).appendValue(value);
    }

    /**
     * Append object to this JSON object.
     *
     * @param key the key of the JSON element to add
     * @param consumer a consumer to this {@code SimpleJsonEncoder} to add JSON body
     *
     * @return A reference to this {@code SimpleJsonEncoder}
     */
    public SimpleJsonEncoder appendObject(final String key, final Consumer<SimpleJsonEncoder> consumer) {
        appendKey(key);
        sb.append(OPEN_BRACE);
        currentDepth++;

        consumer.accept(this);

        end();

        return this;
    }

    private SimpleJsonEncoder appendKey(final String key) {
        if (prependComma[currentDepth]) {
            sb.append(COMMA);
        } else {
            prependComma[currentDepth] = true;
        }

        final CharSequence keyVal = key == null ? NULL : escapeString(key);
        sb.append(QUOTE).append(keyVal).append(QUOTE).append(COLON);

        return this;
    }

    private SimpleJsonEncoder appendValue(final Object value) {
        if (value == null) {
            sb.append(NULL);
        } else if (value instanceof Number) {
            sb.append(value);
        } else {
            sb.append(QUOTE).append(escapeString(value.toString())).append(QUOTE);
        }

        return this;
    }

    void end() {
        sb.append(CLOSE_BRACE);
        prependComma[currentDepth--] = false;
    }

    /**
     * Escape characters in string, if required per RFC-7159 (JSON).
     *
     * @param str string to be escaped.
     * @return escaped string.
     */
    @SuppressWarnings("checkstyle:cyclomaticcomplexity")
    private static StringBuilder escapeString(final String str) {
        final StringBuilder sb = new StringBuilder(str.length());

        for (int i = 0; i < str.length(); i++) {
            final char ch = str.charAt(i);
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

        return sb;
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

}
