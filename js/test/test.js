var assert = require('assert');
var XmlDom = require('xmldom');
var Rabble = require('../rabble').Rabble;

describe('Rabble', function() {
    describe('jsonToHtml', function() {
        let docStr = "<doc><div id='root'/></doc>";
        let doc = new XmlDom.DOMParser().parseFromString(docStr);
        let e = doc.getElementById('root');
        let r = new Rabble(doc, e);
        it('text content', function() {
            let f = doc.createElement('qux');
            r.jsonToHtml('foo', f);
            let s = new XmlDom.XMLSerializer().serializeToString(f);
            assert.equal(s, '<qux>foo</qux>');
        });
        it('simple element', function() {
            let f = doc.createElement('qux');
            r.jsonToHtml({'foo': 'bar'}, f);
            let s = new XmlDom.XMLSerializer().serializeToString(f);
            assert.equal(s, '<qux><foo>bar</foo></qux>');
        });
        it('element with attributes', function() {
            let f = doc.createElement('qux');
            r.jsonToHtml({'foo': [{'*': {'class': 'important'}}, 'bar']}, f);
            let s = new XmlDom.XMLSerializer().serializeToString(f);
            assert.equal(s, '<qux><foo class="important">bar</foo></qux>');
        });
        it('multiple text content', function() {
            let f = doc.createElement('qux');
            r.jsonToHtml(['foo', 'bar', 'baz'], f);
            let s = new XmlDom.XMLSerializer().serializeToString(f);
            assert.equal(s, '<qux>foobarbaz</qux>');
        });
    });
});
