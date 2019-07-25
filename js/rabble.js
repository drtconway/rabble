
if (typeof Node === 'undefined') {
    Node = {}
    Node.ELEMENT_NODE = 1
    Node.TEXT_NODE = 3
    Node.CDATA_SECTION_NODE = 4
    Node.PROCESSING_INSTRUCTION_NODE = 7
    Node.COMMENT_NODE = 8
    Node.DOCUMENT_NODE = 9
    Node.DOCUMENT_TYPE_NODE = 10
    Node.DOCUMENT_FRAGMENT_NODE = 11
    Node.ATTRIBUTE_NODE = 2
    Node.ENTITY_REFERENCE_NODE = 5
    Node.ENTITY_NODE = 6
    Node.NOTATION_NODE = 12
}

class Rabble {

    constructor(doc, root) {
        this.doc = doc;
        this.root = root;
    }

    instantiate(data) {
        let seen = new Set([]);
        return this.visit(seen, this.root, data);
    }

    visit(seen, node, vals) {
        let res = node.cloneNode(false);
        let kids = node.childNodes;
        for (let i = 0; i < kids.length; i++) {
            let kid = kids[i];
            if (kid.nodeType == Node.ELEMENT_NODE) {
                let tn = null;
                if (kid.hasAttribute('data-rabble-name')) {
                    tn = kid.getAttribute('data-rabble-name');
                }
                let tk = "text";
                if (kid.hasAttribute('data-rabble-kind')) {
                    tk = kid.getAttribute('data-rabble-kind');
                }
                if (tn == null)
                {
                    let resKid = this.visit(seen, kid, vals);
                    res.appendChild(resKid);
                    continue;
                }

                if (seen.has(tn)) {
                    continue;
                }
                if (vals[tn] == null) {
                    continue;
                }
                seen.add(tn);

                switch (tk) {
                    case "group":
                        this.makeGroup(kid, vals[tn], res);
                        break;
                    case "rich":
                        this.makeRich(kid, vals[tn], res);
                        break;
                    case "lines":
                        this.makeLines(kid, vals[tn], res);
                        break;
                    case "text":
                        this.makeText(kid, vals[tn], res);
                        break;
                    default:
                        console.log("unknown data-rabble-kind: " + tk);
                        break;
                }
            } else {
                /* Not an element. */
                let resKid = kid.cloneNode(false);
                res.appendChild(resKid);
            }
        }
        return res;
    }

    makeGroup(node, value, res) {
        if (value instanceof Array) {
            for (let i = 0; i < value.length; i++) {
                this.makeGroup(node, value[i], res);
            }
            return;
        }
        if (value instanceof Object) {
            let seen = new Set([]);
            let resNode = this.visit(seen, node, value);
            res.appendChild(resNode);
            return;
        }
        console.log("makeGroup: unexpected data type");
    }

    makeRich(node, value, res) {
        let wrapperNode = node.cloneNode(false);
        this.jsonToHtml(value, wrapperNode);
        res.appendChild(wrapperNode);
    }

    makeLines(node, value, res) {
        let txtWrapperNode = node.cloneNode(false);
        res.appendChild(txtWrapperNode);
        if (value instanceof Array) {
            for (let i = 0; i < value.length; i++) {
                let lineDiv = this.doc.createElement("div");
                txtWrapperNode.appendChild(lineDiv);
                let txtNode = this.doc.createTextNode(value[i]);
                lineDiv.appendChild(txtNode);
            }
        } else {
            let lineDiv = this.doc.createElement("div");
            txtWrapperNode.appendChild(lineDiv);
            let txtNode = this.doc.createTextNode(value);
            lineDiv.appendChild(txtNode);
        }
    }

    makeText(node, value, res) {
        if (value instanceof Array) {
            for (let i = 0; i < value.length; i++) {
                this.makeText(node, value[i], res);
            }
        } else {
            let txtWrapperNode = node.cloneNode(false);
            res.appendChild(txtWrapperNode);
            let txtNode = this.doc.createTextNode(value);
            txtWrapperNode.appendChild(txtNode);
        }
    }

    extract() {
        var data = {}
        this.revisit(this.root, data);
        return this.buildJson(data);
    }

