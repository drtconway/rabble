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

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

class RabbleTest {
    @Test
    public void testEmptyTemplate() throws Exception {
        String docText = Utils.lines(
            "<html>",
            " <head>",
            " </head>",
            " <body>",
            " </body>",
            "</html>"
        );
        Document doc = Utils.textToDoc(docText);

        assertNotNull(doc);

        JsonValue json = Utils.makeJson(
            "{ }"
        );
        assertEquals(JsonValue.ValueType.OBJECT, json.getValueType());
        JsonObject data = (JsonObject)json;
        assertEquals(0, data.size());

        Rabble rabble = new Rabble(doc, doc.getDocumentElement());
        assertNotNull(rabble);

        Element e = rabble.instantiate(data);
        String eStr = Utils.serialize(e);
        assertEquals(docText, eStr);
    }
}
