package org.petermac.rabble;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class PathParser {
    PathParser(List<PathToken> tokens) {
        this.curPos = 0;
        this.tokens = tokens;
    }

    public PathTree parse() throws Exception {
        PathTree res = expression();
        if (curPos != tokens.size()) {
            throw new Exception("unexpected token");
        }
        return res;
    }

    private Integer curPos;
    private List<PathToken> tokens;
    
    public PathTree expression() throws Exception {
        return disjunctiveExpression();
    }

    public PathTree disjunctiveExpression() throws Exception {
        List<PathTree> kids = new ArrayList<PathTree>();
        kids.add(conjunctiveExpression());
        while (more() && peek().kind == PathToken.Kind.OR) {
            next();
            kids.add(conjunctiveExpression());
        }
        return PathTree.make(PathToken.Kind.OR, kids);
    }

    public PathTree conjunctiveExpression() throws Exception {
        List<PathTree> kids = new ArrayList<PathTree>();
        kids.add(relationalExpression());
        while (more() && peek().kind == PathToken.Kind.AND) {
            next();
            kids.add(relationalExpression());
        }
        return PathTree.make(PathToken.Kind.AND, kids);
    }

    private static final Set<PathToken.Kind> relOps = Utils.newHashSet(
        PathToken.Kind.LT, PathToken.Kind.LE,
        PathToken.Kind.EQ, PathToken.Kind.NE,
        PathToken.Kind.GE, PathToken.Kind.GT
    );
    public PathTree relationalExpression() throws Exception {
        PathTree res0 = additiveExpression();
        if (more() && relOps.contains(peek().kind)) {
            PathToken.Kind kind = next().kind;
            PathTree res1 = additiveExpression();
            return PathTree.make(kind, res0, res1);
        }
        return res0;
    }

    private static final Set<PathToken.Kind> addOps = Utils.newHashSet(
        PathToken.Kind.PLUS, PathToken.Kind.MINUS
    );
    public PathTree additiveExpression() throws Exception {
        PathTree res = multiplicativeExpression();
        while (more() && addOps.contains(peek().kind)) {
            PathToken.Kind kind = next().kind;
            PathTree rhs = multiplicativeExpression();
            res = PathTree.make(kind, res, rhs);
        }
        return res;
    }

    private static final Set<PathToken.Kind> mulOps = Utils.newHashSet(
        PathToken.Kind.TIMES, PathToken.Kind.SLASH,
        PathToken.Kind.DSLASH, PathToken.Kind.MOD
    );
    public PathTree multiplicativeExpression() throws Exception {
        PathTree res = unaryExpression();
        while (more() && mulOps.contains(peek().kind)) {
            PathToken.Kind kind = next().kind;
            PathTree rhs = unaryExpression();
            res = PathTree.make(kind, res, rhs);
        }
        return res;
    }

    private static final Set<PathToken.Kind> unaOps = Utils.newHashSet(
        PathToken.Kind.BANG, PathToken.Kind.MINUS
    );
    public PathTree unaryExpression() throws Exception {
        if (more() && unaOps.contains(peek().kind)) {
            PathToken.Kind kind = next().kind;
            PathTree kid = applicativeExpression();
            return PathTree.make(kind, kid);
        } else {
            return applicativeExpression();
        }
    }

    public PathTree applicativeExpression() throws Exception {
        PathTree prim = primaryExpression();
        if (!more()) {
            return prim;
        }
        switch (peek().kind) {
            case LBRAC:
                next();
                PathTree pred = expression();
                require(PathToken.Kind.RBRAC);
                return PathTree.make(PathToken.Kind.LBRAC, prim, pred);
            case LPAREN:
                next();
                if (peek().kind == PathToken.Kind.RPAREN) {
                    next();
                    return PathTree.make(PathToken.Kind.LPAREN, prim);
                } else {
                    List<PathTree> args = new ArrayList<PathTree>();
                    args.add(prim);
                    args.add(expression());
                    while (peek().kind == PathToken.Kind.COMMA) {
                        next();
                        args.add(expression());
                    }
                    require(PathToken.Kind.RPAREN);
                    return PathTree.make(PathToken.Kind.LPAREN, args);
                }
            default:
                return prim;
        }
    }

    public PathTree primaryExpression() throws Exception {
        PathToken tok = null;
        PathTree val = null;

        switch (peek().kind) {
            case NAME:
            case STRING:
            case NUMINT:
            case NUMFLT:
                tok = next();
                return PathTree.make(tok.kind, tok.val);

            case DOLLAR:
                tok = next();
                return PathTree.make(tok.kind);

            case LPAREN:
                next();
                val = expression();
                require(PathToken.Kind.RPAREN);
                return val;

            case AT:
                next();
                tok = require(PathToken.Kind.NAME);
                return PathTree.make(PathToken.Kind.AT, tok.val);

            default:
                throw new Exception("unexpected token");
        }
    }

    public boolean more() {
        return (curPos < tokens.size());
    }

    public PathToken peek() throws Exception {
        if (curPos >= tokens.size()) {
            throw new Exception("unexpected end of input");
        }
        return tokens.get(curPos);
    }

    public PathToken next() throws Exception {
        if (curPos >= tokens.size()) {
            throw new Exception("unexpected end of input");
        }
        return tokens.get(curPos++);
    }

    public PathToken require(PathToken.Kind kind) throws Exception {
        if (peek().kind != kind) {
            throw new Exception("unexpected token");
        }
        return next();
    }
}
