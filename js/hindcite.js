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
    constructor(doc) {
        this.doc = doc;
        this.refs = doc.getElementById('hindcite-references');
    }

    /**
     * Parse a node (usually a span tag) with a 'data-hindcite-cite' tag.
     */
    parseCitationNode(citeNode) {
        if (!citeNode.hasAttribute('data-hindcite-cite') {
            // not a citation node!
            return;
        }

        let nodes = [];
        for (let i = 0; i < citeNode.childNodes.length; i++) {
            let node = citeNode.childNodes[i];
            if (node.nodeType != Node.TEXT_NODE)  {
                nodes.push(node);
                continue;
            }
            let parts = node.textContent.split(/PMID:([0-9]+)/);
            if (parts.length == 1) {
                nodes.push(node);
                continue;
            }
            for (let j = 0; j < parts.length; j++) {
                if ((j & 1) == 0) {
                    // Parts 0, 2, ... are text
                    if (parts[j].length > 0) {
                        citeNode.appendChild(this.doc.createTextNode(parts[j]));
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
        if (!citeNode.hasAttribute('data-hindcite-cite') {
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
            nodes.push(node);
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
            jdx[pmid] = node;
            let pnode = node.parent;
            pnode.removeChild(node);
        }

        // Figure out which references are missing.
        //
        let needed = [];
        for (let i = 0; i < pmids.length; i++) {
            let pmid = pmids[i];
            if (jdx[pmid] == null) {
                needed.push(pmid);
            }
        }

        // TODO fetch any missing references.


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
                node.setAttribute('data-hindcite-unused', null);
            }
            this.refs.appendChild(node);
            this.setCitationLabel('?.', node);
        }
    }

    getCiteNodes() {
        if (doc.querySelectorAll != null) {
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
        if (doc.querySelectorAll != null) {
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
        if (node.nodeName == 'div' && node.hasAttribute('data-hindcite-ref')) {
            let kid0 = node.firstChild;
            if (kid0.nodeName == 'span') {
                kid0.textContent = lab;
            }
        }
    }

    removeAllChildren(node) {
        while (node.hasChildren()) {
            node.removeChild(node.childNodes[0]);
        }
    }

}

exports.PubmedScraper = PubmedScraper;
