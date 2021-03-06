let fs = require('fs');
let assert = require('assert');
let XmlDom = require('xmldom');
let PubmedScraper = require('../hindcite').PubmedScraper;
let Hindcite = require('../hindcite').Hindcite;
let CitationGetter = require('../hindcite').CitationGetter;

class TestCitationResolver {

    constructor(config) {
        this.idx = {};
        this.addRef({"pmid": "10433955", "doi": "10.1016/S0002-9440(10)65158-9", "citation": "Ebert C, von Haken M, Meyer-Puttlitz B, Wiestler OD, Reifenberger G, Pietsch T, von Deimling A: Molecular Genetic Analysis of Ependymal Tumors. The American Journal of Pathology 1999, 155:627–632."});
        this.addRef({"pmid": "10084308", "doi": "10.1046/j.1523-1747.1999.00507.x", "citation": "McGregor JM, Crook T, Fraser-Andrews EA, Rozycka M, Crossland S, Brooks L, Whittaker SJ: Spectrum of p53 Gene Mutations Suggests a Possible Role for Ultraviolet Radiation in the Pathogenesis of Advanced Cutaneous Lymphomas. Journal of Investigative Dermatology 1999, 112:317–321."});
        this.addRef({"pmid": "10439047", "doi": "10.1038/sj.onc.1202824", "citation": "Klugbauer S, Rabes HM: The transcription coactivator HTIF1 and a related protein are fused to the RET receptor tyrosine kinase in childhood papillary thyroid carcinomas. Oncogene 1999, 18:4388–4393."});
        this.addRef({"pmid": "10022821", "doi": "10.1038/sj.onc.1202440", "citation": "Saxena A, Shriml LM, Dean M, Ali IU: Comparative molecular genetic profiles of anaplastic astrocytomas/ glioblastomas multiforme and their subsequent recurrences. Oncogene 1999, 18:1385–1390."});
        this.addRef({"pmid": "10602519", "doi": "10.1038/sj.onc.1203155", "citation": "Panagopoulos I, Mencinger M, Dietrich CU, Bjerkehagen B, Saeter G, Mertens F, Mandahl N, Heim S: Fusion of the RBP56 and CHN genes in extraskeletal myxoid chondrosarcomas with translocation t(9;17)(q22;q11). Oncogene 1999, 18:7594–7598."});
        this.addRef({"pmid": "10327057", "doi": "10.1038/sj.onc.1202585", "citation": "Bartsch D, Hahn SA, Danichevski KD, Ramaswamy A, Bastian D, Galehdari H, Barth P, Schmiegel W, Simon B, Rothmund M: Mutations of the DPC4/Smad4 gene in neuroendocrine pancreatic tumors. Oncogene 1999, 18:2367–2371."});
        this.addRef({"pmid": "10022819", "doi": "10.1038/sj.onc.1202418", "citation": "Gimm O, Neuberg DS, Marsh DJ, Dahia PL, Hoang-Vu C, Raue F, Hinze R, Dralle H, Eng C: Over-representation of a germline RET sequence variant in patients with sporadic medullary thyroid carcinoma and somatic RET codon 918 mutation. Oncogene 1999, 18:1369–1373."});
        this.addRef({"pmid": "10389982", "doi": "10.1038/sj.bjc.6690505", "citation": "Sartor M, Steingrimsdottir H, Elamin F, Gäken J, Warnakulasuriya S, Partridge M, Thakker N, Johnson NW, Tavassoli M: Role of p16/MTS1, cyclin D1 and RB in primary oral cancer and oral cancer cell lines. British Journal of Cancer 1999, 80:79–86."});
        this.addRef({"pmid": "10389976", "doi": "10.1038/sj.bjc.6690319", "citation": "Farrell WE, Simpson DJ, Bicknell J, Magnay JL, Kyrodimou E, Thakker RV, Clayton RN: Sequence analysis and transcript expression of the MEN1 gene in sporadic pituitary tumours. British Journal of Cancer 1999, 80:44–50."});
        this.addRef({"pmid": "10584868", "doi": "10.1038/sj.bjc.6690815", "citation": "Huiping C, Sigurgeirsdottir JR, Jonasson JG, Eiriksdottir G, Johannsdottir JT, Egilsson V, Ingvarsson S: Chromosome alterations and E-cadherin gene mutations in human lobular breast cancer. British Journal of Cancer 1999, 81:1103–1110."});
    }

