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

/**
 * The class Rabble is the top-level class for working with rabble templates.
 *
 * Rabble templates work on the principle of exemplar-as-template where the
 * template is a valid instance of a document. Where most templating strategies
 * are uni-directional - injecting data into a template, rabble template processing
 * is (approximately) bi-directional. Blobs of JSON stuff can be injected into a
 * template, and blobs of JSON stuff can be extracted from a template.
 *
 * A template is simply a well formed HTML document with extra attributes annotating
 * which parts of the document contain the templated data. Elements in the document
 * that drive the templating engine are marked with <code>data-rabble-*</code> attributes.
 *
 * All elements that influence templating behaviour are named with a <code>data-rabble-name</code>
 * attribute. In the documentation these are referred to as rabble nodes. Each rabble node also
 * has a kind which may be marked with a <code>data-rabble-kind</code> attribute.
 *
 * As of the current version, the following types of templated data are supported:
 *
 * <ul>
 *  <li><code>text</code>
 *  <li><code>lines</code>
 *  <li><code>rich</code>
 *  <li><code>group</code>
 * </ul>
 *
 * @author Thomas Conway
 */
public class Rabble {
    private Document doc;
    private Element root;

    public Rabble(Document doc, Element root) {
        this.doc = doc;
        this.root = root;
    }

    /**
     * Populate the template with the given JSON data.
     * @return an Element node for the new new subtree.
     */
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

                String templateName = null;
                if (elem.hasAttribute("data-rabble-name")) {
                    templateName = elem.getAttribute("data-rabble-name");
                }
                String templateKind = "text";
                if (elem.hasAttribute("data-rabble-kind")) {
                    templateKind = elem.getAttribute("data-rabble-kind");
                }

                if (templateName == null) {
                    /* Not a template node. */
                    Node resKid = visit(seen, elem, data);
                    res.appendChild(resKid);
                    continue;
                }

                if (seen.contains(templateName)) {
                    /* we've already processed this template */
                    continue;
                }
                if (!data.containsKey(templateName)) {
                    /* we don't have any data for this template */
                    continue;
                }
                seen.add(templateName);
    
                switch (templateKind) {
                    case "group":
                        makeGroup(elem, data.get(templateName), res);
                        break;
                    case "rich":
                        makeRich(elem, data.get(templateName), res);
                        break;
                    case "lines":
                        makeLines(elem, data.get(templateName), res);
                        break;
                    case "text":
                        makeText(elem, data.get(templateName), res);
                        break;
                    default:
                        throw new Exception("unknown template kind");
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

    private void makeRich(Element elem, JsonValue data, Element res) throws Exception {
        Node wrapperNode = elem.cloneNode(false);
        jsonToHtml(data, wrapperNode);
        res.appendChild(wrapperNode);
    }

    private void makeLines(Element elem, JsonValue data, Element res) throws Exception {
        Node txtWrapperNode = elem.cloneNode(false);
        res.appendChild(txtWrapperNode);
        Node lineDiv = null;
        Node txtNode = null;
        switch (data.getValueType()) {
            case STRING:
                JsonString txt = (JsonString)data;
                lineDiv = doc.createElement("div");
                txtWrapperNode.appendChild(lineDiv);
                txtNode = doc.createTextNode(txt.getString());
                lineDiv.appendChild(txtNode);
                break;

            case ARRAY:
                JsonArray array = (JsonArray)data;
                for (int i = 0; i < array.size(); i++) {
                    lineDiv = doc.createElement("div");
                    txtWrapperNode.appendChild(lineDiv);
                    txtNode = doc.createTextNode(array.getString(i));
                    lineDiv.appendChild(txtNode);
                }
                break;

            default:
                throw new Exception("makeText: unexpected value type");
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


    /**
     * Traverse a template, and extract the JSON data that populates the template.
     * @return the JSON data that was extracted from the template.
     */
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

                String templateName = null;
                if (elem.hasAttribute("data-rabble-name")) {
                    templateName = elem.getAttribute("data-rabble-name");
                }
                String templateKind = "text";
                if (elem.hasAttribute("data-rabble-kind")) {
                    templateKind = elem.getAttribute("data-rabble-kind");
                }

                if (templateName == null) {
                    /* not a template node */
                    revisit(elem, data);
                    continue;
                }

                switch (templateKind) {
                    case "group":
                        Map<String,List<JsonValue>> kidData = new HashMap<String,List<JsonValue>>();
                        revisit(elem, kidData);
                        addExtracted(templateName, buildJson(kidData), data);
                        break;
                    case "rich":
                        extractRich(elem, templateName, data);
                        break;
                    case "lines":
                        extractLines(elem, templateName, data);
                        break;
                    case "text":
                        extractText(elem, templateName, data);
                        break;
                    default:
                        throw new Exception("unknown template kind");
                }
            }
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

    private void extractLines(Node node, String name, Map<String,List<JsonValue>> data) throws Exception {
        List<String> lines = new ArrayList<String>();
        NodeList kids = node.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node kid = kids.item(i);
            Text txt;
            switch (kid.getNodeType()) {
                case Node.ELEMENT_NODE:
                    Element elem = (Element) kid;
                    if (elem.getNodeName() != "div") {
                        throw new Exception("only div elements allowed in lines templates");
                    }
                    NodeList grandKids = elem.getChildNodes();
                    for (int j = 0; j < grandKids.getLength(); j++) {
                        Node grandKid = grandKids.item(j);
                        switch (grandKid.getNodeType()) {
                            case Node.ELEMENT_NODE:
                                throw new Exception("extractLines: unexpect element node");

                            case Node.TEXT_NODE:
                                txt = (Text) grandKid;
                                lines.add(txt.getNodeValue());
                                break;
                        }
                    }
                    break;
                case Node.TEXT_NODE:
                    txt = (Text) kid;
                    lines.add(txt.getNodeValue());
                    break;
            }
        }
        JsonArrayBuilder itmsBld = Json.createArrayBuilder();
        for (int i = 0; i < lines.size(); i++) {
            itmsBld.add(lines.get(i));
        }
        addExtracted(name, itmsBld.build(), data);
    }

    private void extractText(Node node, String name, Map<String,List<JsonValue>> data) throws Exception {
        NodeList kids = node.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node kid = kids.item(i);
            switch (kid.getNodeType()) {
                case Node.ELEMENT_NODE:
                    throw new Exception("extractText: unexpected element node: " + kid.getNodeName());

                case Node.TEXT_NODE:
                    Text txt = (Text) kid;
                    JsonValue val = Json.createValue(txt.getNodeValue());
                    addExtracted(name, val, data);
                    break;
            }
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
                assert object.size() == 1;
                String tag = (String) object.keySet().toArray()[0];

                Element e = doc.createElement(tag);
                ctxt.appendChild(e);

                JsonValue val = object.get(tag);
                switch (val.getValueType()) {
                    case ARRAY:
                        /* If the first child is the object {"*":{....}} then it's the attributes. */
                        JsonArray kids = (JsonArray) val;
                        Map<String,String> attrs = lookForAttributes(kids);
                        int i0 = 0;
                        if (attrs != null) {
                            for (String attrName : attrs.keySet()) {
                                String attrValue = attrs.get(attrName);
                                e.setAttribute(attrName, attrValue);
                            }
                            i0 = 1; // skip the attributes for recursing.
                        }
                        for (int i = i0; i < kids.size(); ++i) {
                            jsonToHtml(kids.get(i), e);
                        }
                        break;

                    default:
                        jsonToHtml(val, e);
                        break;
                }
                break;

            default:
                throw new Exception("jsonToHtml: unexpected value type");
        }
    }

