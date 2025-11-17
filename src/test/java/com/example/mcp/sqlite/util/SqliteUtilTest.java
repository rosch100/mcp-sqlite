package com.example.mcp.sqlite.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqliteUtilTest {

    @Test
    void quoteLiteralEscapesSingleQuotes() {
        assertEquals("'O''Brien'", SqliteUtil.quoteLiteral("O'Brien"));
    }

    @Test
    void quoteLiteralHandlesNull() {
        assertEquals("NULL", SqliteUtil.quoteLiteral(null));
    }
}
