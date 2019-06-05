package org.petermac.rabble;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PathToken {
    public enum Kind {
        NAME, STRING, NUMINT, NUMFLT, LPAREN, RPAREN, LBRAC, RBRAC, COMMA, BANG,
        PLUS, MINUS, TIMES, SLASH, DSLASH, MOD, LT, LE, EQ, NE, GE, GT,
        AND, OR, DOLLAR, _WS
    }
    public Kind kind;
    public String val;

    public PathToken(String val, Kind kind) {
        this.kind = kind;
        this.val = val;
    }

    static class TokenSpec {
        public Pattern regex;
        public Kind kind;

        TokenSpec(Pattern regex, Kind kind) {
            this.regex = regex;
            this.kind = kind;
        }
    }

    static class Tokenizer {
        private List<TokenSpec> tokSpecs;

        public Tokenizer() {
            tokSpecs = new ArrayList<TokenSpec>();
        }

        public void add(String exprn, Kind kind) {
            Pattern p = Pattern.compile(exprn);
            tokSpecs.add(new TokenSpec(p, kind));
        }

        public static final Tokenizer make() {
            Tokenizer t = new Tokenizer();
            t.add("^(\\s+)", Kind._WS);
            t.add("^([a-zA-Z_][a-zA-Z0-9_]*)", Kind.NAME);
            t.add("^'([^']*)'", Kind.STRING);
            t.add("^\"([^\"]*)\"", Kind.STRING);
            t.add("^([-+]?([0-9][0-9_]*)?[.][0-9.]*([eE][-+][0-9]+)?)", Kind.NUMFLT);
            t.add("^(([+-])?(0|[1-9][0-9]*))", Kind.NUMINT);
            t.add("^(\\()", Kind.LPAREN);
            t.add("^(\\))", Kind.RPAREN);
            t.add("^(\\[)", Kind.LBRAC);
            t.add("^(\\])", Kind.RBRAC);
            t.add("^(,)", Kind.COMMA);
            t.add("^(\\+)", Kind.PLUS);
            t.add("^(-)", Kind.MINUS);
            t.add("^(//)", Kind.DSLASH);
            t.add("^(/)", Kind.SLASH);
            t.add("^(%)", Kind.MOD);
            t.add("^(<)", Kind.LT);
            t.add("^(<=)", Kind.LE);
            t.add("^(==)", Kind.EQ);
            t.add("^(!=)", Kind.NE);
            t.add("^(>=)", Kind.GE);
            t.add("^(>)", Kind.GT);
            t.add("^(!)", Kind.BANG);
            t.add("^(&&)", Kind.AND);
            t.add("^(\\|\\|)", Kind.OR);
            t.add("^(\\$)", Kind.DOLLAR);
            return t;
        }

        public List<PathToken> tokenize(String str) throws Exception {
            List<PathToken> tokens = new ArrayList<PathToken>();
            String s = new String(str);
            while (!s.equals("")) {
                boolean match = false;
                for (TokenSpec tx : tokSpecs) {
                    Matcher m = tx.regex.matcher(s);
                    if (m.find()) {
                        match = true;

                        if (tx.kind != Kind._WS) {
                            String tok = m.group();
                            tokens.add(new PathToken(tok, tx.kind));
                        }

                        s = m.replaceFirst("");
                        break;
                    }
                }
                if (!match) {
                    throw new Exception("unexpected character at \"" + s + "\"");
                }
            }
            return tokens;
        }
    }

    public static final List<PathToken> tokenize(String s) throws Exception {
        Tokenizer t = Tokenizer.make();
        return t.tokenize(s);
    }

}
