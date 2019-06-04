package org.petermac.rabble;

import java.util.ArrayList;
import java.util.List;

class PathTree {
    public PathToken.Kind kind;
    public String valu;
    public List<PathTree> kids;

    private PathTree(PathToken.Kind kind) {
        this.kind = kind;
        this.valu = null;
        this.kids = null;
    }

    private PathTree(PathToken.Kind kind, String valu) {
        this.kind = kind;
        this.valu = valu;
        this.kids = null;
    }

    private PathTree(PathToken.Kind kind, List<PathTree> kids) {
        this.kind = kind;
        this.valu = null;
        this.kids = kids;
    }

    public static PathTree make(PathToken.Kind kind) {
        return new PathTree(kind);
    }

    public static PathTree make(PathToken.Kind kind, String valu) {
        return new PathTree(kind, valu);
    }

    public static PathTree make(PathToken.Kind kind, PathTree kid0) {
        List<PathTree> kids = new ArrayList<PathTree>();
        kids.add(kid0);
        return new PathTree(kind, kids);
    }

    public static PathTree make(PathToken.Kind kind, PathTree kid0, PathTree kid1) {
        List<PathTree> kids = new ArrayList<PathTree>();
        kids.add(kid0);
        kids.add(kid1);
        return new PathTree(kind, kids);
    }

    public static PathTree make(PathToken.Kind kind, List<PathTree> kids) {
        return new PathTree(kind, kids);
    }
}