    revisit(node, data) {
        let kids = node.childNodes;
        for (let i = 0; i < kids.length; i++) {
            let kid = kids[i];
            if (kid.nodeType == Node.ELEMENT_NODE) {
                let tn = null;
                if (kid.hasAttribute('data-rabble-name')) {
                    tn = kid.getAttribute('data-rabble-name');
                }
                let tk = "text";
                if (kid.hasAttribute('data-rabble-kind')) {
                    tk = kid.getAttribute('data-rabble-kind');
                }
                if (tn == null)
                {
                    this.revisit(kid, data);
                    continue;
                }
                switch (tk) {
                    case "group":
                        let kidData = {};
                        this.revisit(kid, kidData);
                        kidData = this.buildJson(kidData);
                        this.addExtracted(tn, kidData, data);
                        break;
                    case "rich":
                        this.extractRich(kid, tn, data);
                        break;
                    case "lines":
                        this.extractLines(kid, tn, data);
                        break;
                    case "text":
                        this.extractText(kid, tn, data);
                        break;
                    default:
                        console.log('extract: unknown template kind ' + tk);
                }
            }
        }
    }

    extractRich(node, name, data) {
        let kids = node.childNodes;
        for (let i = 0; i < kids.length; i++) {
            let kidData = this.htmlToJson(kids[i]);
            this.addExtracted(name, kidData, data);
        }
    }

    extractLines(node, name, data) {
        let lines = []
        let kids = node.childNodes;
        for (let i = 0; i < kids.length; i++) {
            let kid = kids[i];
            switch (kid.nodeType) {
                case Node.ELEMENT_NODE:
                    if (kid.nodeName != 'div') {
                        console.log('only div elements are permitted in "lines" templates (' + kid.nodeName + ')');
                        break;
                    }
                    let grandkids = kid.childNodes;
                    for (let j = 0; j < grandkids.length; j++) {
                        let grandkid = grandkids[j];
                        switch (grandkid.nodeType) {
                            case Node.TEXT_NODE:
                                lines.push(grandkid.nodeValue);
                                break;
                            default:
                                console.log('only text nodes are allowed in lines div elements');
                        }
                    }
                    break;
                case Node.TEXT_NODE:
                    lines.push(kid.nodeValue);
                    break;
            }
        }
        this.addExtracted(name, lines, data);
    }

    extractText(node, name, data) {
        let kids = node.childNodes;
        for (let i = 0; i < kids.length; i++) {
            let kid = kids[i];
            switch (kid.nodeType) {
                case Node.TEXT_NODE:
                    this.addExtracted(name, kid.nodeValue, data);
                    break;
                default:
                    console.log('only text nodes are expected in text templates');
            }
        }
    }

    addExtracted(name, value, data) {
        if (data[name] == null) {
            data[name] = [value];
        } else {
            data[name].push(value);
        }
    }

    buildJson(data) {
        let ks = Object.keys(data);
        for (let i = 0; i < ks.length; i++) {
            let k = ks[i];
            let vs = data[k];
            if (vs.length == 1) {
                data[k] = vs[0];
            }
        }
        return data;
    }

    jsonToHtml(value, ctxt) {
        if (value instanceof Array) {
            for (let i = 0; i < value.length; i++) {
                this.jsonToHtml(value[i], ctxt);
            }
            return;
        }
        if (value instanceof Object) {
            // assert Object.keys(value).length == 1
            let tag = Object.keys(value)[0];
            let resNode = this.doc.createElement(tag);
            ctxt.appendChild(resNode);
            let kidVal = value[tag];
            if (kidVal instanceof Array) {
                if (kidVal.length == 0) {
                    return;
                }
                let i0 = 0;
                if (kidVal[0] instanceof Object && kidVal[0]["*"] != null) {
                    let attrs = kidVal[0]["*"];
                    for (let k in attrs) {
                        let v = attrs[k];
                        resNode.setAttribute(k, v);
                    }
                    i0 = 1;
                }
                for (let i = i0; i < kidVal.length; i++) {
                    this.jsonToHtml(kidVal[i], resNode);
                }
            } else {
                this.jsonToHtml(kidVal, resNode);
            }
            return;
        }
        // String
        let txtNode = this.doc.createTextNode(value);
        ctxt.appendChild(txtNode);
    }

    htmlToJson(node) {
        switch (node.nodeType) {
            case Node.ELEMENT_NODE:
                let tag = node.nodeName;
                let kids = []
                if (node.hasAttributes()) {
                    let attrs = node.attributes;
                    let attrObj = {};
                    for (let i = 0; i < attrs.length; i++) {
                        attrObj[attrs[i].nodeName] = attrs[i].nodeValue;
                    }
                    kids.push({'*': attrObj});
                }
                for (let i = 0; i < node.childNodes.length; i++) {
                    kids.push(this.htmlToJson(node.childNodes[i]));
                }
                if (kids.length == 1) {
                    kids = kids[0];
                }
                let res = {}
                res[tag] = kids;
                return res;
            case Node.TEXT_NODE:
                return node.nodeValue;
            default:
                console.log('htmlToJson: unexpected node type');
        }
    }
}

exports.Rabble = Rabble;
