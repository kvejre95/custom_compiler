package edu.ufl.cise.plpfa22;

import java.util.HashMap;
import java.util.regex.Pattern;

public class Lexer implements ILexer {
    String input;
    int position, column_number = 1, line_number = 1, counter = 0;
    boolean have_zero, isString, is_start, is_colon, is_comment, backslash;
    HashMap<String, IToken.Kind> keywords = new HashMap<>();

    public Lexer(String input) {
        this.input = input;
        this.position = 0;
        keywords.put("CONST", IToken.Kind.KW_CONST);
        keywords.put("VAR", IToken.Kind.KW_VAR);
        keywords.put("PROCEDURE", IToken.Kind.KW_PROCEDURE);
        keywords.put("CALL", IToken.Kind.KW_CALL);
        keywords.put("BEGIN", IToken.Kind.KW_BEGIN);
        keywords.put("END", IToken.Kind.KW_END);
        keywords.put("IF", IToken.Kind.KW_IF);
        keywords.put("THEN", IToken.Kind.KW_THEN);
        keywords.put("WHILE", IToken.Kind.KW_WHILE);
        keywords.put("DO", IToken.Kind.KW_DO);
        keywords.put("TRUE", IToken.Kind.BOOLEAN_LIT);
        keywords.put("FALSE", IToken.Kind.BOOLEAN_LIT);

    }

