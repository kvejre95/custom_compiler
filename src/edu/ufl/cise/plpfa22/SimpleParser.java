package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.ast.*;

import java.util.ArrayList;
import java.util.List;

public class SimpleParser implements IParser {
    ILexer scanner;
    IToken t;

    public SimpleParser(ILexer scanner) throws LexicalException {
        this.scanner = scanner;
        t = scanner.next();
    }

    @Override
    public ASTNode parse() throws PLPException {
        return program();
    }

    private boolean isKind(IToken t, IToken.Kind kind) {
        return t.getKind() == kind;
    }

    public IToken consume() throws LexicalException {
        t = scanner.next();
        return t;
    }

    public IToken match(IToken.Kind kind) throws PLPException {
        if (isKind(t, kind)) {
            return consume();
        } else {
            throw new SyntaxException("Syntax Error");
        }
    }

    private ASTNode program() throws PLPException {
        IToken firstToken = t;
        ASTNode an;
        an = new Program(firstToken, (Block) block());
        match(IToken.Kind.DOT);
        if (t.getKind() == IToken.Kind.EOF) {
            return an;
        } else {
            throw new SyntaxException("Program should terminate after .");
        }
    }

    private ASTNode block() throws PLPException {
        IToken firstToken = t;
        ASTNode an;
        List<ConstDec> constDecs = new ArrayList<ConstDec>();
        List<ProcDec> procDecs = new ArrayList<ProcDec>();
        List<VarDec> varDecs = new ArrayList<VarDec>();

        while (isKind(t, IToken.Kind.KW_CONST)) {
            firstToken = t;
            match(IToken.Kind.KW_CONST);
            IToken tempToken  = t;
            match(IToken.Kind.IDENT);
            match(IToken.Kind.EQ);
            Expression e = (Expression) const_val();
            IToken temp = e.firstToken;
            ConstDec constDec = null;
            if(temp.getKind()==IToken.Kind.NUM_LIT){
                 constDec = new ConstDec(firstToken,tempToken,temp.getIntValue());
            } else if(temp.getKind()==IToken.Kind.STRING_LIT){
                 constDec = new ConstDec(firstToken,tempToken,temp.getStringValue());
            } else if (temp.getKind() == IToken.Kind.BOOLEAN_LIT) {
                constDec = new ConstDec(firstToken,tempToken,temp.getBooleanValue());
            }
            constDecs.add(constDec);
            while (isKind(t, IToken.Kind.COMMA)) {
                match(IToken.Kind.COMMA);
                tempToken= t;
                match(IToken.Kind.IDENT);
                match(IToken.Kind.EQ);
                e = (Expression) const_val();
                temp = e.firstToken;
                constDec = null;
                if(temp.getKind()==IToken.Kind.NUM_LIT){
                    constDec = new ConstDec(firstToken,tempToken,temp.getIntValue());
                } else if(temp.getKind()==IToken.Kind.STRING_LIT){
                    constDec = new ConstDec(firstToken,tempToken,temp.getStringValue());
                } else if (temp.getKind() == IToken.Kind.BOOLEAN_LIT) {
                    constDec = new ConstDec(firstToken,tempToken,temp.getBooleanValue());
                }
                constDecs.add(constDec);
            }
            match(IToken.Kind.SEMI);
        }

        while (isKind(t, IToken.Kind.KW_VAR)) {
            firstToken = t;
            match(IToken.Kind.KW_VAR);
            IToken tempToken = t;
            match(IToken.Kind.IDENT);
            VarDec varDec = new VarDec(firstToken, tempToken);
            varDecs.add(varDec);
            while (isKind(t, IToken.Kind.COMMA)) {
                match(IToken.Kind.COMMA);
                tempToken = t;
                match(IToken.Kind.IDENT);
                varDec = new VarDec(firstToken, tempToken);
                varDecs.add(varDec);
            }
            match(IToken.Kind.SEMI);
        }

        while (isKind(t, IToken.Kind.KW_PROCEDURE)) {
            firstToken = t;
            match(IToken.Kind.KW_PROCEDURE);
            IToken tempToken  = t;
            match(IToken.Kind.IDENT);
            match(IToken.Kind.SEMI);
            ProcDec procDec = new ProcDec(firstToken, tempToken, (Block) block());
            procDecs.add(procDec);
            match(IToken.Kind.SEMI);
        }
        Statement s = (Statement) statement();
        an = new Block(firstToken, constDecs, varDecs, procDecs, s);
        return an;
    }

