package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.ast.*;

import java.util.*;

public class ScopeVisitor implements ASTVisitor {
    int scopeID = -1, nestLevel = -1;
    //Map<String, int[]> scopeTable = new HashMap<>();
    Map<String, Map<Integer,Map<Integer,Declaration>>> scopeTable = new HashMap<>();
    Stack<Integer> stack = new Stack<>();

    @Override
    public Object visitBlock(Block block, Object arg) throws PLPException {
        enterScope();

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
        leaveScope();
        return null;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLPException {
        program.block.visit(this, arg);
        return null;
    }

    @Override
    public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
        statementAssign.expression.visit(this, arg);
        statementAssign.ident.visit(this, arg);

        return null;
    }

    @Override
    public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
        if (!scopeTable.containsKey(varDec.ident.getStringValue())) {
            Integer scope = Integer.valueOf(scopeID);
            Integer nest = Integer.valueOf(nestLevel);
            Map<Integer,Declaration> innerMap = new HashMap<>();
            innerMap.put(nest, varDec);
            Map<Integer,Map<Integer,Declaration>> middleMap = new HashMap<>();
            middleMap.put(scope, innerMap);
            scopeTable.put(varDec.ident.getStringValue(),middleMap);
            varDec.setNest(nestLevel);
        } else {
            if(scopeTable.get(varDec.ident.getStringValue()).containsKey(scopeID)){
                throw new ScopeException("This Variable already exists in this block");
            }else{
                Integer scope = Integer.valueOf(scopeID);
                Integer nest = Integer.valueOf(nestLevel);
                Map<Integer,Declaration> innerMap = new HashMap<>();
                innerMap.put(nest, varDec);
                Map<Integer,Map<Integer,Declaration>> middleMap = scopeTable.get(varDec.ident.getStringValue());
                middleMap.put(scope, innerMap);
                scopeTable.put(varDec.ident.getStringValue(),middleMap);
                varDec.setNest(nestLevel);
            }
        }
        return null;
    }

    @Override
    public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
        statementCall.ident.visit(this, arg);
        return null;
    }

    @Override
    public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
        statementInput.ident.visit(this, arg);
        return null;
    }

    @Override
    public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
        statementOutput.expression.visit(this, arg);
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
        statementIf.expression.visit(this, arg);
        statementIf.statement.visit(this, arg);
        return null;
    }

    @Override
    public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
        statementWhile.expression.visit(this, arg);
        statementWhile.statement.visit(this, arg);
        return null;
    }

    @Override
    public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
        expressionBinary.e0.visit(this, arg);
        expressionBinary.e1.visit(this, arg);
        return null;
    }

    @Override
    public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
        String check = expressionIdent.firstToken.getStringValue();

        boolean variableInScope=false;
        Stack <Integer> tempStack = (Stack<Integer>) stack.clone();
        if(scopeTable.containsKey(check)){
            while (!tempStack.isEmpty()) {
                int topOfStack = tempStack.peek();
                if(scopeTable.get(check).containsKey(topOfStack)){
                    Map <Integer,Declaration> innerMostMap = scopeTable.get(check).get(tempStack.pop());
                    Map.Entry<Integer,Declaration> entry = innerMostMap.entrySet().iterator().next();
                    Declaration dec = entry.getValue();
                    expressionIdent.setDec(dec);
                    expressionIdent.setNest(nestLevel);
                    variableInScope = true;
                    break;
                }
                else {
                    tempStack.pop();
                }
            }
        }
        else{
            throw new ScopeException("Variable not declared");
        }
        if (!variableInScope){
            throw  new ScopeException("Variable out of scope");
        }

        return null;
    }

    @Override
    public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
        return null;
    }

    @Override
    public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
        return null;
    }

    @Override
    public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
        return null;
    }

    @Override
    public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
        if (!scopeTable.containsKey(procDec.ident.getStringValue())) {
            Integer scope = Integer.valueOf(scopeID);
            Integer nest = Integer.valueOf(nestLevel);
            Map<Integer,Declaration> innerMap = new HashMap<>();
            innerMap.put(nest, procDec);
            Map<Integer,Map<Integer,Declaration>> middleMap = new HashMap<>();
            middleMap.put(scope, innerMap);
            scopeTable.put(procDec.ident.getStringValue(),middleMap);
            procDec.setNest(nestLevel);
        } else {
            if(scopeTable.get(procDec.ident.getStringValue()).containsKey(scopeID)){
                throw new ScopeException("This Variable already exists in this block");
            }else{
                Integer scope = Integer.valueOf(scopeID);
                Integer nest = Integer.valueOf(nestLevel);
                Map<Integer,Declaration> innerMap = new HashMap<>();
                innerMap.put(nest, procDec);
                Map<Integer,Map<Integer,Declaration>> middleMap = scopeTable.get(procDec.ident.getStringValue());
                middleMap.put(scope, innerMap);
                scopeTable.put(procDec.ident.getStringValue(),middleMap);
                procDec.setNest(nestLevel);
            }
        }
        return null;
    }

    @Override
    public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
        if (!scopeTable.containsKey(constDec.ident.getStringValue())) {
            Integer scope = Integer.valueOf(scopeID);
            Integer nest = Integer.valueOf(nestLevel);
            Map<Integer,Declaration> innerMap = new HashMap<>();
            innerMap.put(nest, constDec);
            Map<Integer,Map<Integer,Declaration>> middleMap = new HashMap<>();
            middleMap.put(scope, innerMap);
            scopeTable.put(constDec.ident.getStringValue(),middleMap);
            constDec.setNest(nestLevel);

        } else {
            if(scopeTable.get(constDec.ident.getStringValue()).containsKey(scopeID)){
                throw new ScopeException("This Variable already exists in this block");
            }else{
                Integer scope = Integer.valueOf(scopeID);
                Integer nest = Integer.valueOf(nestLevel);
                Map<Integer,Declaration> innerMap = new HashMap<>();
                innerMap.put(nest, constDec);
                Map<Integer,Map<Integer,Declaration>> middleMap = scopeTable.get(constDec.ident.getStringValue());
                middleMap.put(scope, innerMap);
                scopeTable.put(constDec.ident.getStringValue(),middleMap);
                constDec.setNest(nestLevel);
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
        String check = ident.firstToken.getStringValue();

        boolean variableInScope=false;
        Stack <Integer> tempStack = (Stack<Integer>) stack.clone();
        if(scopeTable.containsKey(check)){
            while (!tempStack.isEmpty()) {
                int topOfStack = tempStack.peek();
                if(scopeTable.get(check).containsKey(topOfStack)){
                    Map <Integer,Declaration> innerMostMap = scopeTable.get(check).get(tempStack.pop());
                    Map.Entry<Integer,Declaration> entry = innerMostMap.entrySet().iterator().next();
                    Declaration dec = entry.getValue();
                    ident.setDec(dec);
                    ident.setNest(nestLevel);
                    variableInScope = true;
                    break;
                }
                else{
                    tempStack.pop();
                }
            }
        }
        else{
            throw new ScopeException("Variable not declared");
        }
        if (!variableInScope){
            throw  new ScopeException("Variable out of scope");
        }
        return null;
    }

    private void enterScope() {
        scopeID++;
        stack.push(scopeID);
        nestLevel++;
    }

    private void leaveScope() {
        stack.pop();
        nestLevel--;
    }



}
