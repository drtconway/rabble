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

class Hindcite {
    constructor(doc) {
        this.doc = doc;
        this.refs = doc.getElementById('hindcite-references');
    }

    recomputeReferences() {
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
        let jdx = {};
        let refids = [];
        let refNodes = this.getRefNodes();
        for (let i = 0; i < refNodes.length; i++) {
            let node = refNodes[i];
            let pmid = node.getAttribute('id');
            refids.push(pmid);
            jdx[pmid] = node;
        }

        let needed = [];
        for (let i = 0; i < pmids.length; i++) {
            let pmid = pmids[i];
            if (jdx[pmid] == null) {
                needed.push(pmid);
            }
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

}

exports.PubmedScraper = PubmedScraper;
