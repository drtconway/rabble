package org.petermac.rabble

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonNumber;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;

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

    public Element instantiate(JsonObject data) {
        Set<String> seen = [];
        return visit(seen, root, data);
    }

    private Element visit(Set<String> seen, Element node, JsonObject data) {
        Element res = (Element) node.cloneNode(false);
        NodeList kids = node.childNodes;
        for (int i = 0; i < kids.length; i++) {
            Node kid = kids.item(i);
            if (kid.nodeType == Node.ELEMENT_NODE) {
                Element elem = (Element) kid;
                if (elem.hasAttribute("data-rabble")) {
                    String tn = elem.getAttribute("data-rabble");
                    if (seen.contains(tn)) {
                        continue;
                    }
                    if (!data.containsKey(tn)) {
                        continue;
                    }
                    seen.add(tn);
                    List<Node> resKids = make(elem, data[tn], res);
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
                    List<Node> resKids = makeRich(elem, data[tn], res);
                }
                else {
                    /* Not a template node. */
                    Node resKid = visit(seen, kid, data);
                    res.appendChild(resKid);
                }
            } else {
                Node resKid = kid.cloneNode(false);
                res.appendChild(resKid);
            }
        }
        return res;
    }

    private void make(Element elem, JsonObject data, Element res) {
        Set<String> seen = [];
        Element resNode = visit(seen, elem, data);
        res.appendChild(resNode);
    }

    private void make(Element elem, JsonArray data, Element res) {
        for (int i = 0; i < data.size(); i++) {
            make(elem, data[i], res);
        }
    }

    private void make(Element elem, JsonString txt, Element res) {
        Node txtWrapperNode = elem.cloneNode(false);
        Node txtNode = doc.createTextNode(txt.string);
        txtWrapperNode.appendChild(txtNode);
        res.appendChild(txtWrapperNode);
    }

    private void makeRich(Element elem, JsonValue data, Element res) {
        Node wrapperNode = elem.cloneNode(false);
        Node node = jsonToHtml(data, wrapperNode);
        res.appendChild(wrapperNode);
    }

    /* for testing only */
    public Node makeHtml(JsonValue v) {
        Node rootNode = doc.createElement("root");
        jsonToHtml(v, rootNode);
        return rootNode;
    }

    private void jsonToHtml(JsonString val, Node ctxt) {
        Node txtNode = doc.createTextNode(val.string);
        ctxt.appendChild(txtNode);
    }

    private void jsonToHtml(JsonArray vals, Node ctxt) {
        for (int i = 0; i < vals.size(); i++) {
            jsonToHtml(vals[i], ctxt);
        }
    }

    private void jsonToHtml(JsonObject vals, Node ctxt) {
        /* check Json only contains the expected keys. */
        assert vals.containsKey("name");
        int z = 1;
        if (vals.containsKey("attributes")) {
            z++;
        }
        if (vals.containsKey("children")) {
            z++;
        }
        assert vals.size() == z;

        Element e = doc.createElement(vals.getString("name"));
        ctxt.appendChild(e);

        if (vals.containsKey("attributes")) {
            JsonObject attrs = vals.getJsonObject("attributes");
            for (String attrName : attrs.keySet()) {
                e.setAttribute(attrName, attrs.getString(attrName));
            }
        }

        if (vals.containsKey("children")) {
            JsonArray kids = vals.getJsonArray("children");
            for (int i = 0; i < kids.size(); i++) {
                jsonToHtml(kids[i], e);
            }
        }
    }

    public JsonValue htmlToJson(Element elem) {
        JsonObjectBuilder resBld = Json.createObjectBuilder();
        resBld.add("name", elem.nodeName);
        NamedNodeMap attrs = elem.getAttributes();
        if (attrs.length > 0) {
            JsonObjectBuilder resAttrsBld = Json.createObjectBuilder();
            for (int i = 0; i < attrs.length; i++) {
                Node attr = attrs.item(i);
                resAttrsBld.add(attr.nodeName, attr.nodeValue);
            }
            resBld.add("attributes", resAttrsBld.build());
        }
        NodeList kids = elem.childNodes;
        if (kids.length > 0) {
            JsonArrayBuilder resKidsBld =  Json.createArrayBuilder();
            for (int i = 0; i < kids.length; i++) {
                JsonValue kidRes = htmlToJson(kids.item(i));
                resKidsBld.add(kidRes);
            }
            resBld.add("children", resKidsBld.build());
        }
        return resBld.build();
    }

    public JsonValue htmlToJson(Text txt) {
        return Json.createValue(txt.nodeValue);
    }

    public static void main(String[] args) {
        String filename = args[0];
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder(); 
        Document doc = db.parse(new File(filename));

        Rabble r = new Rabble(doc, doc.documentElement);

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
}
