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

@Command(description = "apply data to a template or extract data from a template instantiation",
         name = "apply")
class RabbleApplyCli implements Callable<Integer> {

    @Option(names = {"-V", "--version"},
            versionHelp = true,
            description = "display version information")
    public boolean versionRequested;

    @Option(names = {"-h", "--help"},
            usageHelp = true,
            description = "display usage information")
    public boolean helpRequested;

    @Parameters(index="0",
            description = "html template file")
    public String template;

    @Parameters(index="1..*", arity="0..1",
            description = "JSON data with which to populate the template")
    public String data;

    @Override
    public Integer call() throws Exception {

        if (template == null && data == null) {
            System.err.println("no template or data to check");
            return 1;
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder(); 

        Document doc = db.parse(new File(template));
        Rabble rabble = new Rabble(doc, doc.getDocumentElement());

        if (data == null) {
            JsonValue json = rabble.extract(doc.getDocumentElement());
            Json.createWriter(System.out).write(json);
            System.out.println();
        } else {
            JsonReader reader = Json.createReader(new FileReader(data));
            JsonValue json = reader.read();
            if (json.getValueType() != JsonValue.ValueType.OBJECT) {
                System.err.println("JSON data must be an object at the top-level");
                return 1;
            }
            JsonObject blob = (JsonObject) json;
            Element e = rabble.instantiate(blob);
            System.out.print(Utils.serialize(e));
        }

        return 0; // success!
    }

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new RabbleTestCli()).execute(args);
        System.exit(exitCode);
    }
}


