package org.petermac.rabble;

import java.io.StringWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonValue;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

class Utils {
    public static final <T> List<T> collect(T... items) {
        List<T> res = new ArrayList<T>();
        for (T item : items) {
            res.add(item);
        }
        return res;
    }

    public static final String lines(String... ss) {
        List<String> ls = collect(ss);
        return String.join("\n", ls);
    }

    public static final <T> Set<T> newHashSet(T... items) {
        return new HashSet<T>(collect(items));
    }

    public static final Document makeDoc(String... ss) throws Exception {
        String docText = lines(ss);
        return textToDoc(docText);
    }

    public static final Document textToDoc(String docText) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder(); 
        return db.parse(IOUtils.toInputStream(docText));
    }

    public static final JsonValue makeJson(String... lines) throws Exception {
        List<String> ls = collect(lines);
        String jsonText = String.join("\n", ls);
        return textToJson(jsonText);
    }

    public static final JsonValue textToJson(String jsonText) throws Exception {
        JsonReader reader = Json.createReader(IOUtils.toInputStream(jsonText));
        return reader.read();
    }

    public static final String serialize(Node node) {
        DOMImplementationLS domImplLS = (DOMImplementationLS) node.getOwnerDocument().getImplementation();
        LSSerializer lsSerializer = domImplLS.createLSSerializer();
        DOMConfiguration config = lsSerializer.getDomConfig();
        config.setParameter("xml-declaration", false);

        LSOutput lsOutput =  domImplLS.createLSOutput();
        lsOutput.setEncoding("UTF-8");
        Writer stringWriter = new StringWriter();
        lsOutput.setCharacterStream(stringWriter);
        lsSerializer.write(node, lsOutput);     

        return stringWriter.toString();
    }
}
