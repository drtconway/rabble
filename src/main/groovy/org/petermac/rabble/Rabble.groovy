package org.petermac.rabble

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonArray;
import javax.json.JsonObject;
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
                } else {
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

    public static void main(String[] args) {
        String filename = args[0];
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder(); 
        Document doc = db.parse(new File(filename));

        Rabble r = new Rabble(doc, doc.documentElement);

        JsonReader reader = Json.createReader(new FileReader(args[1]));
        JsonValue dat = reader.read();

        Element e = r.instantiate(dat);
        DOMImplementationLS domImplLS = (DOMImplementationLS) doc.getImplementation();
        LSSerializer serializer = domImplLS.createLSSerializer();
        String str = serializer.writeToString(e);
        println(str);
    }
}
