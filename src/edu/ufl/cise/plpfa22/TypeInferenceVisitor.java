package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.ast.*;

public class TypeInferenceVisitor implements ASTVisitor {

    int isChange = 1;
    StringBuilder changesDone = new StringBuilder();

    @Override
    public Object visitBlock(Block block, Object arg) throws PLPException {
        for (ConstDec constDec : block.constDecs) {
            constDec.visit(this, arg);
        }
        for (VarDec varDec : block.varDecs) {
            varDec.visit(this, arg);
        }

        for (ProcDec procDec : block.procedureDecs) {
            procDec.visit(this, arg);
        }
        for (ProcDec procDec : block.procedureDecs) {
            if (procDec.block != null) {
                procDec.block.visit(this, arg);
            }
        }
        block.statement.visit(this, arg);
        return null;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLPException {
        program.block.visit(this, arg);
        while (isChange > 0) {
            isChange = 0;
            program.block.visit(this, arg);
        }

        return null;
    }

    @Override
    public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
        if (statementAssign.expression.getType() == Types.Type.PROCEDURE || statementAssign.ident.getDec().getType() == Types.Type.PROCEDURE) {
            throw new TypeCheckException("Can't use Procedure name.");
        } else if (statementAssign.ident.getDec().getType() == null) {
            if (statementAssign.expression.getType() == null) {
                statementAssign.expression.visit(this, arg);
            }
            if (statementAssign.expression.getType() != null) {
                statementAssign.ident.getDec().setType(statementAssign.expression.getType());
            }
        } else {
            if (statementAssign.ident.getDec().firstToken.getKind() == IToken.Kind.KW_CONST) {
                throw new TypeCheckException("Can't reassign Constants.");
            } else if (statementAssign.expression.getType() == null) {
                statementAssign.expression.visit(this, arg);
                if (statementAssign.expression.getType() == null) {
                    statementAssign.expression.setType(statementAssign.ident.getDec().getType());
                }
            }
            if (statementAssign.expression.getType() != null) {
                if (statementAssign.ident.getDec().getType() != statementAssign.expression.getType()) {
                    throw new TypeCheckException("Types Don't match");
                }
            }
        }
        return null;
    }

