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
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
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

    public static final Map newHashMap(Object... items) {
        HashMap res = new HashMap();
        List itemList = collect(items);
        assert itemList.size() % 2 == 0;
        for (int i = 0; i < itemList.size(); i += 2) {
            res.put(itemList.get(i), itemList.get(i+1));
        }
        return res;
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

    public static final JsonValue pojoToJson(Object obj) throws Exception {
        if (obj instanceof String) {
            return Json.createValue((String) obj);
        }
        if (obj instanceof Integer) {
            return Json.createValue((Integer) obj);
        }
        if (obj instanceof Double) {
            return Json.createValue((Double) obj);
        }
        if (obj instanceof List) {
            JsonArrayBuilder resBld = Json.createArrayBuilder();
            for (Object itm : (List) obj) {
                resBld.add(pojoToJson(itm));
            }
            return resBld.build();
        }
        if (obj instanceof Map) {
            Map<String,Object> map = (Map<String,Object>)obj;
            JsonObjectBuilder resBld = Json.createObjectBuilder();
            for (String key : map.keySet()) {
                resBld.add(key, pojoToJson(map.get(key)));
            }
            return resBld.build();
        }
        throw new Exception("pojoToJson: unexpected pojo type");
    }
}
