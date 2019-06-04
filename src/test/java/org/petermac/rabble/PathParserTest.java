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

class PathParserTest {

    @Test
    public void testTokenizer0() throws Exception {
        String str = "$";
        List<PathToken> toks = PathToken.tokenize(str);
        PathParser p = new PathParser(toks);
        PathTree t = p.parse();
    }

    @Test
    public void testTokenizer1() throws Exception {
        String str = "$/foo[0]/bar[1 < 2]//baz[pos() != 3]";
        List<PathToken> toks = PathToken.tokenize(str);
        PathParser p = new PathParser(toks);
        PathTree t = p.parse();
    }
}

