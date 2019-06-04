package org.petermac.rabble;

/**
 * The Path class is for representing paths in JsonValues.
 *
 * Paths obey the following grammar:
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
public class Path {

    /**
     * Parse a path string to create a new path object.
     *
     * @param pathStr is a valid path string
     */
    public Path(String pathStr) throws Exception {
    }

    /**
     * Return the string representation of the current path.
     *
     * @return a string form of the path expression.
     */
    public String asString() {
        return null;
    }

}
