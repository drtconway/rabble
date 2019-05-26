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

    @Test
    public void testSimpleTemplate1() throws Exception {
        Document doc = Utils.makeDoc(
            "<html>",
            " <head>",
            " </head>",
            " <body>",
            "  <div data-rabble-name='urn' data-rabble-kind='text'></div>",
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
        assertEquals(1, report.paths.get("text").size());
        assertTrue(report.paths.get("text").contains("#/urn"));

        assertEquals(0, report.problems.size());
    }

    @Test
    public void testSimpleTemplate2() throws Exception {
        Document doc = Utils.makeDoc(
            "<html>",
            " <head>",
            " </head>",
            " <body>",
            "  <div data-rabble-name='request' data-rabble-kind='group'>",
            "  </div>",
            " </body>",
            "</html>"
        );
        assertNotNull(doc);

        //System.out.println("testSimpleTemplate2");
        RabbleTestTemplate.Report report = RabbleTestTemplate.check(doc);

        assertNotNull(report);

        assertEquals(4, report.paths.size());
        assertTrue(report.paths.containsKey("group"));
        assertEquals(1, report.paths.get("group").size());
        assertTrue(report.paths.containsKey("rich"));
        assertEquals(0, report.paths.get("rich").size());
        assertTrue(report.paths.containsKey("lines"));
        assertEquals(0, report.paths.get("lines").size());
        assertTrue(report.paths.containsKey("text"));
        assertEquals(0, report.paths.get("text").size());

        assertTrue(report.paths.get("group").contains("#/request"));

        assertEquals(1, report.problems.size());
        assertEquals("warn", report.problems.get(0).level);
        assertNull(report.problems.get(0).context);
        assertNotNull(report.problems.get(0).path);
        assertEquals("#/request", report.problems.get(0).path);
        assertEquals("group has no concrete instantiation in template", report.problems.get(0).message);
    }

    @Test
    public void testSimpleTemplate3() throws Exception {
        Document doc = Utils.makeDoc(
            "<html>",
            " <head>",
            " </head>",
            " <body>",
            "  <div data-rabble-name='request' data-rabble-kind='group'>",
            "   <div data-rabble-name='urn' data-rabble-kind='text'></div>",
            "  </div>",
            " </body>",
            "</html>"
        );
        assertNotNull(doc);


        //System.out.println("testSimpleTemplate3");
        RabbleTestTemplate.Report report = RabbleTestTemplate.check(doc);

        assertNotNull(report);

        assertEquals(4, report.paths.size());
        assertTrue(report.paths.containsKey("group"));
        assertEquals(1, report.paths.get("group").size());
        assertTrue(report.paths.containsKey("rich"));
        assertEquals(0, report.paths.get("rich").size());
        assertTrue(report.paths.containsKey("lines"));
        assertEquals(0, report.paths.get("lines").size());
        assertTrue(report.paths.containsKey("text"));
        assertEquals(1, report.paths.get("text").size());

        assertTrue(report.paths.get("group").contains("#/request"));
        assertTrue(report.paths.get("text").contains("#/request/urn"));

        assertEquals(0, report.problems.size());
    }
}
