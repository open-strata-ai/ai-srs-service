package com.openstrata.srs.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SemVerTest {

    @Test
    void parsesAndPrints() {
        SemVer v = SemVer.parse("1.4.2");
        assertEquals(1, v.major());
        assertEquals(4, v.minor());
        assertEquals(2, v.patch());
        assertEquals("1.4.2", v.toString());
    }

    @Test
    void rejectsMalformed() {
        assertThrows(DomainException.class, () -> SemVer.parse("1.2"));
        assertThrows(DomainException.class, () -> SemVer.parse("a.b.c"));
    }

    @Test
    void orders() {
        assertTrue(SemVer.parse("1.2.3").compareTo(SemVer.parse("1.2.4")) < 0);
        assertTrue(SemVer.parse("2.0.0").compareTo(SemVer.parse("1.9.9")) > 0);
        assertEquals(0, SemVer.parse("1.0.0").compareTo(SemVer.parse("1.0.0")));
    }

    @Test
    void satisfiesCaretRange() {
        assertTrue(SemVer.parse("1.2.5").satisfies("^1.2.3"));
        assertTrue(SemVer.parse("1.9.0").satisfies("^1.2.3"));
        assertFalse(SemVer.parse("2.0.0").satisfies("^1.2.3"));
        assertFalse(SemVer.parse("1.2.2").satisfies("^1.2.3"));
    }

    @Test
    void satisfiesTildeExactAndWildcard() {
        assertTrue(SemVer.parse("1.2.9").satisfies("~1.2.3"));
        assertFalse(SemVer.parse("1.3.0").satisfies("~1.2.3"));
        assertTrue(SemVer.parse("1.2.3").satisfies("1.2.3"));
        assertFalse(SemVer.parse("1.2.4").satisfies("1.2.3"));
        assertTrue(SemVer.parse("9.9.9").satisfies("*"));
        assertTrue(SemVer.parse("9.9.9").satisfies("latest"));
    }
}
