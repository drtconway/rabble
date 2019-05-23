package org.petermac.rabble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonNumber;


import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

class Rabble {
    private Document doc;
    private Element root;

    public Rabble(Document doc, Element root) {
        this.doc = doc;
        this.root = root;
    }

    public Element instantiate(JsonObject data) throws Exception {
        Set<String> seen = new HashSet<String>();
        return visit(seen, root, data);
    }

    private Element visit(Set<String> seen, Element node, JsonObject data) throws Exception {
        Element res = (Element) node.cloneNode(false);
        NodeList kids = node.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node kid = kids.item(i);
            if (kid.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) kid;
                if (elem.hasAttribute("data-rabble-group")) {
                    String tn = elem.getAttribute("data-rabble-group");
                    if (seen.contains(tn)) {
                        continue;
                    }
                    if (!data.containsKey(tn)) {
                        continue;
                    }
                    seen.add(tn);
                    makeGroup(elem, data.get(tn), res);
                } 
                else if (elem.hasAttribute("data-rabble-text")) {
                    String tn = elem.getAttribute("data-rabble-text");
                    if (seen.contains(tn)) {
                        continue;
                    }
                    if (!data.containsKey(tn)) {
                        continue;
                    }
                    seen.add(tn);
                    makeText(elem, data.get(tn), res);
                }
                else if (elem.hasAttribute("data-rabble-rich")) {
                    String tn = elem.getAttribute("data-rabble-rich");
                    if (seen.contains(tn)) {
                        continue;
                    }
                    if (!data.containsKey(tn)) {
                        continue;
                    }
                    seen.add(tn);
                    makeRich(elem, data.get(tn), res);
                }
                else {
                    /* Not a template node. */
                    Node resKid = visit(seen, elem, data);
                    res.appendChild(resKid);
                }
            } else {
                Node resKid = kid.cloneNode(false);
                res.appendChild(resKid);
            }
        }
        return res;
    }

    private void makeGroup(Element elem, JsonValue data, Element res) throws Exception {
        switch (data.getValueType()) {
            case OBJECT:
                JsonObject object = (JsonObject) data;
                Set<String> seen = new HashSet<String>();
                Element resNode = visit(seen, elem, object);
                res.appendChild(resNode);
                break;
            case ARRAY:
                JsonArray array = (JsonArray)data;
                for (int i = 0; i < array.size(); i++) {
                    makeGroup(elem, array.get(i), res);
                }
                break;
            default:
                throw new Exception("makeGroup: unexpected value type");
        }
    }

    private void makeText(Element elem, JsonValue data, Element res) throws Exception {
        switch (data.getValueType()) {
            case STRING:
                JsonString txt = (JsonString)data;
                Node txtWrapperNode = elem.cloneNode(false);
                Node txtNode = doc.createTextNode(txt.getString());
                txtWrapperNode.appendChild(txtNode);
                res.appendChild(txtWrapperNode);
                break;
            case ARRAY:
                JsonArray array = (JsonArray)data;
                for (int i = 0; i < array.size(); i++) {
                    makeText(elem, array.get(i), res);
                }
                break;
            default:
                throw new Exception("makeText: unexpected value type");
        }
    }

    private void makeRich(Element elem, JsonValue data, Element res) throws Exception {
        Node wrapperNode = elem.cloneNode(false);
        jsonToHtml(data, wrapperNode);
        res.appendChild(wrapperNode);
    }


    public JsonValue extract(Element elem) throws Exception {
        Map<String,List<JsonValue>> data = new HashMap<String,List<JsonValue>>();
        revisit(elem, data);
        return buildJson(data);
    }

    public void revisit(Node node, Map<String,List<JsonValue>> data) throws Exception {
        NodeList kids = node.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node kid = kids.item(i);
            if (kid.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) kid;
                if (elem.hasAttribute("data-rabble-group")) {
                    String tn = elem.getAttribute("data-rabble-group");
                    Map<String,List<JsonValue>> kidData = new HashMap<String,List<JsonValue>>();
                    revisit(elem, kidData);
                    addExtracted(tn, buildJson(kidData), data);
                }
                else if (elem.hasAttribute("data-rabble-text")) {
                    String tn = elem.getAttribute("data-rabble-text");
                    extractText(elem, tn, data);
                }
                else if (elem.hasAttribute("data-rabble-rich")) {
                    String tn = elem.getAttribute("data-rabble-rich");
                    extractRich(elem, tn, data);
                }
                else {
                    /* not a template node */
                    revisit(elem, data);
                }
            }
        }
    }

    private void extractText(Node node, String name, Map<String,List<JsonValue>> data) throws Exception {
        NodeList kids = node.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node kid = kids.item(i);
            if (kid.getNodeType() == Node.ELEMENT_NODE) {
                throw new Exception("extractText: unexpect element node");
            }

            Text txt = (Text) kid;
            JsonValue val = Json.createValue(txt.getNodeValue());
            addExtracted(name, val, data);
        }
    }

    private void extractRich(Node node, String name, Map<String,List<JsonValue>> data) throws Exception {
        NodeList kids = node.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node kid = kids.item(i);
            JsonValue val = htmlToJson(kid);
            addExtracted(name, val, data);
        }
    }

    private void addExtracted(String name, JsonValue value, Map<String,List<JsonValue>> data) {
        if (!data.containsKey(name)) {
            data.put(name, new ArrayList<JsonValue>());
        }
        List<JsonValue> itms = data.get(name);
        if (value != null) {
            itms.add(value);
        }
    }

    private JsonValue buildJson(Map<String,List<JsonValue>> data) {
        JsonObjectBuilder resBld = Json.createObjectBuilder();
        for (String nm : data.keySet()) {
            List<JsonValue> itms = data.get(nm);
            switch (itms.size()) {
                case 0:
                    resBld.add(nm, JsonValue.EMPTY_JSON_ARRAY);
                    break;
                case 1:
                    resBld.add(nm, itms.get(0));
                    break;
                default:
                    JsonArrayBuilder itmsBld = Json.createArrayBuilder();
                    for (int i = 0; i < itms.size(); i++) {
                        itmsBld.add(itms.get(i));
                    }
                    resBld.add(nm, itmsBld.build());
                    break;
            }
        }
        return resBld.build();
    }

    /* for testing only */
    public Node makeHtml(JsonValue v) throws Exception {
        Node rootNode = doc.createElement("root");
        jsonToHtml(v, rootNode);
        return rootNode;
    }

    private void jsonToHtml(JsonValue data, Node ctxt) throws Exception {
        switch (data.getValueType()) {
            case STRING:
                JsonString txt = (JsonString)data;
                Node txtNode = doc.createTextNode(txt.getString());
                ctxt.appendChild(txtNode);
                break;
            case ARRAY:
                JsonArray array = (JsonArray)data;
                for (int i = 0; i < array.size(); i++) {
                    jsonToHtml(array.get(i), ctxt);
                }
                break;
            case OBJECT:
                JsonObject object = (JsonObject) data;
                /* check Json only contains the expected keys. */
                assert object.containsKey("name");
                int z = 1;
                if (object.containsKey("attributes")) {
                    z++;
                }
                if (object.containsKey("children")) {
                    z++;
                }
                assert object.size() == z;

                Element e = doc.createElement(object.getString("name"));
                ctxt.appendChild(e);

                if (object.containsKey("attributes")) {
                    JsonObject attrs = object.getJsonObject("attributes");
                    for (String attrName : attrs.keySet()) {
                        e.setAttribute(attrName, attrs.getString(attrName));
                    }
                }

                if (object.containsKey("children")) {
                    JsonArray kids = object.getJsonArray("children");
                    for (int i = 0; i < kids.size(); i++) {
                        jsonToHtml(kids.get(i), e);
                    }
                }
                break;
            default:
                throw new Exception("jsonToHtml: unexpected value type");
        }
    }

    public JsonValue htmlToJson(Node node) throws Exception {
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                Element elem = (Element) node;
                JsonObjectBuilder resBld = Json.createObjectBuilder();
                resBld.add("name", elem.getNodeName());
                NamedNodeMap attrs = elem.getAttributes();
                if (attrs.getLength() > 0) {
                    JsonObjectBuilder resAttrsBld = Json.createObjectBuilder();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        Node attr = attrs.item(i);
                        resAttrsBld.add(attr.getNodeName(), attr.getNodeValue());
                    }
                    resBld.add("attributes", resAttrsBld.build());
                }
                NodeList kids = elem.getChildNodes();
                if (kids.getLength() > 0) {
                    JsonArrayBuilder resKidsBld =  Json.createArrayBuilder();
                    for (int i = 0; i < kids.getLength(); i++) {
                        JsonValue kidRes = htmlToJson(kids.item(i));
                        resKidsBld.add(kidRes);
                    }
                    resBld.add("children", resKidsBld.build());
                }
                return resBld.build();
            case Node.TEXT_NODE:
                Text txt = (Text) node;
                return Json.createValue(txt.getNodeValue());
            default:
                throw new Exception("htmlToJson: unexpected node type");
        }
    }

