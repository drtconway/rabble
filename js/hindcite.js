let buildUrl = require('build-url');
let xpath = require('xpath');
let XmlDom = require('xmldom');
if (typeof XMLHttpRequest === 'undefined') {
    XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
}

class PubmedScraper {

    constructor(config) {
        this.config = config;
    }

    getPubmedRef(pmid) {
        let url = 'https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id=' + pmid;
        if (this.config && this.config.api_key) {
            url = url + '&api_key=' + this.config.api_key;
        } else {
        }
        let req = new XMLHttpRequest();
        req.open('GET', url, false);
        req.send(null);
        let doc = new XmlDom.DOMParser().parseFromString(req.responseText);
        return this.scrapePubmedRef(pmid, doc);
    }

    scrapePubmedRef(pmid, doc) {
        let scraper = this;
        let res = {};
        res['pmid'] = pmid;

        let auths = [];
        this.withNode(doc, '//Article/AuthorList/Author', function(auth) {
            let nameParts = []
            nameParts.push(xpath.select('string(LastName)', auth));
            scraper.withNode(auth, 'Initials', function(node) {
                let ii = node.textContent;
                for (let i = 0; i < ii.length; i++) {
                    nameParts.push(ii[i] + '.');
                }
            });
            auths.push(nameParts.join(' '));
        });
        res['authors'] = auths.join(', ');

        res['title'] = xpath.select('string(//Article/ArticleTitle)', doc);

        let jparts = [];
        this.withNode(doc, '//Article/Journal', function(jnode) {
            scraper.withNode(jnode, './/Title', function(node) {
                jparts.push(node.textContent + '. ');
            });
            scraper.withNode(jnode, './/Year', function(node) {
                jparts.push(node.textContent + ';');
            });
            scraper.withNode(jnode, './/Volume', function(node) {
                jparts.push(node.textContent);
            });
            scraper.withNode(jnode, './/Issue', function(node) {
                jparts.push('(' + node.textContent + ')');
            });
        });
        this.withNode(doc, '//Pagination/MedlinePgn', function(node) {
            jparts.push(':' + node.textContent);
        });
        jparts.push('.');
        res['journal'] = jparts.join('');

        this.withNode(doc, '//Article/ELocationID[@EIdType="doi"]', function(node) {
            res['doi'] = node.textContent;
        });

        return res;
    }

    withNode(ctxt, exprn, func) {
        let result = xpath.evaluate(exprn, ctxt, null, xpath.XPathResult.ANY_TYPE, null);
        let node = result.iterateNext();
        while (node) {
            func(node);
            node = result.iterateNext();
        }
    }
}

class CitationGetter {

    constructor(config) {
        this.config = config;
    }

    pubmedToDoi(pmid) {
        let u = buildUrl('https://www.ncbi.nlm.nih.gov', {
            path: 'pmc/utils/idconv/v1.0/',
            queryParams: {
                tool: 'hindcite',
                email: this.config.email,
                format: 'json',
                idtype: 'pmid',
                ids: pmid
            }
        });
        let req = new XMLHttpRequest();
        req.open('GET', u, false);
        req.send(null);
        let resp = JSON.parse(req.responseText);
        if (!resp.status == 'ok') {
            return null;
        }
        return resp.records[0].doi;
    }

    lookupPubmed(pmid, style) {
        let doi = this.pubmedToDoi(pmid);
        let res = this.lookupDoi(doi, style);
        res.pmid = pmid;
        return res;
    }

    lookupDoi(doi, style) {
        // curl -LH "Accept: text/bibliography; style=mla" https://doi.org/10.1186/s12864-018-5156-1
        //let u = buildUrl('https://doi.org', {path: doi});
        let u = buildUrl('https://data.crossref.org', {path: doi});
        let req = new XMLHttpRequest();
        req.open('GET', u, false);
        req.setRequestHeader('Accept', 'text/bibliography; style=' + style);
        req.send(null);
        let resp = req.responseText.trim();
        let result = {doi: doi, citation: resp};
        return result;
    }
}

