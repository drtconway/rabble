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
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

class RabbleTestTemplateTest {
    @Test
    public void testEmptyTemplate() throws Exception {
        Document doc = Utils.makeDoc(
            "<html>",
            " <head>",
            " </head>",
            " <body>",
            " </body>",
            "</html>"
        );
        assertNotNull(doc);

        RabbleTestTemplate.Report report = RabbleTestTemplate.check(doc);

        assertNotNull(report);

        assertEquals(4, report.paths.size());
        assertTrue(report.paths.containsKey("group"));
        assertEquals(0, report.paths.get("group").size());
        assertTrue(report.paths.containsKey("rich"));
        assertEquals(0, report.paths.get("rich").size());
        assertTrue(report.paths.containsKey("lines"));
        assertEquals(0, report.paths.get("lines").size());
        assertTrue(report.paths.containsKey("text"));
        assertEquals(0, report.paths.get("text").size());

        assertEquals(0, report.problems.size());
    }
}
