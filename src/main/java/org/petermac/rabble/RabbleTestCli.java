package org.petermac.rabble;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

import java.util.concurrent.Callable;

import java.util.Set;
import java.util.HashSet;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(description = "check a template or data-blob for integrity",
         name = "test",
         aliases = {"check"})
class RabbleTestCli implements Callable<Integer> {

    @Option(names = {"-V", "--version"},
            versionHelp = true,
            description = "display version information")
    public boolean versionRequested;

    @Option(names = {"-h", "--help"},
            usageHelp = true,
            description = "display usage information")
    public boolean helpRequested;

    @Option(names = {"-t", "--template"},
            description = "html template file")
    public String template;

    @Option(names = {"-d", "--data"},
            description = "JSON data")
    public String data;

    @Option(names = {"-s", "--strict"},
            description = "perform strict checking")
    public boolean strict;

    static class PathSet {
        public Set<String> groupPaths;
        public Set<String> richPaths;
        public Set<String> linesPaths;
        public Set<String> textPaths;

        PathSet() {
            groupPaths = new HashSet<String>();
            richPaths = new HashSet<String>();
            linesPaths = new HashSet<String>();
            textPaths = new HashSet<String>();
        }

        public boolean contains(String path) {
            return groupPaths.contains(path) || richPaths.contains(path) || textPaths.contains(path);
        }
    }