/**
 * Class for managing citations and references in an HTML document.
 *
 * Limitations:
 *  - Hindcite only supports 1 citation style: numbered citations, ordered by position of first appearence.
 *  - All references are identified by a PMID. If it doesn't appear on Pubmed, we can't do it! But then, if
 *    it's not on Pubmed, it's not real science. :D
 *
 * The document must contain an element with the id 'hindcite-references' in which the list of cited references is to be placed.
 * The typical structure is to have a heading element followed by the div container:
 *     ...
 *     <h1>References</h1>
 *     <div id='hindcite-references'>
 *     </div>
 *     ...
 *
 * A reference has a fixed structures:
 *     <div data-hindcite-ref id='PMID12345'><span>7.</span>...content...</div>
 *
 * The id attribute gives the Pubmed ID, and the text content of the span gives the label.
 * CSS Styling for the span can be achieved using a selector such as:
 *
 *  div[data-hindcite-ref] span:first-child {
 *      padding-right: 1em;
 *  }
 *
 * Citation groups all occur within a span tagged with a 'data-hindcite-cite' attribute:
 *  <span data-hindcite-cite>...</span>
 *
 * Individual citations themselves are formatted as anchor tags:
 *  <a data-hindcite-key='PMID12345' href='#PMID12345'>7</a>
 *
 * Citation groups (which may be singleton) can be reformatted with the function reformatCitation,
 * which recreates the content of the citation group span element with the following format:
 *  <span data-hindcite-cite>[<a ...>n_1</a>, <a ...>n_2</a>, ...]</span>
 *
 * Additionally, the function parseCitationNode can be used to scan a citation group node for new
 * citations. To do this, the text node children of the citation group are scanned for pubmed ids
 * with the regular expression /PMID:([0-9]+)/, and any matches are replaced with an anchor node
 * with the correct format.
 *
 * To recompile the references, the method recomputeReferences is used. It scans the document looking
 * for citations, and compiles the list of citations in order. It then looks to see which ones are in the
 * document, and if any are missing or unused. If a reference resolver has been supplied, it attempts to
 * look up and missing references. The references div is then repopulated with the references in first-citation
 * order, and any unused ones are added at the end.
 *
 * Unused references have a 'data-hindcite-unused' attribute added, so that unused references may be easily identified,
 * and hidden if desired. To hide unused references, CSS such as the following could be used:
 *  div[data-hindcite-unused] {
 *      display: none;
 *  }
 */
class Hindcite {
    constructor(doc, resolver) {
        this.doc = doc;
        this.resolver = resolver;
        this.refs = doc.getElementById('hindcite-references');
    }

    /**
     * scan the text nodes in a document subtree looking for new citations.
     *
     */
    parseForCitations(node) {
        switch (node.nodeType) {
            case Node.TEXT_NODE: {
                let txt = node.nodeValue;
                let re = /\[PMID:([0-9]+)(, PMID:([0-9]+))*\]/g;
                let parts = [];
                let beg = 0;
                let m = null;
                while (m = re.exec(txt)) {
                    let citeTxt = m[0];
                    let fstTxt = txt.substring(beg, m.index);
                    beg = m.index + citeTxt.length;

                    let citeNode = this.doc.createElement('span');
                    citeNode.setAttribute('data-hindcite-cite', true);
                    citeNode.textContent = citeTxt;
                    this.parseCitationNode(citeNode);

                    if (fstTxt.length > 0) {
                        parts.push(this.doc.createTextNode(fstTxt));
                    }
                    parts.push(citeNode);
                }
                if (parts.length == 0) {
                    return null;
                }
                let lstTxt = txt.substring(beg, txt.length);
                if (lstTxt.length > 0) {
                    parts.push(this.doc.createTextNode(lstTxt));
                }
                return parts;
            }
            break;
            case Node.ELEMENT_NODE: {
                if (node.hasAttribute('data-hindcite-cite')) {
                    this.parseCitationNode(node);
                    return null;
                }
                let kids = node.childNodes;
                let newKids = [];
                let changed = false;
                for (let i = 0; i < kids.length; i++) {
                    let kid = kids[i];
                    let res = this.parseForCitations(kid);
                    if (res == null) {
                        newKids.push(kid.cloneNode(true));
                        continue;
                    }
                    newKids = newKids.concat(res);
                    changed = true;
                }
                if (changed) {
                    this.removeAllChildren(node);
                    for (let i = 0; i < newKids.length; i++) {
                        node.appendChild(newKids[i]);
                    }
                }
            }
            break;
        }
        return null;
    }

