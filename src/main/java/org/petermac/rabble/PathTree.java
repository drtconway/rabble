package org.petermac.rabble;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonValue;

class PathTree {
    public enum Kind {
        OR, AND,                                // Logical operators
        LT, LE, EQ, NE, GE, GT,                 // Comparison operators
        PLUS, MINUS, TIMES, SLASH, DSLASH, MOD, // Arithmetic operators
        NOT, NEG,                               // Unary operators
        PRED, FUNC,                             // Applicative operators
        ROOT, NAME, VALUE                       // Atoms
    };

    public Kind kind;
    public String name;
    public JsonValue value;
    public List<PathTree> kids;

    private PathTree(Kind kind) {
        this.kind = kind;
        this.value = null;
        this.value = null;
        this.kids = null;
    }

    public static PathTree make(Kind kind) {
        return new PathTree(kind);
    }

    public static PathTree make(String name) {
        PathTree res = new PathTree(Kind.NAME);
        res.name = name;
        return res;
    }

    public static PathTree make(JsonValue value) {
        PathTree res = new PathTree(Kind.VALUE);
        res.value = value;
        return res;
    }

    public static PathTree make(Kind kind, PathTree kid0) {
        PathTree res =  new PathTree(kind);
        res.kids = new ArrayList<PathTree>();
        res.kids.add(kid0);
        return res;
    }

    public static PathTree make(Kind kind, PathTree kid0, PathTree kid1) {
        PathTree res =  new PathTree(kind);
        res.kids = new ArrayList<PathTree>();
        res.kids.add(kid0);
        res.kids.add(kid1);
        return res;
    }

    public static PathTree make(Kind kind, List<PathTree> kids) {
        PathTree res =  new PathTree(kind);
        res.kids = kids;
        return res;
    }
}
