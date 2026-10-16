package com.custommobs.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/* ───────── Version rules: -alpha suffix, every part rolls over at 99 ───────── */
class AlphaVersionTest {
    @Test
    void parsesTagsAndVersions() {
        assertEquals(new AlphaVersion(0, 0, 1), AlphaVersion.parse("v0.0.1-alpha"));
        assertEquals(new AlphaVersion(12, 3, 99), AlphaVersion.parse("12.3.99-alpha"));
        assertNull(AlphaVersion.parse("v1.0.0"));
        assertNull(AlphaVersion.parse("v100.0.0-alpha"));
        assertNull(AlphaVersion.parse("1.0.0-beta"));
        assertEquals("v0.0.1-alpha", AlphaVersion.parse("0.0.1-alpha").tag());
    }

    @Test
    void rollsOverAt99() {
        assertEquals("0.0.2-alpha", AlphaVersion.parse("0.0.1-alpha").next().toString());
        assertEquals("0.1.0-alpha", AlphaVersion.parse("0.0.99-alpha").next().toString());
        assertEquals("1.0.0-alpha", AlphaVersion.parse("0.99.99-alpha").next().toString());
        assertEquals("5.99.0-alpha", AlphaVersion.parse("5.98.99-alpha").next().toString());
        assertEquals("99.99.99-alpha", AlphaVersion.parse("99.99.98-alpha").next().toString());
        assertThrows(IllegalStateException.class, () -> AlphaVersion.parse("99.99.99-alpha").next());
    }

    @Test
    void comparesPartByPart() {
        assertTrue(AlphaVersion.parse("0.1.0-alpha").compareTo(AlphaVersion.parse("0.0.99-alpha")) > 0);
        assertTrue(AlphaVersion.parse("1.0.0-alpha").compareTo(AlphaVersion.parse("0.99.99-alpha")) > 0);
        assertEquals(0, AlphaVersion.parse("v2.3.4-alpha").compareTo(AlphaVersion.parse("2.3.4-alpha")));
    }
}