    @Override
    public IToken next() throws LexicalException {
        is_start = true;
        is_comment = false;
        is_colon = false;
        isString = false;
        backslash = false;

        have_zero = false;
        Token t = new Token();
        //EOF
        if (position >= input.length()) {
            t.setKind(IToken.Kind.EOF);
            t.setLocation(column_number, line_number);
            t.setValue("");
        } else {
            for (int i = position; i < input.length(); i++) {
                char c = input.charAt(i);
                counter++;
                if (c == '0') {
                    if (!is_comment) {
                        if (is_start) {
                            t.setKind(IToken.Kind.NUM_LIT);
                            t.setLocation(column_number, line_number);
                            t.setValue("0");
                            t.ActualText.add(c);
                            is_start = false;
                            have_zero = true;
                        } else if ((t.kind == IToken.Kind.NUM_LIT && !have_zero) || t.kind == IToken.Kind.IDENT || isString) {
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                        } else if (is_colon) {
                            t.setKind(IToken.Kind.ERROR);
                            break;
                        } else if (backslash) {
                            t.setKind(IToken.Kind.ERROR);
                            backslash = false;
                            break;
                        } else {
                            position = i;
                            column_number = column_number + counter - 1;
                            counter = 0;
                            break;
                        }
                    }
                } else if (Pattern.matches("[1-9]", Character.toString(c))) {
                    if (!is_comment) {
                        if (is_start) {
                            t.setKind(IToken.Kind.NUM_LIT);
                            t.setLocation(column_number, line_number);
                            t.setValue(Character.toString(c));
                            t.ActualText.add(c);
                            is_start = false;
                        } else if ((t.getKind() == IToken.Kind.NUM_LIT && !have_zero) || t.getKind() == IToken.Kind.IDENT || isString) {
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                        } else if (is_colon) {
                            t.setKind(IToken.Kind.ERROR);
                            break;
                        } else if (backslash) {
                            t.setKind(IToken.Kind.ERROR);
                            backslash = false;
                            break;
                        } else {
                            position = i;
                            column_number = column_number + counter - 1;
                            counter = 0;
                            break;
                        }
                    }
                } else if (Pattern.matches("[a-zA-Z_$]", Character.toString(c))) {
                    if (!is_comment) {
                        if (is_start) {
                            t.setKind(IToken.Kind.IDENT);
                            t.setLocation(column_number, line_number);
                            t.setValue(Character.toString(c));
                            t.ActualText.add(c);
                            is_start = false;
                        } else if (t.getKind() == IToken.Kind.IDENT) {
                            t.ActualText.add(c);
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                        } else if (isString) {
                            if (backslash) {
                                if (c == 'b') {
                                    char bc = 8;
                                    StringBuilder sb = new StringBuilder();
                                    t.value = sb.append(t.value).append(bc).toString();
                                    t.ActualText.add(c);
                                    backslash = false;
                                } else if (c == 't') {
                                    char bc = 9;
                                    StringBuilder sb = new StringBuilder();
                                    t.value = sb.append(t.value).append(bc).toString();
                                    t.ActualText.add(c);
                                    backslash = false;
                                } else if (c == 'n') {
                                    char bc = 10;
                                    StringBuilder sb = new StringBuilder();
                                    t.value = sb.append(t.value).append(bc).toString();
                                    t.ActualText.add(c);
                                    backslash = false;
                                } else if (c == 'f') {
                                    char bc = 12;
                                    StringBuilder sb = new StringBuilder();
                                    t.value = sb.append(t.value).append(bc).toString();
                                    t.ActualText.add(c);
                                    backslash = false;
                                } else if (c == 'r') {
                                    char bc = 13;
                                    StringBuilder sb = new StringBuilder();
                                    t.value = sb.append(t.value).append(bc).toString();
                                    t.ActualText.add(c);
                                    backslash = false;
                                } else {
                                    t.setKind(IToken.Kind.ERROR);
                                    break;
                                }
                            } else {
                                StringBuilder sb = new StringBuilder();
                                t.value = sb.append(t.value).append(c).toString();
                                t.ActualText.add(c);
                            }
                        } else if (is_colon) {
                            t.setKind(IToken.Kind.ERROR);
                            break;
                        } else {
                            position = i;
                            column_number = column_number + counter - 1;
                            counter = 0;
                            break;
                        }
                    }
                } else if (Pattern.matches("[.,;()+%?#!*-]", Character.toString(c))) {
                    if (!is_comment) {
                        if (is_start) {
                            if (c == '.') {
                                t.setKind(IToken.Kind.DOT);
                            } else if (c == ',') {
                                t.setKind(IToken.Kind.COMMA);
                            } else if (c == ';') {
                                t.setKind(IToken.Kind.SEMI);
                            } else if (c == '(') {
                                t.setKind(IToken.Kind.LPAREN);
                            } else if (c == ')') {
                                t.setKind(IToken.Kind.RPAREN);
                            } else if (c == '+') {
                                t.setKind(IToken.Kind.PLUS);
                            } else if (c == '%') {
                                t.setKind(IToken.Kind.MOD);
                            } else if (c == '?') {
                                t.setKind(IToken.Kind.QUESTION);
                            } else if (c == '#') {
                                t.setKind(IToken.Kind.NEQ);
                            } else if (c == '!') {
                                t.setKind(IToken.Kind.BANG);
                            } else if (c == '*') {
                                t.setKind(IToken.Kind.TIMES);
                            } else if (c == '-') {
                                t.setKind(IToken.Kind.MINUS);
                            }
                            t.setLocation(column_number, line_number);
                            t.setValue(Character.toString(c));
                            t.ActualText.add(c);
                            position = i + 1;
                            column_number = column_number + 1;
                            counter = 0;
                            break;
                        } else if (isString) {
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                        } else if (is_colon) {
                            t.setKind(IToken.Kind.ERROR);
                            break;
                        } else {
                            position = i;
                            column_number = column_number + counter - 1;
                            counter = 0;
                            break;
                        }
                    }
                } else if (c == '/') {
                    if (!is_comment) {
                        if (is_start) {
                            t.setKind(IToken.Kind.DIV);
                            t.setLocation(column_number, line_number);
                            t.setValue(Character.toString(c));
                            t.ActualText.add(c);
                            is_start = false;
                        } else if (t.getKind() == IToken.Kind.DIV) {
                            t.setKind(null);
                            //t.setValue(null);
                            t.ActualText.clear();
                            is_comment = true;
                        } else if (is_colon) {
                            t.setKind(IToken.Kind.ERROR);
                            break;
                        } else if (isString) {
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                        } else {
                            position = i;
                            column_number = column_number + counter - 1;
                            counter = 0;
                            break;
                        }
                    }
                } else if (c == '<') {
                    if (!is_comment) {
                        if (is_start) {
                            t.setKind(IToken.Kind.LT);
                            t.setLocation(column_number, line_number);
                            t.setValue(Character.toString(c));
                            t.ActualText.add(c);
                            is_start = false;
                        } else if (is_colon) {
                            t.setKind(IToken.Kind.ERROR);
                            break;
                        } else if (isString) {
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                        } else {
                            position = i;
                            column_number = column_number + counter - 1;
                            counter = 0;
                            break;
                        }
                    }
                } else if (c == '>') {
                    if (!is_comment) {
                        if (is_start) {
                            t.setKind(IToken.Kind.GT);
                            t.setLocation(column_number, line_number);
                            t.setValue(Character.toString(c));
                            t.ActualText.add(c);
                            is_start = false;
                        } else if (is_colon) {
                            t.setKind(IToken.Kind.ERROR);
                            break;
                        } else if (isString) {
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                        } else {
                            position = i;
                            column_number = column_number + counter - 1;
                            counter = 0;
                            break;
                        }
                    }
                } else if (c == ':') {
                    {
                        if (is_start) {
                            is_colon = true;
                            t.setLocation(column_number, line_number);
                            t.setValue(Character.toString(c));
                            t.ActualText.add(c);
                            is_start = false;
                        } else if (is_colon) {
                            t.setKind(IToken.Kind.ERROR);
                            break;
                        } else if (isString) {
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                        } else {
                            position = i;
                            column_number = column_number + counter - 1;
                            counter = 0;
                            break;
                        }
                    }
                } else if (c == '=') {
                    if (!is_comment) {
                        if (is_start) {
                            t.setKind(IToken.Kind.EQ);
                            t.setLocation(column_number, line_number);
                            t.setValue(Character.toString(c));
                            t.ActualText.add(c);
                            position = i + 1;
                            column_number = column_number + 1;
                            counter = 0;
                            break;

                        } else if (t.getKind() == IToken.Kind.GT) {
                            t.setKind(IToken.Kind.GE);
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                            position = i + 1;
                            column_number = column_number + 2;
                            counter = 0;
                            break;

                        } else if (t.getKind() == IToken.Kind.LT) {
                            t.setKind(IToken.Kind.LE);
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                            position = i + 1;
                            column_number = column_number + 2;
                            counter = 0;
                            break;

                        } else if (is_colon) {
                            t.setKind(IToken.Kind.ASSIGN);
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                            is_colon = false;
                            position = i + 1;
                            column_number = column_number + 2;
                            counter = 0;
                            break;
                        } else if (isString) {
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                        } else {
                            position = i;
                            column_number = column_number + counter - 1;
                            counter = 0;
                            break;
                        }
                        /*counter = 0;
                        break;*/
                    }
                } else if (c == ' ' || c == '\t' || c == '\r') {
                    if (!is_comment) {
                        if (isString) {
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                        } else {
                            position = i + 1;
                            column_number = column_number + counter;
                            counter = 0;
                            if (!is_start) {
                                break;
                            } else if (is_colon) {
                                t.setKind(IToken.Kind.ERROR);
                                break;
                            }
                        }
                    }
                } else if (c == '"') {
                    if (!is_comment) {
                        if (is_start) {
                            t.setKind(IToken.Kind.STRING_LIT);
                            t.setLocation(column_number, line_number);
                            //t.setValue("\"");
                            t.setValue("");
                            t.ActualText.add(c);
                            is_start = false;
                            isString = true;
                        } else if (isString && !backslash) {
                            //t.value=t.value + c;
                            t.ActualText.add(c);
                            position = i + 1;
                            column_number = column_number + counter;
                            counter = 0;
                            isString = false;
                            break;
                        } else if (backslash) {
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                            backslash = false;
                        } else {
                            position = i;
                            column_number = column_number + counter - 1;
                            counter = 0;
                            break;
                        }
                    }
                } else if (c == '\\') {
                    if (isString) {
                        t.ActualText.add(c);
                        if (!backslash) {
                            backslash = true;
                        } else {
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            backslash = false;
                        }
                    } else {
                        t.setKind(IToken.Kind.ERROR);
                        t.setLocation(column_number, line_number);
                        t.setValue("ERROR");
                        throw new LexicalException("Error in line " + line_number + ", and column " + column_number + ". Can't find token " + c);
                    }
                } else if (c == '\'') {
                    if (backslash) {
                        StringBuilder sb = new StringBuilder();
                        t.value = sb.append(t.value).append(c).toString();
                        t.ActualText.add(c);
                        backslash = false;
                    } else {
                        t.setKind(IToken.Kind.ERROR);
                        t.setLocation(column_number, line_number);
                        t.setValue("ERROR");
                        throw new LexicalException("Error in line " + line_number + ", and column " + column_number + ". Can't find token " + c);
                    }
                } else if (c == '\n') {
                    //System.out.println(c + "  " +i);
                    position = i + 1;
                    column_number = 1;
                    line_number = line_number + 1;
                    counter = 0;
                    if (!is_comment) {
                        if (!isString) {
                            if (!is_start) {
                                break;
                            } else if (is_colon) {
                                t.setKind(IToken.Kind.ERROR);
                                break;
                            }
                        } else {
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                        }
                    } else {
                        is_comment = false;
                        is_start = true;
                    }
                } else {
                    if (!is_comment) {
                        if (isString) {
                            StringBuilder sb = new StringBuilder();
                            t.value = sb.append(t.value).append(c).toString();
                            t.ActualText.add(c);
                        } else if (is_start) {
                            t.setKind(IToken.Kind.ERROR);
                            t.setLocation(column_number, line_number);
                            t.setValue("ERROR");
                            throw new LexicalException("Error in line " + line_number + ", and column " + column_number + ". Can't find token " + c);
                        } else {
                            position = i;
                            column_number = column_number + counter - 1;
                            counter = 0;
                            break;
                        }
                    }
                }
            }
            if (position + counter >= input.length()) {
                position = position + counter;
            }
            if (isString || is_colon) {
                t.setKind(IToken.Kind.ERROR);
            }
        }
        if (t.getKind() == null && position >= input.length()) {
            t.setKind(IToken.Kind.EOF);
            t.setLocation(column_number, line_number);
            t.setValue("");
        }
        if (t.getKind() == IToken.Kind.NUM_LIT) {
            try {
                Integer.parseInt(t.value);
            } catch (RuntimeException re) {
                throw new LexicalException(re);
            }
        } else if (t.getKind() == IToken.Kind.IDENT) {
            if (keywords.get(t.value) != null) {
                t.setKind(keywords.get(t.value));
            }
        } else if (t.getKind() == IToken.Kind.ERROR) {
            throw new LexicalException("Error in line " + line_number + ", and column " + column_number + ".");
        }
        return t;
    }

    @Override
    public IToken peek() throws LexicalException {
        return null;
    }
}
