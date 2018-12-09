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

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

public class SimpleJsonEncoderTest {

    private final StringWriter sw = new StringWriter();
    private final SimpleJsonEncoder enc = new SimpleJsonEncoder(sw);

    @Test
    public void string() {
        enc.appendToJSON("aaa", "bbb");
        assertEquals("{\"aaa\":\"bbb\"}", produce());
    }

    private String produce() {
        enc.close();
        return sw.toString();
    }

    @Test
    public void number() {
        enc.appendToJSON("aaa", 123);
        assertEquals("{\"aaa\":123}", produce());
    }

    @Test
    public void quote() {
        enc.appendToJSON("aaa", "\"");
        assertEquals("{\"aaa\":\"\\\"\"}", produce());
    }

    @Test
    public void reverseSolidus() {
        enc.appendToJSON("aaa", "\\");
        assertEquals("{\"aaa\":\"\\\\\"}", produce());
    }

    @Test
    public void solidus() {
        enc.appendToJSON("aaa", "/");
        assertEquals("{\"aaa\":\"\\/\"}", produce());
    }

    @Test
    public void backspace() {
        enc.appendToJSON("aaa", "\b");
        assertEquals("{\"aaa\":\"\\b\"}", produce());
    }

    @Test
    public void formFeed() {
        enc.appendToJSON("aaa", "\f");
        assertEquals("{\"aaa\":\"\\f\"}", produce());
    }

    @Test
    public void newline() {
        enc.appendToJSON("aaa", "\n");
        assertEquals("{\"aaa\":\"\\n\"}", produce());
    }

    @Test
    public void carriageReturn() {
        enc.appendToJSON("aaa", "\r");
        assertEquals("{\"aaa\":\"\\r\"}", produce());
    }

    @Test
    public void tab() {
        enc.appendToJSON("aaa", "\t");
        assertEquals("{\"aaa\":\"\\t\"}", produce());
    }

    @Test
    @SuppressWarnings("checkstyle:avoidescapedunicodecharacters")
    public void unicode() {
        enc.appendToJSON("\u0002", "\u0007\u0019");
        assertEquals("{\"\\u0002\":\"\\u0007\\u0019\"}", produce());
    }

    @Test
    public void multipleFields() {
        enc.appendToJSON("bbb", "ccc");
        enc.appendToJSON("ddd", 123);

        assertEquals("{\"bbb\":\"ccc\",\"ddd\":123}", produce());
    }

}
