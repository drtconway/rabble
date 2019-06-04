package org.petermac.rabble;

class PathToken {
  public enum Kind {
    NAME, STRING, NUMBER, LPAREN, RPAREN, LBRAC, RBRAC, AT, COMMA, BANG,
    PLUS, MINUS, TIMES, SLASH, DSLASH, MOD, LT, LE, EQ, NE, GE, GT,
    AND, OR, DOLLAR
  }
}
