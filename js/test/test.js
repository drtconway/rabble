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

    describe('htmlToJson', function() {
        let docStr = "<doc><div id='one'/></doc>";
        let doc = new XmlDom.DOMParser().parseFromString(docStr);
        let d = doc.documentElement;
        let r = new Rabble(doc, d);
        it('single node', function() {
            let f = doc.createElement('qux');
            let j = r.htmlToJson(f);
            let s = JSON.stringify(j);
            assert.equal(s, '{"qux":[]}');
        });
        it('single node with attributes', function() {
            let f = doc.createElement('qux');
            f.setAttribute('foo', 'bar');
            f.setAttribute('baz', 42);
            let j = r.htmlToJson(f);
            let s = JSON.stringify(j);
            assert.equal(s, '{"qux":{"*":{"foo":"bar","baz":"42"}}}');
        });
        it('single node with children', function() {
            let f = doc.createElement('qux');
            let nn = ['one', 'two', 'three'];
            for (let i = 0; i < nn.length; i++) {
                let g = doc.createElement(nn[i]);
                f.appendChild(g);
            }
            let j = r.htmlToJson(f);
            let s = JSON.stringify(j);
            assert.equal(s, '{"qux":[{"one":[]},{"two":[]},{"three":[]}]}');
        });
        it('text node', function() {
            let f = doc.createTextNode('qux');
            let j = r.htmlToJson(f);
            let s = JSON.stringify(j);
            assert.equal(s, '"qux"');
        });
        it('element with text node', function() {
            let f = doc.createElement('qux');
            let g = doc.createTextNode('wombat');
            f.appendChild(g);
            let j = r.htmlToJson(f);
            let s = JSON.stringify(j);
            assert.equal(s, '{"qux":"wombat"}');
        });
    });

    it('instantiate text', function() {
        let docStr = "<doc>" +
                     "<div id='one'>" +
                     "<div data-rabble-name='foo'>" +
                     "</div>" +
                     "</div>" +
                     "</doc>";
        let doc = new XmlDom.DOMParser().parseFromString(docStr);
        let d = doc.getElementById('one');
        let r = new Rabble(doc, d);
        let e = r.instantiate({'foo':'bar'});
        let s = new XmlDom.XMLSerializer().serializeToString(e);
        assert.equal(s, '<div id="one"><div data-rabble-name="foo">bar</div></div>');
    });

    it('instantiate text multiple', function() {
        let docStr = "<doc>" +
                     "<div id='one'>" +
                     "<div data-rabble-name='foo'>" +
                     "</div>" +
                     "</div>" +
                     "</doc>";
        let doc = new XmlDom.DOMParser().parseFromString(docStr);
        let d = doc.getElementById('one');
        let r = new Rabble(doc, d);
        let e = r.instantiate({'foo':['bar','baz','qux']});
        let s = new XmlDom.XMLSerializer().serializeToString(e);
        assert.equal(s, '<div id="one">' +
                        '<div data-rabble-name="foo">bar</div>' +
                        '<div data-rabble-name="foo">baz</div>' +
                        '<div data-rabble-name="foo">qux</div>' +
                        '</div>');
    });

    it('instantiate lines', function() {
        let docStr = "<doc>" +
                     "<div id='one'>" +
                     "<div data-rabble-name='foo' data-rabble-kind='lines'>" +
                     "</div>" +
                     "</div>" +
                     "</doc>";
        let doc = new XmlDom.DOMParser().parseFromString(docStr);
        let d = doc.getElementById('one');
        let r = new Rabble(doc, d);
        let e = r.instantiate({'foo':['bar','baz','qux']});
        let s = new XmlDom.XMLSerializer().serializeToString(e);
        assert.equal(s, '<div id="one">' +
                        '<div data-rabble-name="foo" data-rabble-kind="lines">' +
                        '<div>bar</div>' +
                        '<div>baz</div>' +
                        '<div>qux</div>' +
                        '</div>' +
                        '</div>');
    });

    it('instantiate rich', function() {
        let docStr = "<doc>" +
                     "<div id='one'>" +
                     "<div data-rabble-name='foo' data-rabble-kind='rich'/>" +
                     "</div>" +
                     "</doc>";
        let doc = new XmlDom.DOMParser().parseFromString(docStr);
        let d = doc.getElementById('one');
        let r = new Rabble(doc, d);
        let e = r.instantiate({'foo':{'p':{'cite':[{'*':{'data-pmid':'1234'}},'Doig et al, 2017']}}});
        let s = new XmlDom.XMLSerializer().serializeToString(e);
        assert.equal(s, '<div id="one">' +
                        '<div data-rabble-name="foo" data-rabble-kind="rich">' +
                        '<p><cite data-pmid="1234">Doig et al, 2017</cite></p>' +
                        '</div>' +
                        '</div>');
    });

    it('instantiate group', function() {
        let docStr = "<doc>" +
                     "<div id='one'>" +
                     "<div data-rabble-name='foo' data-rabble-kind='group'>" +
                     "<div data-rabble-name='bar' data-rabble-kind='text'/>" +
                     "</div>" +
                     "</div>" +
                     "</doc>";
        let doc = new XmlDom.DOMParser().parseFromString(docStr);
        let d = doc.getElementById('one');
        let r = new Rabble(doc, d);
        let e = r.instantiate({'foo':{'bar':'baz'}});
        let s = new XmlDom.XMLSerializer().serializeToString(e);
        assert.equal(s, '<div id="one">' +
                        '<div data-rabble-name="foo" data-rabble-kind="group">' +
                        '<div data-rabble-name="bar" data-rabble-kind="text">' +
                        'baz' +
                        '</div>' +
                        '</div>' +
                        '</div>');
    });

    it('instantiate group multiple', function() {
        let docStr = "<doc>" +
                     "<div id='one'>" +
                     "<div data-rabble-name='foo' data-rabble-kind='group'>" +
                     "<div data-rabble-name='bar' data-rabble-kind='text'/>" +
                     "</div>" +
                     "</div>" +
                     "</doc>";
        let doc = new XmlDom.DOMParser().parseFromString(docStr);
        let d = doc.getElementById('one');
        let r = new Rabble(doc, d);
        let e = r.instantiate({'foo':[{'bar':'baz'},{'bar':['qux','wombat']}]});
        let s = new XmlDom.XMLSerializer().serializeToString(e);
        assert.equal(s, '<div id="one">' +
                        '<div data-rabble-name="foo" data-rabble-kind="group">' +
                        '<div data-rabble-name="bar" data-rabble-kind="text">' +
                        'baz' +
                        '</div>' +
                        '</div>' +
                        '<div data-rabble-name="foo" data-rabble-kind="group">' +
                        '<div data-rabble-name="bar" data-rabble-kind="text">' +
                        'qux' +
                        '</div>' +
                        '<div data-rabble-name="bar" data-rabble-kind="text">' +
                        'wombat' +
                        '</div>' +
                        '</div>' +
                        '</div>');
    });

});
