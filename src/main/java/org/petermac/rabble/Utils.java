package org.petermac.rabble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;

import org.w3c.dom.Document;

class Utils {
    public static final <T> List<T> collect(T... items) {
        List<T> res = new ArrayList<T>();
        for (T item : items) {
            res.add(item);
        }
        return res;
    }

    public static final <T> Set<T> newHashSet(T... items) {
        return new HashSet<T>(collect(items));
    }

    public static final Document makeDoc(String... lines) throws Exception {
        List<String> ls = collect(lines);
        String docText = String.join("\n", ls);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder(); 
        return db.parse(IOUtils.toInputStream(docText));
    }
}
