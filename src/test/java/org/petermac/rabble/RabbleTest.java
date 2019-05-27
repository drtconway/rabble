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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

class RabbleTest {
    public static final void assertXmlEquals(Node lhs, Node rhs) {
        assertEquals(lhs.getNodeType(), rhs.getNodeType());
        switch (lhs.getNodeType()) {
            case Node.ELEMENT_NODE:
                Element lhsElem = (Element) lhs;
                Element rhsElem = (Element) rhs;
                assertEquals(lhs.getNodeName(), rhs.getNodeName());

                /* Check attributes */
                NamedNodeMap lhsAttrs = lhsElem.getAttributes();
                NamedNodeMap rhsAttrs = rhsElem.getAttributes();
                for (int i = 0; i < lhsAttrs.getLength(); i++) {
                    /* for each lhs attr, check the rhs one is there. */
                    Node lhsAttr = lhsAttrs.item(i);
                    assertTrue(rhsElem.hasAttribute(lhsAttr.getNodeName()));
                    Node rhsAttr = rhsAttrs.getNamedItem(lhsAttr.getNodeName());
                    assertEquals(lhsAttr.getNodeName(), rhsAttr.getNodeName());
                    assertEquals(lhsAttr.getNodeValue(), rhsAttr.getNodeValue());
                }
                for (int i = 0; i < rhsAttrs.getLength(); i++) {
                    /* for each rhs attr, check the lhs one is there. */
                    Node rhsAttr = rhsAttrs.item(i);
                    assertTrue(lhsElem.hasAttribute(rhsAttr.getNodeName()));
                    Node lhsAttr = lhsAttrs.getNamedItem(rhsAttr.getNodeName());
                    assertEquals(lhsAttr.getNodeName(), rhsAttr.getNodeName());
                    assertEquals(lhsAttr.getNodeValue(), rhsAttr.getNodeValue());
                }

                /* Check children */
                NodeList lhsKids = lhsElem.getChildNodes();
                NodeList rhsKids = rhsElem.getChildNodes();
                assertEquals(lhsKids.getLength(), rhsKids.getLength());
                for (int i = 0; i < lhsKids.getLength(); i++) {
                    assertXmlEquals(lhsKids.item(i), rhsKids.item(i));
                }

                break;

            case Node.TEXT_NODE:
                assertEquals(lhs.getNodeValue(), rhs.getNodeValue());
                break;

            default:
                /* ignore comments, etc. */
                break;
        }
    }

    public static final void assertJsonEquals(JsonValue lhs, JsonValue rhs) {
        assertEquals(lhs.getValueType(), rhs.getValueType());
        switch (lhs.getValueType()) {
            case STRING:
                JsonString lhsTxt = (JsonString)lhs;
                JsonString rhsTxt = (JsonString)rhs;
                assertEquals(lhsTxt.getString(), rhsTxt.getString());
                break;

            case ARRAY:
                JsonArray lhsArray = (JsonArray)lhs;
                JsonArray rhsArray = (JsonArray)rhs;
                assertEquals(lhsArray.size(), rhsArray.size());
                for (int i = 0; i < lhsArray.size(); i++) {
                    assertJsonEquals(lhsArray.get(i), rhsArray.get(i));
                }
                break;

            case OBJECT:
                JsonObject lhsObject = (JsonObject)lhs;
                JsonObject rhsObject = (JsonObject)rhs;
                assertEquals(lhsObject.size(), rhsObject.size());
                Set<String> lhsKeys = lhsObject.keySet();
                Set<String> rhsKeys = rhsObject.keySet();
                assertEquals(lhsKeys, rhsKeys);
                for (String key : lhsKeys) {
                    JsonValue lhsKid = lhsObject.get(key);
                    JsonValue rhsKid = rhsObject.get(key);
                    assertJsonEquals(lhsKid, rhsKid);
                }
        }
    }

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

        String jsonText = Utils.lines(
            "{ }"
        );
        JsonValue json = Utils.textToJson(jsonText);
        assertEquals(JsonValue.ValueType.OBJECT, json.getValueType());
        JsonObject data = (JsonObject)json;
        assertEquals(0, data.size());

