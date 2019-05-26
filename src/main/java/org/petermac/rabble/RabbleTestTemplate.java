package org.petermac.rabble;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

class RabbleTestTemplate {
    public static class Message {
        public String level;
        public String context;
        public String path;
        public String message;

        Message(String level, String context, String path, String message) {
            this.level = level;
            this.context = context;
            this.path = path;
            this.message = message;
        }
    }

    public static class Report {
        public Map<String,Set<String>> paths;
        public List<Message> problems;

        Report(Map<String,Set<String>> paths, List<Message> problems) {
            this.paths = paths;
            this.problems = problems;
        }

        public void dumpPaths() {
            System.out.println("--->");
            for (String kind : paths.keySet()) {
                System.out.printf("%s: %d\n", kind, paths.get(kind).size());
                for (String pth : paths.get(kind)) {
                    System.out.printf("%s: %s\n", kind, pth);
                }
            }
            System.out.println("<---");
        }

    }

    private final Document template;
    private Map<String,Set<String>> paths;
    private Map<String,String> pathIdx;
    private List<Message> problems;

    private RabbleTestTemplate(final Document template) {
        this.template = template;

        paths = new HashMap<String,Set<String>>();
        paths.put("group", new HashSet<String>());
        paths.put("rich", new HashSet<String>());
        paths.put("lines", new HashSet<String>());
        paths.put("text", new HashSet<String>());

        pathIdx = new HashMap<String,String>();

        problems = new ArrayList<Message>();
    }

    public static Report check(Document template) {
        RabbleTestTemplate rtt = new RabbleTestTemplate(template);

        rtt.checkNode(template.getDocumentElement(), "#", "#");
        rtt.checkOrphans();

        Report res = new Report(rtt.paths, rtt.problems);

        return res;
    }

    private void checkNode(Node node, String ctxt, String path) {
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
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
                                warn(ctxt, path, "attribute %s is in the rabble namespace, but is not a recognised rabble attribute", attrName);
                                break;
                        }
                    }
                }

                NodeList kids = elem.getChildNodes();

                if (name != null) {
                    // We have a template node, so check if it makes sense.
                    //
                    String kidPath = path + "/" + name;
                    switch (kind) {
                        case "group":
                            if (hasEdit) {
                                warn(ctxt, path, "group nodes are not editable");
                            }
                            addPath(ctxt, kind, kidPath);

                            for (int i = 0; i < kids.getLength(); i++) {
                                checkNode(kids.item(i), elemCtxt, kidPath);
                            }
                            break;
                        case "rich":
                            addPath(ctxt, kind, kidPath);
                            for (int i = 0; i < kids.getLength(); i++) {
                                checkRichNode(kids.item(i), elemCtxt, kidPath);
                            }
                            break;
                        case "lines":
                            addPath(ctxt, kind, kidPath);
                            for (int i = 0; i < kids.getLength(); i++) {
                                checkLinesNode(kids.item(i), elemCtxt, kidPath);
                            }
                            break;
                        case "text":
                            addPath(ctxt, kind, kidPath);
                            for (int i = 0; i < kids.getLength(); i++) {
                                Node kid = kids.item(i);
                                if (kid.getNodeType() == Node.ELEMENT_NODE) {
                                    error(ctxt, kidPath, "text templates my not have element children - %s", kid.getNodeName());
                                }
                            }
                            break;
                        default:
                            error(ctxt, path, "unknown kind %s", kind);
                            break;
                    }
                } else {
                    // Not a template node.
                    //
                    if (hasEdit) {
                        warn(ctxt, path, "nodes without a name annotation should not be marked as editable.");
                    }
                    for (int i = 0; i < kids.getLength(); i++) {
                        checkNode(kids.item(i), elemCtxt, path);
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
                        error(ctxt, path, "rich templates may not contain rabble attributes - %s", attrName);
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
                    error(ctxt, path, "lines templates may only contain div elements - %s", node.getNodeName());
                    return;
                }

                // Better not have rabble attributes.
                NamedNodeMap attrs = elem.getAttributes();
                for (int i = 0; i < attrs.getLength(); i++) {
                    Node attr = attrs.item(i);
                    String attrName = attr.getNodeName();
                    String attrValue = attr.getNodeValue();
                    if (attrName.startsWith("data-rabble")) {
                        error(ctxt, path, "lines templates may not contain rabble attributes - %s", attrName);
                    }

                }

                NodeList kids = elem.getChildNodes();
                for (int i = 0; i < kids.getLength(); i++) {
                    Node kid = kids.item(i);
                    if (kid.getNodeType() == Node.ELEMENT_NODE) {
                        error(ctxt, path, "lines templates may only contain div/text content - %s", kid.getNodeName());
                    }
                }
                break;
        }
    }

    private void addPath(String ctxt, String kind, String path) {
        assert paths.containsKey(kind);
        paths.get(kind).add(path);
        if (pathIdx.containsKey(path)) {
            if (kind != pathIdx.get(path)) {
                warn(ctxt, path, "path has two different kinds of instantiation in template: %s, %s", pathIdx.get(path), kind);
            }
        } else {
            pathIdx.put(path, kind);
        }
    }

    private void checkOrphans() {
        Set<String> allTerminals = new HashSet<String>();
        for (String kind : paths.keySet()) {
            if (kind == "group") {
                continue;
            }
            allTerminals.addAll(paths.get(kind));
        }
        Set<String> groups = new HashSet<String>(paths.get("group"));
        for (String ter : allTerminals) {
            if (groups.size() == 0) {
                break;
            }
            String foundGrp = null;
            for (String grp : groups) {
                if (ter.startsWith(grp)) {
                    foundGrp = grp;
                    break;
                }
            }
            if (foundGrp != null) {
                groups.remove(foundGrp);
            }
        }
        for (String grp : groups) {
            warn(null, grp, "group has no concrete instantiation in template");
        }
    }

    private void warn(String ctxt, String path, String fmt, Object... args) {
        problems.add(new Message("warn", ctxt, path, String.format(fmt, args)));
    }

    private void error(String ctxt, String path, String fmt, Object... args) {
        problems.add(new Message("error", ctxt, path, String.format(fmt, args)));
    }

}
