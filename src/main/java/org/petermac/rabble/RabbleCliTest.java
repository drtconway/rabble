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
class RabbleCliTest implements Callable<Integer> {

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
        public Set<String> textPaths;

        PathSet() {
            groupPaths = new HashSet<String>();
            richPaths = new HashSet<String>();
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
                boolean hasEdit = false;
                String groupName = null;
                String richName = null;
                String textName = null;
                for (int i = 0; i < attrs.getLength(); i++) {
                    Node attr = attrs.item(i);
                    String attrName = attr.getNodeName();
                    String attrValue = attr.getNodeValue();
                    if (attrName.startsWith("data-rabble")) {
                        switch (attrName) {
                            case "data-rabble-group":
                                groupName = attrValue;
                                break;
                            case "data-rabble-rich":
                                richName = attrValue;
                                break;
                            case "data-rabble-text":
                                textName = attrValue;
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

                if (paths.richPaths.contains(path)) {
                    if (groupName != null || richName != null || textName != null || hasEdit) {
                        error(ctxt, "elements inside a rich node must not have data-rabble attributes.");
                        return;
                    }
                }

                String kidPath = path;
                boolean kidInRich = false;
                boolean kidInText = false;
                if (groupName != null) {
                    if (hasEdit) {
                        warn(ctxt, "group nodes are not editable");
                    }
                    if (richName != null || textName != null) {
                        warn(ctxt, "group nodes may not be rich nodes or text nodes");
                    }
                    kidPath = path + "/" + groupName;
                    paths.groupPaths.add(kidPath);
                }
                else if (richName != null) {
                    if (textName != null) {
                        warn(ctxt, "nodes may not be both rich nodes and text nodes");
                    }
                    kidInRich = true;
                    String termPath = path + "/" + richName;
                    paths.richPaths.add(termPath);
                }
                else if (textName != null) {
                    kidInText = true;
                    String termPath = path + "/" + textName;
                    paths.textPaths.add(termPath);
                }
                else if (hasEdit) {
                    warn(ctxt, "nodes without rich or text annotations should not be marked as editable.");
                }

                NodeList kids = elem.getChildNodes();
                for (int i = 0; i < kids.getLength(); i++) {
                    checkNode(kids.item(i), elemCtxt, kidPath, paths);
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
        int exitCode = new CommandLine(new RabbleCli()).execute(args);
        System.exit(exitCode);
    }
}

