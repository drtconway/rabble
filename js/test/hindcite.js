let fs = require('fs');
let assert = require('assert');
let XmlDom = require('xmldom');
let PubmedScraper = require('../hindcite').PubmedScraper;

describe('PubmedScraper', function() {
    let doc = null;
    let data = fs.readFileSync('test/PMID22673234.xml', 'utf8');
    doc = new XmlDom.DOMParser().parseFromString(data);

    let p = new PubmedScraper();
    it('test xpath extraction of title', function() {
        let found = false;
        p.withNode(doc, '//ArticleTitle', function(node) {
            assert.equal(node.textContent, 'The human TLR innate immune gene family is ' +
                                           'differentially influenced by DNA stress and p53 status in cancer cells.');
            found = true;
        });
        assert.equal(found, true);
    });
    it('test xpath extraction of undefined', function() {
        let found = false;
        p.withNode(doc, '//Wibble', function(node) {
            found = true;
        });
        assert.equal(found, false);
    });

    it('test finding authors', function() {
        let count = 0;
        p.withNode(doc, '//Article/AuthorList/Author', function(node) {
            count += 1;
        });
        assert.equal(count, 3);
    });

    it('test scraping', function() {
        let ref = p.scrapePubmedRef('22673234', doc);
        assert.equal(Object.keys(ref).length, 5);
        assert.equal(ref['pmid'], '22673234');
        assert.equal(ref['authors'], 'Shatz M., Menendez D., Resnick M. A.');
        assert.equal(ref['title'], 'The human TLR innate immune gene family is differentially influenced by DNA stress and p53 status in cancer cells.');
        assert.equal(ref['journal'], 'Cancer research. 2012;72(16):3948-57.');
        assert.equal(ref['doi'], '10.1158/0008-5472.CAN-11-4134');
    });
});

describe('Pubmed Fetch', function() {
    let config = {};
    let p = new PubmedScraper(config);

    // This doesn't work behind a proxy.
    if (false) {
        it('test scraping', function() {
            this.timeout(10000);
            let ref = p.getPubmedRef('22673234');
            assert.equal(ref['pmid'], '22673234');
            assert.equal(ref['authors'], 'Shatz M., Menendez D., Resnick M. A.');
            assert.equal(ref['title'], 'The human TLR innate immune gene family is differentially influenced by DNA stress and p53 status in cancer cells.');
            assert.equal(ref['journal'], 'Cancer research. 2012;72(16):3948-57.');
            assert.equal(ref['doi'], '10.1158/0008-5472.CAN-11-4134');
            assert.equal(Object.keys(ref).length, 5);
        });
    }
});