/*
    public static void main(String[] args) {
        try {

            String filename = args[0];
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder(); 
            Document doc = db.parse(new File(filename));

            Rabble r = new Rabble(doc, doc.getDocumentElement());

            if (false) {
                JsonValue j = r.htmlToJson(doc.getDocumentElement());
                Node n = r.makeHtml(j);
                DOMImplementationLS domImplLS = (DOMImplementationLS) doc.getImplementation();
                LSSerializer serializer = domImplLS.createLSSerializer();
                String str = serializer.writeToString(n);
                System.out.println(str);
            }
            if (true) {
                JsonValue j = r.extract(doc.getDocumentElement());
                Json.createWriter(System.out).write(j);
            }

        } catch (Exception e) {
            System.err.println(e.toString());
        }

        if (1) {
            JsonValue j = r.htmlToJson(doc.documentElement);
            Node n = r.makeHtml(j);
            DOMImplementationLS domImplLS = (DOMImplementationLS) doc.getImplementation();
            LSSerializer serializer = domImplLS.createLSSerializer();
            String str = serializer.writeToString(n);
            println(str);
        }
        if (0) {
            JsonValue j = r.htmlToJson(doc.documentElement);
            Json.createWriter(System.out).write(j);
        }

        if (0) {
            JsonReader reader = Json.createReader(new FileReader(args[1]));
            JsonValue dat = reader.read();

            Element e = r.instantiate(dat);
            DOMImplementationLS domImplLS = (DOMImplementationLS) doc.getImplementation();
            LSSerializer serializer = domImplLS.createLSSerializer();
            String str = serializer.writeToString(e);
            println(str);
        }

        if (0) {
            JsonReader reader = Json.createReader(new FileReader(args[1]));
            JsonValue dat = reader.read();

            Element e = r.instantiate(dat);
            DOMImplementationLS domImplLS = (DOMImplementationLS) doc.getImplementation();
            LSSerializer serializer = domImplLS.createLSSerializer();
            String str = serializer.writeToString(e);
            println(str);
        }
    }
*/
}