    private Map<String,String> lookForAttributes(JsonArray kids) {
        if (kids.size() == 0) {
            return null;
        }
        JsonValue kid0 = kids.get(0);
        switch (kid0.getValueType()) {
            case OBJECT:
                JsonObject object = (JsonObject) kid0;
                // Had better be singleton whether it's the attributes or not!
                assert object.size() == 1;
                String key = (String) object.keySet().toArray()[0];
                if (key != "*") {
                    return null;
                }
                // Ok, now it had better be an object.
                JsonObject attrsObj = object.getJsonObject(key);
                Map<String,String> attrs = new HashMap<String,String>();
                for (String attrName : attrsObj.keySet()) {
                    String attrValue = attrsObj.getString(attrName);
                    attrs.put(attrName, attrValue);
                }
                return attrs;

            default:
                return null;
        }
    }

    public JsonValue htmlToJson(Node node) throws Exception {
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                Element elem = (Element) node;
                String tag = elem.getNodeName();

                JsonArrayBuilder resKidsBld =  Json.createArrayBuilder();

                NamedNodeMap attrs = elem.getAttributes();
                if (attrs.getLength() > 0) {
                    JsonObjectBuilder resAttrsBld = Json.createObjectBuilder();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        Node attr = attrs.item(i);
                        resAttrsBld.add(attr.getNodeName(), attr.getNodeValue());
                    }
                    JsonObjectBuilder resAttrsWrapperBld = Json.createObjectBuilder();
                    resAttrsWrapperBld.add("*", resAttrsBld.build());
                    resKidsBld.add(resAttrsWrapperBld.build());
                }
                NodeList kids = elem.getChildNodes();
                for (int i = 0; i < kids.getLength(); i++) {
                    JsonValue kidRes = htmlToJson(kids.item(i));
                    resKidsBld.add(kidRes);
                }
                JsonObjectBuilder resBld = Json.createObjectBuilder();
                resBld.add(tag, resKidsBld.build());
                return resBld.build();

            case Node.TEXT_NODE:
                Text txt = (Text) node;
                return Json.createValue(txt.getNodeValue());

            default:
                throw new Exception("htmlToJson: unexpected node type");
        }
    }

}