        Rabble rabble = new Rabble(doc, doc.getDocumentElement());
        assertNotNull(rabble);

        Element e = rabble.instantiate(data);
        String eStr = Utils.serialize(e);
        assertEquals(docText, eStr);
    }

    @Test
    public void testSimpleTemplate1() throws Exception {
        String docText = Utils.lines(
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
        Document doc = Utils.textToDoc(docText);

        assertNotNull(doc);

        String jsonText = Utils.lines(
            "{\"request\": {\"urn\": \"PMEX1234567\"}}"
        );
        JsonValue json = Utils.textToJson(jsonText);
        assertEquals(JsonValue.ValueType.OBJECT, json.getValueType());
        JsonObject data = (JsonObject)json;
        assertEquals(1, data.size());

        Rabble rabble = new Rabble(doc, doc.getDocumentElement());
        assertNotNull(rabble);

        Element e = rabble.instantiate(data);

        String resText = Utils.lines(
            "<html>",
            " <head>",
            " </head>",
            " <body>",
            "  <div data-rabble-kind='group' data-rabble-name='request'>",
            "   <div data-rabble-kind='text' data-rabble-name='urn'>PMEX1234567</div>",
            "  </div>",
            " </body>",
            "</html>"
        );
        Document resDoc = Utils.textToDoc(resText);
        assertXmlEquals(resDoc.getDocumentElement(), e);
    }

    @Test
    public void testSimpleExtract() throws Exception {
        String docText = Utils.lines(
            "<html>",
            " <head>",
            " </head>",
            " <body>",
            "  <div data-rabble-name='request' data-rabble-kind='group'>",
            "   <div data-rabble-name='urn' data-rabble-kind='text'>PMEX1234567</div>",
            "  </div>",
            " </body>",
            "</html>"
        );
        Document doc = Utils.textToDoc(docText);

        assertNotNull(doc);

        String jsonText = Utils.lines(
            "{\"request\": {\"urn\": \"PMEX1234567\"}}"
        );
        JsonValue json = Utils.textToJson(jsonText);
        assertEquals(JsonValue.ValueType.OBJECT, json.getValueType());
        JsonObject data = (JsonObject)json;
        assertEquals(1, data.size());

        Rabble rabble = new Rabble(doc, doc.getDocumentElement());
        assertNotNull(rabble);

        JsonValue e = rabble.extract(doc.getDocumentElement());
        assertNotNull(e);
        assertJsonEquals(json, e);
    }

    @Test
    public void testRichExtract() throws Exception {
        String docText = Utils.lines(
            "<html>",
            " <head>",
            " </head>",
            " <body>",
            "  <div data-rabble-name='result' data-rabble-kind='group'>",
            "   <div data-rabble-name='comment' data-rabble-kind='rich'>",
            "    <div><b>PMS2</b> Mutations to the PMS2 gene are bad <cite>PM:31102422</cite>.",
            "    </div>",
            "   </div>",
            "  </div>",
            " </body>",
            "</html>"
        );
        Document doc = Utils.textToDoc(docText);

        assertNotNull(doc);

        String jsonText = Utils.lines(
            "{\"result\":{\"comment\":[\"\\n    \",{\"name\":\"div\",\"children\":[{\"name\":\"b\",\"children\":[\"PMS2\"]},\" Mutations to the PMS2 gene are bad \",{\"name\":\"cite\",\"children\":[\"PM:31102422\"]},\".\\n    \"]},\"\\n   \"]}}"
        );
        JsonValue json = Utils.textToJson(jsonText);
        assertEquals(JsonValue.ValueType.OBJECT, json.getValueType());
        JsonObject data = (JsonObject)json;
        assertEquals(1, data.size());

        Rabble rabble = new Rabble(doc, doc.getDocumentElement());
        assertNotNull(rabble);

        JsonValue e = rabble.extract(doc.getDocumentElement());
        assertNotNull(e);

        Json.createWriter(System.out).write(e);

        assertJsonEquals(json, e);
    }
}
