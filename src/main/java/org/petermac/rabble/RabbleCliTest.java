package org.petermac.rabble;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

import java.util.concurrent.Callable;

import java.util.Set;
import java.util.HashSet;

import javax.json.Json;
import javax.json.JsonReader;
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

    private void checkNode(Node node, String ctxt,
                           String path, Set<String> allPaths, Set<String> terminalPaths,
                           boolean inRich, boolean inText) {
        assert !(inRich && inText);
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                if (inText) {
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

                if (inRich) {
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
                    allPaths.add(kidPath);
                }
                else if (richName != null) {
                    if (textName != null) {
                        warn(ctxt, "nodes may not be both rich nodes and text nodes");
                    }
                    kidInRich = true;
                    String termPath = path + "/" + richName;
                    allPaths.add(termPath);
                    terminalPaths.add(termPath);
                }
                else if (textName != null) {
                    kidInText = true;
                    String termPath = path + "/" + textName;
                    allPaths.add(termPath);
                    terminalPaths.add(termPath);
                }
                else if (hasEdit) {
                    warn(ctxt, "nodes without rich or text annotations should not be marked as editable.");
                }

                NodeList kids = elem.getChildNodes();
                for (int i = 0; i < kids.getLength(); i++) {
                    checkNode(kids.item(i), elemCtxt, kidPath, allPaths, terminalPaths, kidInRich, kidInText);
                }
                break;
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

        Document doc;
        if (template != null) {
            doc = db.parse(new File(template));
            Set<String> allPaths = new HashSet<String>();
            Set<String> terminalPaths = new HashSet<String>();
            checkNode(doc.getDocumentElement(), "#", "#", allPaths, terminalPaths, false, false);

            for (String p : allPaths) {
                if (terminalPaths.contains(p)) {
                    continue;
                }
                boolean found = false;
                for (String t : terminalPaths) {
                    if (t.startsWith(p)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    System.out.printf("group has no text/rich children: %s\n", p);
                }
            }
        }

        JsonValue blob;
        if (data != null) {
            JsonReader reader = Json.createReader(new FileReader(data));
            blob = reader.read();
        }

        return 0; // success!
    }

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new RabbleCli()).execute(args);
        System.exit(exitCode);
    }
}