    addRef(stuff) {
        this.idx[stuff['pmid']] = stuff;
    }

    lookupPubmed(pmid) {
        let re = /^PMID([0-9]+)$/;
        let res = re.exec(pmid);
        if (res != null) {
            pmid = res[1];
        }
        return this.idx[pmid];
    }
}

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


describe('CitationGetter', function() {
    let C = new CitationGetter({email: 'thomas.conway@petermac.org'});
    if (false) {
        it('test pubmedToDoi 1', function() {
            this.timeout(5000);
            let d = C.pubmedToDoi('22673234');
            assert.equal(d, '10.1158/0008-5472.CAN-11-4134');
        });
        it('test pubmedToDoi 2', function() {
            this.timeout(5000);
            let d = C.pubmedToDoi('30382840');
            assert.equal(d, '10.1186/s12864-018-5156-1');
        });
        it('test lookupDoi', function() {
            this.timeout(5000);
            let res = C.lookupDoi('10.1186/s12864-018-5156-1', 'biomed-central');
            assert.equal(res.doi, '10.1186/s12864-018-5156-1');
            assert.equal(res.citation, '1. Vu TN, Deng W, Trac QT, Calza S, Hwang W, Pawitan Y: A fast detection of fusion genes from paired-end RNA-seq data. BMC Genomics 2018, 19.');

        });
        it('test lookupPubmed', function() {
            this.timeout(5000);
            let res = C.lookupPubmed('30382840', 'biomed-central');
            assert.equal(res.pmid, '30382840');
            assert.equal(res.doi, '10.1186/s12864-018-5156-1');
            assert.equal(res.citation, '1. Vu TN, Deng W, Trac QT, Calza S, Hwang W, Pawitan Y: A fast detection of fusion genes from paired-end RNA-seq data. BMC Genomics 2018, 19.');

        });
    }
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

describe('Hindcite', function() {
    let data = fs.readFileSync('test/test-doc.xml', 'utf8');
    let doc = new XmlDom.DOMParser().parseFromString(data);
    let R = new TestCitationResolver({});
    let H = new Hindcite(doc, R);
    it('test getRefNodes', function() {
        let nodes = H.getRefNodes();
        assert.equal(nodes.length, 8);
        assert.equal(nodes[0].getAttribute('id'), 'PMID20066118');
        assert.equal(nodes[7].getAttribute('id'), 'PMID28280037');
    });
    it('test getCiteNodes', function() {
        let nodes = H.getCiteNodes();
        assert.equal(nodes.length, 12);
    });

    it('test reformatCitation', function() {
        let n = doc.getElementById('test-target-1');
        assert.equal(n.childNodes.length, 3);
        assert.equal(n.childNodes[0].textContent, '(');
        assert.equal(n.childNodes[1].getAttribute('data-hindcite-key'), 'PMID20066118');
        assert.equal(n.childNodes[2].textContent, ')');
        H.reformatCitation(n);
        assert.equal(n.childNodes.length, 3);
        assert.equal(n.childNodes[0].textContent, '[');
        assert.equal(n.childNodes[1].getAttribute('data-hindcite-key'), 'PMID20066118');
        assert.equal(n.childNodes[2].textContent, ']');
    });

    it('test parseCitationNode unlinked', function() {
        let citeNode = doc.createElement('span');
        citeNode.setAttribute('data-hindcite-cite', true);
        citeNode.textContent = '[PMID:20066118, PMID:27667712, PMID:28280037]';
        H.parseCitationNode(citeNode);
        assert.equal(citeNode.textContent, '[?, ?, ?]');
    });

    it('test parseCitationNode', function() {
        let n = doc.getElementById('test-target-1');
        assert.equal(n.childNodes.length, 3);
        assert.equal(n.childNodes[0].textContent, '[');
        assert.equal(n.childNodes[1].getAttribute('data-hindcite-key'), 'PMID20066118');
        assert.equal(n.childNodes[2].textContent, ']');
        n.childNodes[2].textContent = 'PMID:27667712]'
        H.parseCitationNode(n);
        assert.equal(n.childNodes.length, 4);
        assert.equal(n.childNodes[0].textContent, '[');
        assert.equal(n.childNodes[1].getAttribute('data-hindcite-key'), 'PMID20066118');
        assert.equal(n.childNodes[2].getAttribute('data-hindcite-key'), 'PMID27667712');
        assert.equal(n.childNodes[3].textContent, ']');
        H.reformatCitation(n);
        assert.equal(n.childNodes.length, 5);
        assert.equal(n.childNodes[0].textContent, '[');
        assert.equal(n.childNodes[1].getAttribute('data-hindcite-key'), 'PMID20066118');
        assert.equal(n.childNodes[2].textContent, ', ');
        assert.equal(n.childNodes[3].getAttribute('data-hindcite-key'), 'PMID27667712');
        assert.equal(n.childNodes[4].textContent, ']');
    });

    it('test recomputeReferences reorder', function() {
        // We've done the previous tests, so this is the expected state.
        //
        let n = doc.getElementById('test-target-1');
        assert.equal(n.textContent, '[1, ?]');

        let m = doc.getElementById('test-target-2');
        assert.equal(m.textContent, '[4]');

        let r = H.getRefNodes();
        assert.equal(r.length, 8);
        assert.equal(r[0].getAttribute('id'), 'PMID20066118');
        assert.equal(r[1].getAttribute('id'), 'PMID1905840');
        assert.equal(r[2].getAttribute('id'), 'PMID27880943');
        assert.equal(r[3].getAttribute('id'), 'PMID27667712');
        assert.equal(r[4].getAttribute('id'), 'PMID26228128');

        H.recomputeReferences();

        assert.equal(n.textContent, '[1, 2]');

        assert.equal(m.textContent, '[2]');

        r = H.getRefNodes();
        assert.equal(r.length, 8);
        assert.equal(r[0].getAttribute('id'), 'PMID20066118');
        assert.equal(r[1].getAttribute('id'), 'PMID27667712');
        assert.equal(r[2].getAttribute('id'), 'PMID1905840');
        assert.equal(r[3].getAttribute('id'), 'PMID27880943');
        assert.equal(r[4].getAttribute('id'), 'PMID26228128');
    });

    it('test parseForCitations', function() {
        let cns = H.getCiteNodes();
        assert.equal(cns.length, 13);

        let n = doc.getElementById('the-content');
        n.appendChild(doc.createTextNode('the quick [PMID:27667712], brown [PMID:20066118, PMID:27667712, PMID:28280037] fox.'));
        H.parseForCitations(n);
        H.recomputeReferences(n);

        cns = H.getCiteNodes();
        assert.equal(cns.length, 17);
        assert.equal(cns[13].textContent, '2');
        assert.equal(cns[14].textContent, '1');
        assert.equal(cns[15].textContent, '2');
        assert.equal(cns[16].textContent, '8');

        //let s = new XmlDom.XMLSerializer().serializeToString(doc)
        //console.log(s);
    });

    it('test recomputeReferences remove', function() {
        let n = doc.getElementById('test-target-3-parent');

        H.reformatCitation(n);
        assert.equal(n.textContent, '[5, 6, 7, 8]');

        let r = H.getRefNodes();
        assert.equal(r.length, 8);
        assert.equal(r[0].getAttribute('id'), 'PMID20066118');
        assert.equal(r[1].getAttribute('id'), 'PMID27667712');
        assert.equal(r[2].getAttribute('id'), 'PMID1905840');
        assert.equal(r[3].getAttribute('id'), 'PMID27880943');
        assert.equal(r[4].getAttribute('id'), 'PMID26228128');
        assert.equal(r[5].getAttribute('id'), 'PMID21764762');
        assert.equal(r[6].getAttribute('id'), 'PMID22673234');
        assert.equal(r[7].getAttribute('id'), 'PMID28280037');
        assert.equal(r[7].hasAttribute('data-hindcite-unused'), false);

        assert.equal(n.childNodes.length, 9);
        let m = doc.getElementById('test-target-3');
        n.removeChild(m);
        assert.equal(n.childNodes.length, 8);
        H.reformatCitation(n);
        assert.equal(n.textContent, '[5, 6, 8]');

        H.recomputeReferences();

        assert.equal(n.textContent, '[5, 6, 7]');

        r = H.getRefNodes();
        assert.equal(r.length, 8);
        assert.equal(r[0].getAttribute('id'), 'PMID20066118');
        assert.equal(r[1].getAttribute('id'), 'PMID27667712');
        assert.equal(r[2].getAttribute('id'), 'PMID1905840');
        assert.equal(r[3].getAttribute('id'), 'PMID27880943');
        assert.equal(r[4].getAttribute('id'), 'PMID26228128');
        assert.equal(r[5].getAttribute('id'), 'PMID21764762');
        assert.equal(r[6].getAttribute('id'), 'PMID28280037');
        assert.equal(r[7].getAttribute('id'), 'PMID22673234');
        assert.equal(r[7].getAttribute('data-hindcite-unused'), 'true');
    });

    it('test recomputeReferences add', function() {
        let n = doc.getElementById('test-target-3-parent');


        n.appendChild(doc.createTextNode('PMID:10389976'));
        H.parseCitationNode(n);

        H.reformatCitation(n);
        assert.equal(n.textContent, '[5, 6, 7, ?]');

        H.recomputeReferences();
        assert.equal(n.textContent, '[5, 6, 7, 8]');

        let r = H.getRefNodes();
        assert.equal(r.length, 9);
        assert.equal(r[0].getAttribute('id'), 'PMID20066118');
        assert.equal(r[0].firstChild.textContent, '1.');
        assert.equal(r[1].getAttribute('id'), 'PMID27667712');
        assert.equal(r[1].firstChild.textContent, '2.');
        assert.equal(r[2].getAttribute('id'), 'PMID1905840');
        assert.equal(r[2].firstChild.textContent, '3.');
        assert.equal(r[3].getAttribute('id'), 'PMID27880943');
        assert.equal(r[3].firstChild.textContent, '4.');
        assert.equal(r[4].getAttribute('id'), 'PMID26228128');
        assert.equal(r[4].firstChild.textContent, '5.');
        assert.equal(r[5].getAttribute('id'), 'PMID21764762');
        assert.equal(r[5].firstChild.textContent, '6.');
        assert.equal(r[6].getAttribute('id'), 'PMID28280037');
        assert.equal(r[6].firstChild.textContent, '7.');
        assert.equal(r[7].getAttribute('id'), 'PMID10389976');
        assert.equal(r[7].firstChild.textContent, '8.');
        assert.equal(r[7].textContent, '8. ' + R.idx['10389976']['citation'] + 'PMID:10389976' + 'doi');
        assert.equal(r[8].getAttribute('id'), 'PMID22673234');
        assert.equal(r[8].getAttribute('data-hindcite-unused'), 'true');

        let s = new XmlDom.XMLSerializer().serializeToString(doc)
        console.log(s);
    });
});
