package org.petermac.rabble;

// Dependencies for testing framework
//

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Dependencies for actual test code
//

import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;

class UtilsTest {
    @Test
    public void testCollect() {
        List<String> res = Utils.collect("the", "quick", "brown", "fox");
        assertNotNull(res);
        assertEquals(4, res.size());
        assertEquals("the", res.get(0));
        assertEquals("quick", res.get(1));
        assertEquals("brown", res.get(2));
        assertEquals("fox", res.get(3));
    }

    @Test
    public void testNewHashSet() {
        Set<Integer> res = Utils.newHashSet(1, 2, 3, 4, 5);
        assertNotNull(res);
        assertEquals(5, res.size());
        assertTrue(res.contains(1), "set does not contain 1");
        assertTrue(res.contains(2), "set does not contain 1");
        assertTrue(res.contains(3), "set does not contain 1");
        assertTrue(res.contains(4), "set does not contain 1");
        assertTrue(res.contains(5), "set does not contain 1");
    }

    @Test
    public void testMakeDoc1() throws Exception {
        Document doc = Utils.makeDoc(
            "<html>",
            "</html>"
        );
        assertNotNull(doc);
        assertEquals("html", doc.getDocumentElement().getNodeName());
    }
}