    @Override
    public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
        return null;
    }

    @Override
    public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
        if (statementCall.ident.getDec().getType() == null && statementCall.ident.getDec().firstToken.getKind() == IToken.Kind.KW_PROCEDURE) {
            statementCall.ident.getDec().setType(Types.Type.PROCEDURE);
        } else if (statementCall.ident.getDec().getType() != Types.Type.PROCEDURE) {
            throw new TypeCheckException("Can't call anything other than procedure.");
        }
        return null;
    }

    @Override
    public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
        if (statementInput.ident.getDec().firstToken.getKind() == IToken.Kind.KW_PROCEDURE) {
            throw new TypeCheckException("Can't take procedure as input");
        } else if (statementInput.ident.getDec().firstToken.getKind() == IToken.Kind.KW_CONST) {
            throw new TypeCheckException("Can't take Constant as input");
        } else if (statementInput.ident.getDec().getType() == null && isChange == 0) {
            throw new TypeCheckException("Variable not assigned");
        }
        return null;
    }

    @Override
    public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
        if (statementOutput.expression.getType() == null) {
            statementOutput.expression.visit(this, null);
        }
        if (statementOutput.expression.getType() == Types.Type.PROCEDURE) {
            throw new TypeCheckException("Can't take procedure as input");
        } else if (statementOutput.expression.getType() == null && isChange == 0) {
            throw new TypeCheckException("Variable Type not defined.");
        }
        return null;
    }

    @Override
    public Object visitStatementBlock(StatementBlock statementBlock, Object arg) throws PLPException {
        for (Statement each_statement : statementBlock.statements) {
            each_statement.visit(this, arg);
        }
        return null;
    }

    @Override
    public Object visitStatementIf(StatementIf statementIf, Object arg) throws PLPException {
        if (statementIf.expression.getType() == null) {
            statementIf.expression.visit(this, null);
        } else {
            if (statementIf.expression.getType() != Types.Type.BOOLEAN) {
                throw new TypeCheckException("expression should return boolean for if statements");
            }
        }
        statementIf.statement.visit(this, arg);
        return null;
    }

    @Override
    public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
        if (statementWhile.expression.getType() == null) {
            statementWhile.expression.visit(this, null);
        } else {
            if (statementWhile.expression.getType() != Types.Type.BOOLEAN) {
                throw new TypeCheckException("expression should return boolean for if statements");
            }
        }
        statementWhile.statement.visit(this, arg);
        return null;
    }

    @Override
    public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {

        if (expressionBinary.e0.getType() == null) {
            expressionBinary.e0.visit(this, "Subexpression");
        }
        if (expressionBinary.e1.getType() == null) {
            expressionBinary.e1.visit(this, "Subexpression");
        }
        if (expressionBinary.e0.getType() == Types.Type.PROCEDURE || expressionBinary.e1.getType() == Types.Type.PROCEDURE) {
            throw new TypeCheckException("Can't use operator on Procedures.");
        } else if (expressionBinary.e0.getType() == null && expressionBinary.e1.getType() == null && isChange == 0 && arg != "Subexpression") {
            System.out.println(changesDone);
            throw new TypeCheckException("Can't Infer the type of expression");
        }
        if (expressionBinary.e0.getType() != null || expressionBinary.e1.getType() != null) {
            if (expressionBinary.op.getKind() == IToken.Kind.PLUS) {
                if (expressionBinary.e0.getType() != null && expressionBinary.e1.getType() == null) {
                    expressionBinary.e1.setType(expressionBinary.e0.getType());
                    expressionBinary.e1.visit(this, null);
                } else if (expressionBinary.e0.getType() == null && expressionBinary.e1.getType() != null) {
                    expressionBinary.e0.setType(expressionBinary.e1.getType());
                    expressionBinary.e0.visit(this, null);
                } else if (expressionBinary.e0.getType() != null && expressionBinary.e1.getType() != null) {
                    if (expressionBinary.e0.getType() == expressionBinary.e1.getType())
                        expressionBinary.setType(expressionBinary.e0.getType());
                    else
                        throw new TypeCheckException("Type Error.");
                }
            } else if (expressionBinary.op.getKind() == IToken.Kind.MINUS || expressionBinary.op.getKind() == IToken.Kind.DIV || expressionBinary.op.getKind() == IToken.Kind.MOD) {
                if (expressionBinary.e0.getType() != null && expressionBinary.e1.getType() == null) {
                    expressionBinary.e1.setType(expressionBinary.e0.getType());
                    expressionBinary.e1.visit(this, null);
                } else if (expressionBinary.e0.getType() == null && expressionBinary.e1.getType() != null) {
                    expressionBinary.e0.setType(expressionBinary.e1.getType());
                    expressionBinary.e0.visit(this, null);
                } else if (expressionBinary.e0.getType() != null && expressionBinary.e1.getType() != null) {
                    if (expressionBinary.e0.getType() == Types.Type.NUMBER && expressionBinary.e1.getType() == Types.Type.NUMBER)
                        expressionBinary.setType(Types.Type.NUMBER);
                    else
                        throw new TypeCheckException("Numbers only.");
                }
            } else if (expressionBinary.op.getKind() == IToken.Kind.TIMES) {
                if (expressionBinary.e0.getType() == Types.Type.STRING || expressionBinary.e1.getType() == Types.Type.STRING) {
                    throw new TypeCheckException("Can't use operator on Procedures.");
                }
                if (expressionBinary.e0.getType() != null && expressionBinary.e1.getType() == null) {
                    expressionBinary.e1.setType(expressionBinary.e0.getType());
                    expressionBinary.e1.visit(this, null);
                } else if (expressionBinary.e0.getType() == null && expressionBinary.e1.getType() != null) {
                    expressionBinary.e0.setType(expressionBinary.e1.getType());
                    expressionBinary.e0.visit(this, null);
                } else if (expressionBinary.e0.getType() != null && expressionBinary.e1.getType() != null) {
                    if (expressionBinary.e0.getType() == expressionBinary.e1.getType())
                        expressionBinary.setType(expressionBinary.e0.getType());
                    else
                        throw new TypeCheckException("Type Error.");
                }
            } else if (expressionBinary.op.getKind() == IToken.Kind.EQ
                    || expressionBinary.op.getKind() == IToken.Kind.NEQ
                    || expressionBinary.op.getKind() == IToken.Kind.LT
                    || expressionBinary.op.getKind() == IToken.Kind.LE
                    || expressionBinary.op.getKind() == IToken.Kind.GT
                    || expressionBinary.op.getKind() == IToken.Kind.GE) {
                if (expressionBinary.e0.getType() != null && expressionBinary.e1.getType() == null) {
                    expressionBinary.e1.setType(expressionBinary.e0.getType());
                    expressionBinary.e1.visit(this, null);
                }
                if (expressionBinary.e0.getType() == null && expressionBinary.e1.getType() != null) {
                    expressionBinary.e0.setType(expressionBinary.e1.getType());
                    expressionBinary.e0.visit(this, null);
                }
                if (expressionBinary.e0.getType() != null && expressionBinary.e1.getType() != null) {
                    if (expressionBinary.e0.getType() == expressionBinary.e1.getType())
                        expressionBinary.setType(Types.Type.BOOLEAN);
                    else
                        throw new TypeCheckException("Type Error.");
                }

            }
        }else {
            if(expressionBinary.getType() != null){
                expressionBinary.e0.setType(expressionBinary.getType());
                expressionBinary.e0.visit(this,arg);
                expressionBinary.e1.setType(expressionBinary.getType());
                expressionBinary.e1.visit(this,arg);
            }
        }



        return null;
    }

    @Override
    public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
        if (expressionIdent.getType() == null && expressionIdent.getDec().getType() == null){

        }else {
            if (expressionIdent.getType() == null) {
                expressionIdent.setType(expressionIdent.getDec().getType());
                isChange++;
                changesDone.append(expressionIdent.firstToken.getStringValue() +" - " +  expressionIdent.getType()+"\n");
            }
            if (expressionIdent.getDec().getType() == null) {
                expressionIdent.getDec().setType(expressionIdent.getType());
                isChange++;
                changesDone.append(expressionIdent.getDec().firstToken.getStringValue()  +" " +expressionIdent.firstToken.getStringValue()+" - " +  expressionIdent.getDec().getType()+"\n");
            }
        }
        return null;
    }

    @Override
    public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
        if (expressionNumLit.getType() == null) {
            expressionNumLit.setType(Types.Type.NUMBER);
            isChange++;
            changesDone.append(expressionNumLit.firstToken.getStringValue()+ " - " + expressionNumLit.getType()+"\n");
        }
        return null;
    }

    @Override
    public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
        if (expressionStringLit.getType() == null) {
            expressionStringLit.setType(Types.Type.STRING);
            isChange++;
            changesDone.append(expressionStringLit.firstToken.getStringValue()+ " - " + expressionStringLit.getType()+"\n");
        }
        return null;
    }

    @Override
    public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
        if (expressionBooleanLit.getType() == null) {
            expressionBooleanLit.setType(Types.Type.BOOLEAN);
            isChange++;
            changesDone.append(expressionBooleanLit.firstToken.getStringValue()+ " - " + expressionBooleanLit.getType()+"\n");
        }
        return null;
    }

    @Override
    public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
        if (procDec.getType() == null) {
            procDec.setType(Types.Type.PROCEDURE);
            isChange++;
            changesDone.append(procDec.firstToken.getStringValue()+" " +procDec.ident.getStringValue()+" - " +  procDec.getType()+"\n");
        }
        return null;
    }

    @Override
    public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
        if (constDec.getType() == null) {
            if (constDec.val.getClass() == Integer.class) {
                constDec.setType(Types.Type.NUMBER);
                isChange++;
                changesDone.append(constDec.firstToken.getStringValue()+" " +constDec.ident.getStringValue()+" - " +  constDec.getType()+"\n");
            } else if (constDec.val.getClass() == Boolean.class) {
                constDec.setType(Types.Type.BOOLEAN);
                isChange++;
                changesDone.append(constDec.firstToken.getStringValue()+" " +constDec.ident.getStringValue()+" - " +  constDec.getType()+"\n");
            } else if (constDec.val.getClass() == String.class) {
                constDec.setType(Types.Type.STRING);
                isChange++;
                changesDone.append(constDec.firstToken.getStringValue()+" " +constDec.ident.getStringValue()+" - " + constDec.getType()+"\n");
            }
        }
        return null;
    }

    @Override
    public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
        return null;
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLPException {
        return null;
    }
}