    private ASTNode statement() throws PLPException {
        IToken firstToken = t;
        ASTNode an;
        if (isKind(t, IToken.Kind.IDENT)) {
            Ident ident = new Ident(t);
            match(IToken.Kind.IDENT);
            match(IToken.Kind.ASSIGN);
            Expression e = (Expression) expression();
            an = new StatementAssign(firstToken, ident, e);
        } else if (isKind(t, IToken.Kind.KW_CALL)) {
            match(IToken.Kind.KW_CALL);
            Ident ident = new Ident(t);
            match(IToken.Kind.IDENT);
            an = new StatementCall(firstToken, ident);
        } else if (isKind(t, IToken.Kind.QUESTION)) {
            match(IToken.Kind.QUESTION);
            Ident ident = new Ident(t);
            match(IToken.Kind.IDENT);
            an = new StatementInput(firstToken, ident);
        } else if (isKind(t, IToken.Kind.BANG)) {
            match(IToken.Kind.BANG);
            Expression e = (Expression) expression();
            an = new StatementOutput(firstToken, e);
        } else if (isKind(t, IToken.Kind.KW_BEGIN)) {
            match(IToken.Kind.KW_BEGIN);
            List<Statement> s = new ArrayList<>();
            s.add((Statement) statement());
            while (isKind(t, IToken.Kind.SEMI)) {
                match(IToken.Kind.SEMI);
                s.add((Statement) statement());
            }
            match(IToken.Kind.KW_END);
            an = new StatementBlock(firstToken, s);
        } else if (isKind(t, IToken.Kind.KW_IF)) {
            match(IToken.Kind.KW_IF);
            Expression e = (Expression) expression();
            match(IToken.Kind.KW_THEN);
            Statement s = (Statement) statement();
            an = new StatementIf(firstToken, e, s);
        } else if (isKind(t, IToken.Kind.KW_WHILE)) {
            match(IToken.Kind.KW_WHILE);
            Expression e = (Expression) expression();
            match(IToken.Kind.KW_DO);
            Statement s = (Statement) statement();
            an = new StatementWhile(firstToken, e, s);
        } else {
            an = new StatementEmpty(firstToken);
        }
        return an;
    }

    private ASTNode expression() throws PLPException {
        IToken firstToken = t;
        ASTNode an_left, an_right;
        an_left = additive_expression();
        while (isKind(t, IToken.Kind.LT) || isKind(t, IToken.Kind.GT) || isKind(t, IToken.Kind.EQ) || isKind(t, IToken.Kind.NEQ) || isKind(t, IToken.Kind.LE) || isKind(t, IToken.Kind.GE)) {
            IToken op = t;
            if (isKind(t, IToken.Kind.LT)) {
                match(IToken.Kind.LT);
            } else if (isKind(t, IToken.Kind.GT)) {
                match(IToken.Kind.GT);
            } else if (isKind(t, IToken.Kind.EQ)) {
                match(IToken.Kind.EQ);
            } else if (isKind(t, IToken.Kind.NEQ)) {
                match(IToken.Kind.NEQ);
            } else if (isKind(t, IToken.Kind.LE)) {
                match(IToken.Kind.LE);
            } else if (isKind(t, IToken.Kind.GE)) {
                match(IToken.Kind.GE);
            }
            an_right = additive_expression();
            an_left = new ExpressionBinary(firstToken, (Expression) an_left, op, (Expression) an_right);
        }
        return an_left;
    }

    private ASTNode additive_expression() throws PLPException {
        IToken firstToken = t;
        ASTNode an_left, an_right;
        an_left = multiplicative_expression();
        while (isKind(t, IToken.Kind.PLUS) || isKind(t, IToken.Kind.MINUS)) {
            IToken op = t;
            if (isKind(t, IToken.Kind.PLUS)) {
                match(IToken.Kind.PLUS);
            } else if (isKind(t, IToken.Kind.MINUS)) {
                match(IToken.Kind.MINUS);
            }
            an_right = multiplicative_expression();
            an_left = new ExpressionBinary(firstToken, (Expression) an_left, op, (Expression) an_right);
        }
        return an_left;
    }

    private ASTNode multiplicative_expression() throws PLPException {
        IToken firstToken = t;
        ASTNode an_left, an_right;
        an_left = primary_expression();
        while (isKind(t, IToken.Kind.TIMES) || isKind(t, IToken.Kind.DIV) || isKind(t, IToken.Kind.MOD)) {
            IToken op = t;
            if (isKind(t, IToken.Kind.TIMES)) {
                match(IToken.Kind.TIMES);
            } else if (isKind(t, IToken.Kind.DIV)) {
                match(IToken.Kind.DIV);
            } else if (isKind(t, IToken.Kind.MOD)) {
                match(IToken.Kind.MOD);
            }
            an_right = primary_expression();
            an_left = new ExpressionBinary(firstToken, (Expression) an_left, op, (Expression) an_right);
        }
        return an_left;
    }

    private ASTNode primary_expression() throws PLPException {
        IToken firstToken = t;
        ASTNode an;
        if (isKind(t, IToken.Kind.IDENT)) {
            an = new ExpressionIdent(firstToken);
            match(IToken.Kind.IDENT);
        } else if (isKind(t, IToken.Kind.NUM_LIT) || isKind(t, IToken.Kind.STRING_LIT) || isKind(t, IToken.Kind.BOOLEAN_LIT)) {
            an = const_val();
        } else if (isKind(t, IToken.Kind.LPAREN)) {
            match(IToken.Kind.LPAREN);
            an = expression();
            match(IToken.Kind.RPAREN);
        } else {
            throw new SyntaxException("Error Found");
        }
        return an;
    }

    private ASTNode const_val() throws PLPException {
        IToken firstToken = t;
        ASTNode an;
        if (isKind(t, IToken.Kind.NUM_LIT)) {
            an = new ExpressionNumLit(firstToken);
            match(IToken.Kind.NUM_LIT);
        } else if (isKind(t, IToken.Kind.STRING_LIT)) {
            an = new ExpressionStringLit(firstToken);
            match(IToken.Kind.STRING_LIT);
        } else if (isKind(t, IToken.Kind.BOOLEAN_LIT)) {
            an = new ExpressionBooleanLit(firstToken);
            match(IToken.Kind.BOOLEAN_LIT);
        } else {
            throw new SyntaxException("Error Found");
        }
        return an;
    }
}