    /**
     * Parse a node (usually a span tag) with a 'data-hindcite-cite' tag.
     */
    parseCitationNode(citeNode) {
        if (!citeNode.hasAttribute('data-hindcite-cite')) {
            // not a citation node!
            return;
        }

        let nodes = [];
        for (let i = 0; i < citeNode.childNodes.length; i++) {
            let node = citeNode.childNodes[i];
            if (node.nodeType != Node.TEXT_NODE)  {
                nodes.push(node.cloneNode(true));
                continue;
            }
            let parts = node.textContent.split(/PMID:([0-9]+)/);
            if (parts.length == 1) {
                nodes.push(node.cloneNode(true));
                continue;
            }
            for (let j = 0; j < parts.length; j++) {
                if ((j & 1) == 0) {
                    // Parts 0, 2, ... are text
                    if (parts[j].length > 0) {
                        nodes.push(this.doc.createTextNode(parts[j]));
                    }
                } else {
                    // Parts 1, 3, ... are PMIDs
                    let a = this.doc.createElement('a');
                    let k = 'PMID' + parts[j];
                    a.setAttribute('data-hindcite-key', k);
                    a.setAttribute('href', '#' + k);
                    a.textContent = '?';
                    nodes.push(a);
                }
            }
        }
        this.removeAllChildren(citeNode);
        for (let i = 0; i < nodes.length; i++) {
            citeNode.appendChild(nodes[i]);
        }
    }

    reformatCitation(citeNode) {
        if (!citeNode.hasAttribute('data-hindcite-cite')) {
            // not a citation node!
            return;
        }
        let seen = {};
        let nodes = [];
        for (let i = 0; i < citeNode.childNodes.length; i++) {
            let node = citeNode.childNodes[i];
            if (node.nodeType != Node.ELEMENT_NODE)  {
                continue;
            }
            if (!node.hasAttribute('data-hindcite-key')) {
                continue;
            }
            let k = node.getAttribute('data-hindcite-key');
            if (seen[k] != null) {
                continue;
            }
            nodes.push(node.cloneNode(true));
            seen[k] = node;
        }

        this.removeAllChildren(citeNode);
        citeNode.appendChild(this.doc.createTextNode('['));
        for (let i = 0; i < nodes.length; i++) {
            if (i > 0) {
                citeNode.appendChild(this.doc.createTextNode(', '));
            }
            citeNode.appendChild(nodes[i]);
        }
        citeNode.appendChild(this.doc.createTextNode(']'));
    }

    recomputeReferences() {
        // Slurp up all the citations in the document.
        //
        // We keep them in order in "pmids" so we can number
        // them in order of first apprearence.
        //
        let idx = {};
        let pmids = [];
        let citeNodes = this.getCiteNodes();
        for (let i = 0; i < citeNodes.length; i++) {
            let node = citeNodes[i];
            let pmid = node.getAttribute('data-hindcite-key');
            if (idx[pmid] == null) {
                let n = pmids.length;
                pmids.push(pmid);
                idx[pmid] = n;
            }
            let n = idx[pmid] + 1;
            node.textContent = n;
        }

        // Slurp up all the references.
        //
        let jdx = {};
        let refids = [];
        let refNodes = this.getRefNodes();
        for (let i = 0; i < refNodes.length; i++) {
            let node = refNodes[i];
            let pmid = node.getAttribute('id');
            refids.push(pmid);
            jdx[pmid] = node.cloneNode(true);
        }

        // Figure out which references are missing, and fetch them.
        //
        let needed = [];
        for (let i = 0; i < pmids.length; i++) {
            let pmid = pmids[i];
            if (jdx[pmid] == null) {
                jdx[pmid] = this.makeRefNode(pmid, i);
            }
        }

        // Now remove all the references, and re-attach the ones that have citations.
        //
        this.removeAllChildren(this.refs);
        for (let i = 0; i < pmids.length; i++) {
            let pmid = pmids[i];
            let node = jdx[pmid];
            if (node.hasAttribute('data-hindcite-unused')) {
                node.removeAttribute('data-hindcite-unused');
            }
            this.refs.appendChild(node);
            this.setCitationLabel(i+1 + '.', node);
            delete jdx[pmid];
        }

        // Now reattach all the extra ones and add a 'data-hindcite-unused' attribute
        // so that CSS can be used to hide them if desired.
        //
        let extras = Object.keys(jdx).sort();
        for (let i = 0; i < extras.length; i++) {
            let pmid = extras[i];
            let node = jdx[pmid];
            if (!node.hasAttribute('data-hindcite-unused')) {
                node.setAttribute('data-hindcite-unused', true);
            }
            this.refs.appendChild(node);
            this.setCitationLabel('?.', node);
        }
    }

