package org.petermac.rabble;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;

/**
 * The PathParser parses strings of tokens with the following grammar:
 *
 * <pre>
 * primaryExpression = name | string | number | '(' expression ')' | ('@' name) | '$'
 * applicativeExpression = primaryExpression (predicateApplication|functionApplication)?
 * predicateApplication = '[' expression ']'
 * functionApplication = '(' (expression (',' expression)*)? ')'
 * unaryExpression = ('!'|'-')? applicativeExpression
 * multiplicativeExpression = unaryExpression (('*'|'/'|'%'|'//') unaryExpression)*
 * additiveExpression = multiplicativeExpression (('+'|'-') multiplicativeExpression)*
 * relationalExpression = additiveExpression (('<'|'<='|'=='|'!='|'>='|'>') additiveExpression)*
 * conjunctiveExpression = relationalExpression ('&&' relationalExpression)*
 * disjunctiveExpression = conjunctiveExpression ('||' conjunctiveExpression)*
 * expression = disjunctiveExpression
 * </pre>
 */
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
        return PathTree.make(PathTree.Kind.OR, kids);
    }

    public PathTree conjunctiveExpression() throws Exception {
        List<PathTree> kids = new ArrayList<PathTree>();
        kids.add(relationalExpression());
        while (more() && peek().kind == PathToken.Kind.AND) {
            next();
            kids.add(relationalExpression());
        }
        return PathTree.make(PathTree.Kind.AND, kids);
    }

    @SuppressWarnings("unchecked")
    private static final Map<PathToken.Kind,PathTree.Kind> relOps = Utils.newHashMap(
        PathToken.Kind.LT, PathTree.Kind.LT,
        PathToken.Kind.LE, PathTree.Kind.LE,
        PathToken.Kind.EQ, PathTree.Kind.EQ,
        PathToken.Kind.NE, PathTree.Kind.NE,
        PathToken.Kind.GE, PathTree.Kind.GE,
        PathToken.Kind.GT, PathTree.Kind.GT
    );
    public PathTree relationalExpression() throws Exception {
        PathTree res0 = additiveExpression();
        if (more() && relOps.containsKey(peek().kind)) {
            PathToken.Kind kind = next().kind;
            PathTree res1 = additiveExpression();
            return PathTree.make(relOps.get(kind), res0, res1);
        }
        return res0;
    }
 
    @SuppressWarnings("unchecked")
    private static final Map<PathToken.Kind,PathTree.Kind> addOps = Utils.newHashMap(
        PathToken.Kind.PLUS,  PathTree.Kind.PLUS,
        PathToken.Kind.MINUS, PathTree.Kind.MINUS
    );
    public PathTree additiveExpression() throws Exception {
        PathTree res = multiplicativeExpression();
        while (more() && addOps.containsKey(peek().kind)) {
            PathToken.Kind kind = next().kind;
            PathTree rhs = multiplicativeExpression();
            res = PathTree.make(addOps.get(kind), res, rhs);
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    private static final Map<PathToken.Kind,PathTree.Kind> mulOps = Utils.newHashMap(
        PathToken.Kind.TIMES,  PathTree.Kind.TIMES,
        PathToken.Kind.SLASH,  PathTree.Kind.SLASH,
        PathToken.Kind.DSLASH, PathTree.Kind.DSLASH,
        PathToken.Kind.MOD,    PathTree.Kind.MOD
    );
    public PathTree multiplicativeExpression() throws Exception {
        PathTree res = unaryExpression();
        while (more() && mulOps.containsKey(peek().kind)) {
            PathToken.Kind kind = next().kind;
            PathTree rhs = unaryExpression();
            res = PathTree.make(mulOps.get(kind), res, rhs);
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    private static final Map<PathToken.Kind,PathTree.Kind> unaOps = Utils.newHashMap(
        PathToken.Kind.BANG, PathTree.Kind.NOT,
        PathToken.Kind.MINUS, PathTree.Kind.NEG
    );
    public PathTree unaryExpression() throws Exception {
        if (more() && unaOps.containsKey(peek().kind)) {
            PathToken.Kind kind = next().kind;
            PathTree kid = applicativeExpression();
            return PathTree.make(unaOps.get(kind), kid);
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
                return PathTree.make(PathTree.Kind.PRED, prim, pred);
            case LPAREN:
                next();
                if (peek().kind == PathToken.Kind.RPAREN) {
                    next();
                    return PathTree.make(PathTree.Kind.FUNC, prim);
                } else {
                    List<PathTree> args = new ArrayList<PathTree>();
                    args.add(prim);
                    args.add(expression());
                    while (peek().kind == PathToken.Kind.COMMA) {
                        next();
                        args.add(expression());
                    }
                    require(PathToken.Kind.RPAREN);
                    return PathTree.make(PathTree.Kind.FUNC, args);
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
                tok = next();
                return PathTree.make(tok.val);
            case STRING:
                tok = next();
                return PathTree.make(Json.createValue(tok.val));
            case NUMINT:
                tok = next();
                return PathTree.make(Json.createValue(Integer.valueOf(tok.val)));
            case NUMFLT:
                tok = next();
                return PathTree.make(Json.createValue(Float.valueOf(tok.val)));

            case DOLLAR:
                tok = next();
                return PathTree.make(PathTree.Kind.ROOT);

            case LPAREN:
                next();
                val = expression();
                require(PathToken.Kind.RPAREN);
                return val;

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