    private void checkNode(Node node, String ctxt, String path, PathSet paths) {
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                if (paths.textPaths.contains(path)) {
                    error(ctxt, "element %s is inside a context marked for text only.", node.getNodeName());
                    return;
                }

                Element elem = (Element) node;
                String elemCtxt = ctxt + "/" + elem.getNodeName();

                NamedNodeMap attrs = elem.getAttributes();
                boolean hasRabble = false;
                boolean hasEdit = false;
                String name = null;
                String kind = "text";
                for (int i = 0; i < attrs.getLength(); i++) {
                    Node attr = attrs.item(i);
                    String attrName = attr.getNodeName();
                    String attrValue = attr.getNodeValue();
                    if (attrName.startsWith("data-rabble")) {
                        hasRabble = true;
                        switch (attrName) {
                            case "data-rabble-name":
                                name = attrValue;
                                break;
                            case "data-rabble-kind":
                                kind = attrValue;
                                break;
                            case "data-rabble-edit":
                                hasEdit = true;
                                break;
                            default:
                                warn(ctxt, "attribute %s is in the rabble namespace, but is not a recognised rabble attribute", attrName);
                                break;
                        }
                    }
                }

                if (paths.richPaths.contains(path) && hasRabble) {
                    error(ctxt, "elements inside a rich node must not have data-rabble attributes.");
                    return;
                }

                NodeList kids = elem.getChildNodes();

                if (name != null) {
                    // We have a template node, so check if it makes sense.
                    //
                    switch (kind) {
                        case "group":
                            if (hasEdit) {
                                warn(ctxt, "group nodes are not editable");
                            }
                            String kidPath = path + "/" + name;
                            paths.groupPaths.add(kidPath);

                            for (int i = 0; i < kids.getLength(); i++) {
                                checkNode(kids.item(i), elemCtxt, kidPath, paths);
                            }
                            break;
                        case "rich":
                            String richPath = path + "/" + name;
                            paths.richPaths.add(richPath);

                            for (int i = 0; i < kids.getLength(); i++) {
                                checkRichNode(kids.item(i), elemCtxt, richPath);
                            }
                            break;
                        case "lines":
                            String linesPath = path + "/" + name;
                            paths.linesPaths.add(linesPath);

                            for (int i = 0; i < kids.getLength(); i++) {
                                checkLinesNode(kids.item(i), elemCtxt, linesPath);
                            }
                            break;
                        case "text":
                            String textPath = path + "/" + name;
                            paths.textPaths.add(textPath);

                            for (int i = 0; i < kids.getLength(); i++) {
                                Node kid = kids.item(i);
                                if (kid.getNodeType() == Node.ELEMENT_NODE) {
                                    error(ctxt, "text templates my not have element children - %s", kid.getNodeName());
                                }
                            }
                            break;
                        default:
                            error(ctxt, "unknown kind %s", kind);
                            break;
                    }
                } else {
                    // Not a template node.
                    //
                    if (hasEdit) {
                        warn(ctxt, "nodes without a name annotation should not be marked as editable.");
                    }
                    for (int i = 0; i < kids.getLength(); i++) {
                        checkNode(kids.item(i), elemCtxt, path, paths);
                    }
                }

                break;
        }
    }

    private void checkRichNode(Node node, String ctxt, String path) {
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                Element elem = (Element) node;
                String elemCtxt = ctxt + "/" + elem.getNodeName();

                NamedNodeMap attrs = elem.getAttributes();
                for (int i = 0; i < attrs.getLength(); i++) {
                    Node attr = attrs.item(i);
                    String attrName = attr.getNodeName();
                    String attrValue = attr.getNodeValue();
                    if (attrName.startsWith("data-rabble")) {
                        error(ctxt, "rich templates may not contain rabble attributes - %s", attrName);
                    }
                }

                NodeList kids = elem.getChildNodes();
                for (int i = 0; i < kids.getLength(); i++) {
                    checkRichNode(kids.item(i), elemCtxt, path);
                }
                break;
        }
    }

    private void checkLinesNode(Node node, String ctxt, String path) {
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                // If it's an element, it had better be a DIV with no element children.
                Element elem = (Element) node;

                if (node.getNodeName() != "div") {
                    error(ctxt, "lines templetes may only contain div elements - %s", node.getNodeName());
                    return;
                }

                // Better not have rabble attributes.
                NamedNodeMap attrs = elem.getAttributes();
                for (int i = 0; i < attrs.getLength(); i++) {
                    Node attr = attrs.item(i);
                    String attrName = attr.getNodeName();
                    String attrValue = attr.getNodeValue();
                    if (attrName.startsWith("data-rabble")) {
                        error(ctxt, "lines templates may not contain rabble attributes - %s", attrName);
                    }

                }

                NodeList kids = elem.getChildNodes();
                for (int i = 0; i < kids.getLength(); i++) {
                    Node kid = kids.item(i);
                    if (kid.getNodeType() == Node.ELEMENT_NODE) {
                        error(ctxt, "lines templates may only contain div/text content - %s", kid.getNodeName());
                    }
                }
                break;
        }
    }

    private void checkJson(JsonValue data, String path, PathSet paths) throws Exception {
        switch (data.getValueType()) {
            case ARRAY:
                JsonArray array = (JsonArray) data;
                for (int i = 0; i < array.size(); i++) {
                    checkJson(array.get(i), path, paths);
                }
                break;
            case OBJECT:
                JsonObject object = (JsonObject) data;
                if (paths.richPaths.contains(path)) {
                    checkRichJson(path, object);
                    break;
                }
                if (strict && !paths.groupPaths.contains(path)) {
                    error(path, "the template does not support an object in this context");
                }
                if (paths.textPaths.contains(path)) {
                    error(path, "the template requires this path to contain text, not an object");
                }
                for (String key : object.keySet()) {
                    String kidPath = path + "/" + key;
                    if (!paths.contains(kidPath)) {
                        warn(kidPath, "the template does not contain this path");
                        continue;
                    }
                    checkJson(object.get(kidPath), kidPath, paths);
                }
                break;
            case STRING:
                JsonString txt = (JsonString) data;
                if (paths.groupPaths.contains(path)) {
                    error(path, "this path is expect to contain an object, not text");
                }
                break;
            default:
                throw new Exception("checkJson: unexpected value type");
        }
    }

    private void checkRichJson(String path, JsonObject object) {
        if (!object.containsKey("name")) {
            error(path, "json encoded html must have a name member");
            return;
        }
        for (String key : object.keySet()) {
            switch (key) {
                case "name":
                    break;
                case "attributes":
                    JsonObject attrs = object.getJsonObject(key);
                    for (String attrName : attrs.keySet()) {
                        JsonValue attrVal = attrs.get(attrName);
                        if (attrVal.getValueType() != JsonValue.ValueType.STRING) {
                            error(path, "json encoded html attribute values must be strings");
                        }
                    }
                    break;
                case "children":
                    JsonArray kids = object.getJsonArray("children");
                    for (int i = 0; i < kids.size(); i++) {
                        JsonValue kid = kids.get(i);
                        if (kid.getValueType() == JsonValue.ValueType.OBJECT) {
                            JsonObject kidObj = (JsonObject) kid;
                            checkRichJson(path, kidObj);
                        }
                    }
                    break;
                default:
                    error(path, "json encoded html contains unexpected structure key %s", key);
                    break;
            }
        }
    }

    private void warn(String ctxt, String fmt, Object... args) {
        System.out.println("warning: in context " + ctxt);
        System.out.println("warning: " + String.format(fmt, args));
    }

    private void error(String ctxt, String fmt, Object... args) {
        System.out.println("error: in context " + ctxt);
        System.out.println("error: " + String.format(fmt, args));
    }

    @Override
    public Integer call() throws Exception {

        if (template == null && data == null) {
            System.err.println("no template or data to check");
            return 1;
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder(); 

        Document doc = null;
        PathSet paths = new PathSet();
        if (template != null) {
            doc = db.parse(new File(template));
            checkNode(doc.getDocumentElement(), "#", "#", paths);

            if (strict) {
                for (String p : paths.groupPaths) {
                    boolean found = false;
                    if (!found) {
                        for (String t : paths.richPaths) {
                            if (t.startsWith(p)) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        for (String t : paths.textPaths) {
                            if (t.startsWith(p)) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        System.out.printf("group has no text/rich children: %s\n", p);
                    }
                }
            }
        }

        JsonValue blob = null;
        if (data != null) {
            JsonReader reader = Json.createReader(new FileReader(data));
            blob = reader.read();
        }

        if (doc != null && blob != null) {
            checkJson(blob, "#", paths);
        }

        return 0; // success!
    }

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new RabbleTestCli()).execute(args);
        System.exit(exitCode);
    }
}

