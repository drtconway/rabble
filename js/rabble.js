
if (!Node) {
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
                if (kid.hasAttribute('data-rabble')) {
                    let tn = kid.getAttribute('data-rabble');
                    if (seen.has(tn)) {
                        continue;
                    }
                    if (vals[tn] == null) {
                        continue;
                    }
                    seen.add(tn);
                    let resKids = this.make(kid, vals[tn]);
                    for (let j = 0; j < resKids.length; j++) {
                        res.appendChild(resKids[j]);
                    }
                } else {
                    /* Not a template node. */
                    let resKid = this.visit(seen, kid, vals);
                    res.appendChild(resKid);
                }
            } else {
                /* Not an element. */
                let resKid = kid.cloneNode(false);
                res.appendChild(resKid);
            }
        }
        return res;
    }

    make(node, value) {
        if (value instanceof Array) {
            let res = []
            for (let i = 0; i < value.length; i++) {
                let res0 = this.make(node, value[i]);
                for (let j = 0; j < res0.length; j++) {
                    res.push(res0[j]);
                }
            }
            return res;
        }

        if (value instanceof Object) {
            let seen = new Set([]);
            return [this.visit(seen, node, value)]
        }

        let res = node.cloneNode(false);
        let txt = this.doc.createTextNode(value);
        res.appendChild(txt);
        return [res];
    }

    extract() {
        var data = {}
        this.revisit(this.root, data);
        return this.simplify(data);
        //return data;
    }

    revisit(node, data) {
        let kids = node.childNodes;
        for (let i = 0; i < kids.length; i++) {
            let kid = kids[i];
            if (kid.nodeType == Node.ELEMENT_NODE) {
                if (kid.hasAttribute('data-rabble')) {
                    let tn = kid.getAttribute('data-rabble');
                    let vals = this.contentof(kid);
                    //console.log(tn + " <- " + JSON.stringify(vals));
                    for (let j = 0; j < vals.length; j++) {
                        if (data[tn] == null) {
                            data[tn] = [];
                        }
                        data[tn].push(vals[j]);
                    }
                } else {
                    /* Not a template node. */
                    let resKid = this.revisit(kid, data);
                }
            }
        }
    }

    contentof(node) {
        let kids = node.childNodes;
        let anyElems = false;
        for (let i = 0; i < kids.length; i++) {
            let kid = kids[i];
            if (kid.nodeType == Node.ELEMENT_NODE) {
                anyElems = true;
                break;
            }
        }

        if (anyElems) {
            let data = {}
            this.revisit(node, data);
            return [data];
        }

        let vals = []
        for (let i = 0; i < kids.length; i++) {
            let kid = kids[i];
            let val = kid.textContent;
            if (/[^\t\n\r ]/.test(val)) {
                vals.push(val);
            }
        }
        return vals;
    }

    simplify(value) {
        if (value instanceof Array) {
            if (value.length == 1) {
                return this.simplify(value[0]);
            }
            let res = []
            for (let i = 0; i < value.length; i++) {
                res.push(this.simplify(value[i]));
            }
            return res;
        }

        if (value instanceof Object) {
            let res = {}
            for (let key in value) {
                res[key] = this.simplify(value[key]);
            }
            return res;
        }

        return value;
    }
}

//exports.Rabble = Rabble;
