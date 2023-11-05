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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SimpleJsonEncoderTest {

    private final StringBuilder sb = new StringBuilder();
    private final SimpleJsonEncoder enc = new SimpleJsonEncoder(sb);

    @Test
    void string() {
        enc.append("aaa", "bbb");
        assertThat(produce()).isEqualTo("{\"aaa\":\"bbb\"}");
    }

    private String produce() {
        enc.end();
        return sb.toString();
    }

    @Test
    void number() {
        enc.append("aaa", 123);
        assertThat(produce()).isEqualTo("{\"aaa\":123}");
    }

    @Test
    void quote() {
        enc.append("aaa", "\"");
        assertThat(produce()).isEqualTo("{\"aaa\":\"\\\"\"}");
    }

    @Test
    void reverseSolidus() {
        enc.append("aaa", "\\");
        assertThat(produce()).isEqualTo("{\"aaa\":\"\\\\\"}");
    }

    @Test
    void solidus() {
        enc.append("aaa", "/");
        assertThat(produce()).isEqualTo("{\"aaa\":\"\\/\"}");
    }

    @Test
    void backspace() {
        enc.append("aaa", "\b");
        assertThat(produce()).isEqualTo("{\"aaa\":\"\\b\"}");
    }

    @Test
    void formFeed() {
        enc.append("aaa", "\f");
        assertThat(produce()).isEqualTo("{\"aaa\":\"\\f\"}");
    }

    @Test
    void newline() {
        enc.append("aaa", "\n");
        assertThat(produce()).isEqualTo("{\"aaa\":\"\\n\"}");
    }

    @Test
    void carriageReturn() {
        enc.append("aaa", "\r");
        assertThat(produce()).isEqualTo("{\"aaa\":\"\\r\"}");
    }

    @Test
    void tab() {
        enc.append("aaa", "\t");
        assertThat(produce()).isEqualTo("{\"aaa\":\"\\t\"}");
    }

    @Test
    @SuppressWarnings("checkstyle:avoidescapedunicodecharacters")
    void unicode() {
        enc.append("\u0002", "\u0007\u0019");
        assertThat(produce()).isEqualTo("{\"\\u0002\":\"\\u0007\\u0019\"}");
    }

    @Test
    void multipleFields() {
        enc.append("bbb", "ccc");
        enc.append("ddd", 123);

        assertThat(produce()).isEqualTo("{\"bbb\":\"ccc\",\"ddd\":123}");
    }

}
