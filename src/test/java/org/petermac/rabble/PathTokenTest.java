package org.petermac.rabble;

// Dependencies for testing framework
//

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Dependencies for actual test code
//

import java.util.List;

class PathTokenTest {

    @Test
    public void testTokenizer() throws Exception {
        String str = "$/wibble[0]/foo//bar[baz()]";
        List<PathToken> toks = PathToken.tokenize(str);
        assertEquals(15, toks.size());
        assertEquals(PathToken.Kind.DOLLAR, toks.get(0).kind);
        assertEquals(PathToken.Kind.SLASH,  toks.get(1).kind);
        assertEquals(PathToken.Kind.NAME,   toks.get(2).kind);
        assertEquals(PathToken.Kind.LBRAC,  toks.get(3).kind);
        assertEquals(PathToken.Kind.NUMINT, toks.get(4).kind);
        assertEquals(PathToken.Kind.RBRAC,  toks.get(5).kind);
        assertEquals(PathToken.Kind.SLASH,  toks.get(6).kind);
        assertEquals(PathToken.Kind.NAME,   toks.get(7).kind);
        assertEquals(PathToken.Kind.DSLASH, toks.get(8).kind);
        assertEquals(PathToken.Kind.NAME,   toks.get(9).kind);
        assertEquals(PathToken.Kind.LBRAC,  toks.get(10).kind);
        assertEquals(PathToken.Kind.NAME,   toks.get(11).kind);
        assertEquals(PathToken.Kind.LPAREN, toks.get(12).kind);
        assertEquals(PathToken.Kind.RPAREN, toks.get(13).kind);
        assertEquals(PathToken.Kind.RBRAC,  toks.get(14).kind);
    }

}