    getCiteNodes() {
        if (this.doc.querySelectorAll != null) {
            return this.doc.querySelectorAll('a[data-hindcite-key]');
        }

        // Compatability for NodeJS.
        //
        let citeNodes = [];
        let ancNodes = this.doc.getElementsByTagName('a');
        for (let i = 0; i < ancNodes.length; i++) {
            let anc = ancNodes[i];
            if (anc.hasAttribute('data-hindcite-key')) {
                citeNodes.push(anc);
            }
        }
        return citeNodes;
    }

    getRefNodes() {
        if (this.doc.querySelectorAll != null) {
            return this.doc.querySelectorAll('div[data-hindcite-ref]');
        }

        // Compatability for NodeJS.
        //
        let refNodes = [];
        let divNodes = this.doc.getElementsByTagName('div');
        for (let i = 0; i < divNodes.length; i++) {
            let div = divNodes[i];
            if (div.hasAttribute('data-hindcite-ref')) {
                refNodes.push(div);
            }
        }
        return refNodes;
    }

    setCitationLabel(lab, node) {
        console.log('setting citation label: ' + lab + ' -> ' + node.nodeName);
        if (node.nodeName.toLowerCase() == 'div' && node.hasAttribute('data-hindcite-ref')) {
            let kid0 = node.firstChild;
            if (kid0.nodeName.toLowerCase() == 'span') {
                kid0.textContent = lab;
            }
        }
    }

    removeAllChildren(node) {
        while (node.hasChildNodes()) {
            node.removeChild(node.firstChild);
        }
    }

    makeRefNode(pmid, lab) {
        pmid = this.normalizePmid(pmid);
        if (this.resolver) {
            let ref = this.resolver.lookupPubmed(pmid);
            if (ref == null) {
                return this.makeDummyRefNode(pmid, lab);
            }
            let rn = this.doc.createElement('div');
            rn.setAttribute('data-hindcite-ref', 'PMID' + pmid);
            rn.setAttribute('id', 'PMID' + pmid);

            let ln = this.doc.createElement('span');
            ln.textContent = lab;
            rn.appendChild(ln);

            let txt = ref['citation'];
            let tn = this.doc.createTextNode(' ' + txt);
            rn.appendChild(tn);

            if (true) {
                let a = this.doc.createElement('a');
                a.setAttribute('href', 'https://www.ncbi.nlm.nih.gov/pubmed/' + pmid);
                a.textContent = 'PMID:' + pmid;
                rn.appendChild(a);
            }

            if (ref['doi']) {
                let a = this.doc.createElement('a');
                a.setAttribute('href', 'https://doi.org/' + pmid);
                a.textContent = 'doi';
                rn.appendChild(a);
            }

            return rn;
        } else {
            return this.makeDummyRefNode(pmid, lab);
        }
    }

    makeDummyRefNode(pmid, lab) {
        let rn = this.doc.createElement('div');
        rn.setAttribute('data-hindcite-ref', 'PMID' + pmid);
        rn.setAttribute('id', 'PMID' + pmid);

        let ln = this.doc.createElement('span');
        ln.textContent = lab;
        rn.appendChild(ln);

        let tn = this.doc.createTextNode(' pubmed id not found');
        rn.appendChild(tn);

        return rn;
    }

    normalizePmid(pmid) {
        let re = /^PMID([0-9]+)$/;
        let res = re.exec(pmid);
        if (res != null) {
            pmid = res[1];
        }
        return pmid;
    }

}

exports.PubmedScraper = PubmedScraper;
exports.CitationGetter = CitationGetter;
exports.Hindcite = Hindcite;
