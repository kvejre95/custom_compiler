package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.CodeGenUtils.GenClass;
import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.ast.*;
import edu.ufl.cise.plpfa22.ast.Types.Type;
import org.objectweb.asm.*;

import java.util.*;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	final String packageName;
	final String className;
	final String sourceFileName;
	final String fullyQualifiedClassName;
	final String classDesc;
	boolean firstProcVisit = true;
	String currentClass;

	HashMap<ProcDec, String> procDecList = new HashMap<>();

	ClassWriter classWriter;
	List<GenClass> output = new ArrayList<>();

	public CodeGenVisitor(String className, String packageName, String sourceFileName) {
		super();
		this.packageName = packageName;
		this.className = className;
		this.sourceFileName = sourceFileName;
		this.fullyQualifiedClassName = packageName + "/" + className;
		this.classDesc = "L" + this.fullyQualifiedClassName + ';';
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws PLPException {
		String blockClass = currentClass;
		if (firstProcVisit) {
			for (ProcDec procDec : block.procedureDecs) {
				procDec.visit(this, null);
				currentClass = blockClass;
			}
			return null;
		}
		ClassWriter cWriter = (ClassWriter) arg;
		for (VarDec varDec : block.varDecs) {
			varDec.visit(this, cWriter);
		}
		for (ProcDec procDec : block.procedureDecs) {
			procDec.visit(this, null);
			currentClass = blockClass;
		}

		MethodVisitor methodVisitor = cWriter.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
		methodVisitor.visitCode();
		block.statement.visit(this, methodVisitor);
		methodVisitor.visitInsn(RETURN);
		methodVisitor.visitMaxs(0, 0);
		methodVisitor.visitEnd();
		return null;

	}

	@Override
	public Object visitProgram(Program program, Object arg) throws PLPException {
		currentClass = fullyQualifiedClassName;
		program.block.visit(this, null);
		firstProcVisit = false;
		MethodVisitor methodVisitor;
		//create a classWriter and visit it
		classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		//Hint:  if you get failures in the visitMaxs, try creating a ClassWriter with 0
		// instead of ClassWriter.COMPUTE_FRAMES.  The result will not be a valid classfile,
		// but you will be able to print it so you can see the instructions.  After fixing,
		// restore ClassWriter.COMPUTE_FRAMES
		classWriter.visit(V16, ACC_PUBLIC | ACC_SUPER, fullyQualifiedClassName, null, "java/lang/Object", new String[]{"java/lang/Runnable"});
		methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		methodVisitor.visitCode();
		methodVisitor.visitVarInsn(ALOAD, 0);
		methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		methodVisitor.visitInsn(RETURN);
		methodVisitor.visitMaxs(-1, -1);
		methodVisitor.visitEnd();
		//get a method visitor for the main method.
		//visit the block, passing it the methodVisitor

		//finish up the class
		methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		methodVisitor.visitCode();
		methodVisitor.visitTypeInsn(NEW, fullyQualifiedClassName);
		methodVisitor.visitInsn(DUP);
		methodVisitor.visitMethodInsn(INVOKESPECIAL, fullyQualifiedClassName, "<init>", "()V", false);
		methodVisitor.visitMethodInsn(INVOKEVIRTUAL, fullyQualifiedClassName, "run", "()V", false);
		methodVisitor.visitInsn(RETURN);
		methodVisitor.visitMaxs(0, 0);
		methodVisitor.visitEnd();
		currentClass = fullyQualifiedClassName;
		program.block.visit(this, classWriter);
		classWriter.visitEnd();

		GenClass mainClass = new GenClass(fullyQualifiedClassName, classWriter.toByteArray());

		output.add(mainClass);
		Collections.reverse(output);
		//return the bytes making up the classfile
		return output;
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
		statementAssign.expression.visit(this, arg);
		statementAssign.ident.visit(this, arg);
		return null;
	}

	@Override
	public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
		ClassWriter cWriter = (ClassWriter) arg;
		Type etype = varDec.getType();
		if (etype != null) {
			String JVMType = (etype.equals(Type.NUMBER) ? "I" : (etype.equals(Type.BOOLEAN) ? "Z" : "Ljava/lang/String;"));
			FieldVisitor fieldVisitor = cWriter.visitField(0, varDec.ident.getStringValue(), JVMType, null, null);
			fieldVisitor.visitEnd();
		}
		return null;
	}

	@Override
	public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
		String statementCallClass = procDecList.get(statementCall.ident.getDec());
		if (currentClass.equals(statementCallClass)) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, statementCallClass, "run", "()V", false);
		} else {
			String outerClassName = currentClass;
			int currentNestLevel = statementCall.ident.getNest() - 1;
			mv.visitTypeInsn(NEW, statementCallClass);
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 0);
			while (!(outerClassName.equals(statementCallClass.substring(0, statementCallClass.lastIndexOf("$"))))) {
				mv.visitFieldInsn(GETFIELD, outerClassName, "this$" + currentNestLevel, CodeGenUtils.toJVMClassDesc(outerClassName.substring(0, outerClassName.lastIndexOf("$"))));
				outerClassName = outerClassName.substring(0, outerClassName.lastIndexOf("$"));
				currentNestLevel--;
			}
			String descriptor = CodeGenUtils.toJVMClassDesc(outerClassName);
			mv.visitMethodInsn(INVOKESPECIAL, statementCallClass, "<init>", "(" + descriptor + ")V", false);
			mv.visitMethodInsn(INVOKEVIRTUAL, statementCallClass, "run", "()V", false);
		}
		return null;
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		statementOutput.expression.visit(this, arg);
		Type etype = statementOutput.expression.getType();
		String JVMType = (etype.equals(Type.NUMBER) ? "I" : (etype.equals(Type.BOOLEAN) ? "Z" : "Ljava/lang/String;"));
		String printlnSig = "(" + JVMType + ")V";
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", printlnSig, false);
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
		MethodVisitor mv = (MethodVisitor) arg;
		statementIf.expression.visit(this, arg);
		Label label0 = new Label();
		mv.visitJumpInsn(IFEQ, label0);
		statementIf.statement.visit(this, arg);
		mv.visitLabel(label0);
		return null;
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
		Label label1 = new Label();
		mv.visitLabel(label1);
		statementWhile.expression.visit(this, arg);
		Label label0 = new Label();
		mv.visitJumpInsn(IFEQ, label0);
		statementWhile.statement.visit(this, arg);
		mv.visitJumpInsn(GOTO, label1);
		mv.visitLabel(label0);
		return null;
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
		Type argType = expressionBinary.e0.getType();
		Kind op = expressionBinary.op.getKind();
		switch (argType) {
			case NUMBER -> {
				expressionBinary.e0.visit(this, arg);
				expressionBinary.e1.visit(this, arg);
				switch (op) {
					case PLUS -> mv.visitInsn(IADD);
					case MINUS -> mv.visitInsn(ISUB);
					case TIMES -> mv.visitInsn(IMUL);
					case DIV -> mv.visitInsn(IDIV);
					case MOD -> mv.visitInsn(IREM);
					case EQ -> {
						Label label0 = new Label();
						mv.visitJumpInsn(IF_ICMPNE, label0);
						mv.visitInsn(ICONST_1);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label1);
					}
					case NEQ -> {
						Label label0 = new Label();
						mv.visitJumpInsn(IF_ICMPEQ, label0);
						mv.visitInsn(ICONST_1);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label1);
					}
					case LT -> {
						Label label0 = new Label();
						mv.visitJumpInsn(IF_ICMPGE, label0);
						mv.visitInsn(ICONST_1);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label1);
					}
					case LE -> {
						Label label0 = new Label();
						mv.visitJumpInsn(IF_ICMPGT, label0);
						mv.visitInsn(ICONST_1);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label1);
					}
					case GT -> {
						Label label0 = new Label();
						mv.visitJumpInsn(IF_ICMPLE, label0);
						mv.visitInsn(ICONST_1);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label1);
					}
					case GE -> {
						Label label0 = new Label();
						mv.visitJumpInsn(IF_ICMPLT, label0);
						mv.visitInsn(ICONST_1);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label1);
					}
					default -> {
						throw new IllegalStateException("code gen bug in visitExpressionBinary NUMBER");
					}
				}
				;
			}
			case BOOLEAN -> {
				expressionBinary.e0.visit(this, arg);
				expressionBinary.e1.visit(this, arg);
				switch (op) {
					case PLUS -> mv.visitInsn(IOR);
					case TIMES -> mv.visitInsn(IAND);
					case EQ -> {
						Label label0 = new Label();
						mv.visitJumpInsn(IF_ICMPNE, label0);
						mv.visitInsn(ICONST_1);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label1);
					}
					case NEQ -> {
						Label label0 = new Label();
						mv.visitJumpInsn(IF_ICMPEQ, label0);
						mv.visitInsn(ICONST_1);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label1);
					}
					case LT -> {
						Label label0 = new Label();
						mv.visitJumpInsn(IF_ICMPGE, label0);
						mv.visitInsn(ICONST_1);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label1);
					}
					case LE -> {
						Label label0 = new Label();
						mv.visitJumpInsn(IF_ICMPGT, label0);
						mv.visitInsn(ICONST_1);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label1);
					}
					case GT -> {
						Label label0 = new Label();
						mv.visitJumpInsn(IF_ICMPLE, label0);
						mv.visitInsn(ICONST_1);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label1);
					}
					case GE -> {
						Label label0 = new Label();
						mv.visitJumpInsn(IF_ICMPLT, label0);
						mv.visitInsn(ICONST_1);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label1);
					}
					default -> {
						throw new IllegalStateException("code gen bug in visitExpressionBinary NUMBER");
					}
				}
				;
			}
			case STRING -> {
				expressionBinary.e0.visit(this, arg);
				expressionBinary.e1.visit(this, arg);
				switch (op) {
					case PLUS -> {
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
					}
					case EQ -> {
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
					}
					case NEQ -> {
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
						Label label0 = new Label();
						mv.visitJumpInsn(IFEQ, label0);
						mv.visitInsn(ICONST_0);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_1);
						mv.visitLabel(label1);
					}
					case LT -> {
						mv.visitInsn(SWAP);
						mv.visitInsn(DUP2);
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
						Label label0 = new Label();
						mv.visitJumpInsn(IFNE, label0);
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
						Label label1 = new Label();
						mv.visitJumpInsn(IFEQ, label1);
						mv.visitInsn(ICONST_1);
						Label label2 = new Label();
						mv.visitJumpInsn(GOTO, label2);
						mv.visitLabel(label1);
						mv.visitInsn(ICONST_0);
						mv.visitJumpInsn(GOTO, label2);
						mv.visitLabel(label0);
						mv.visitInsn(POP2);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label2);
					}
					case LE -> {
						mv.visitInsn(SWAP);
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
						Label label0 = new Label();
						mv.visitJumpInsn(IFEQ, label0);
						mv.visitInsn(ICONST_1);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label1);
					}
					case GT -> {
						mv.visitInsn(DUP2);
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
						Label label0 = new Label();
						mv.visitJumpInsn(IFNE, label0);
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false);
						Label label1 = new Label();
						mv.visitJumpInsn(IFEQ, label1);
						mv.visitInsn(ICONST_1);
						Label label2 = new Label();
						mv.visitJumpInsn(GOTO, label2);
						mv.visitLabel(label1);
						mv.visitInsn(ICONST_0);
						mv.visitJumpInsn(GOTO, label2);
						mv.visitLabel(label0);
						mv.visitInsn(POP2);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label2);
					}
					case GE -> {
						mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false);
						Label label0 = new Label();
						mv.visitJumpInsn(IFEQ, label0);
						mv.visitInsn(ICONST_1);
						Label label1 = new Label();
						mv.visitJumpInsn(GOTO, label1);
						mv.visitLabel(label0);
						mv.visitInsn(ICONST_0);
						mv.visitLabel(label1);
					}
					default -> {
						throw new IllegalStateException("code gen bug in visitExpressionBinary NUMBER");
					}
				}
				;
			}
			default -> {
				throw new IllegalStateException("code gen bug in visitExpressionBinary");
			}
		}
		return null;
	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
		if (expressionIdent.getDec().firstToken.getStringValue().equals("CONST")) {
			mv.visitLdcInsn(((ConstDec) expressionIdent.getDec()).val);
		} else if (expressionIdent.getDec().firstToken.getStringValue().equals("VAR")) {
			Type etype = expressionIdent.getDec().getType();
			String JVMType = (etype.equals(Type.NUMBER) ? "I" : (etype.equals(Type.BOOLEAN) ? "Z" : "Ljava/lang/String;"));

			mv.visitVarInsn(ALOAD, 0);
			String expressionIdentClass = fullyQualifiedClassName;
			if (!currentClass.equals(fullyQualifiedClassName)) {
				int loopNumber = expressionIdent.getNest() - expressionIdent.getDec().getNest();
				int currentNestLevel = expressionIdent.getNest() - 1;
				expressionIdentClass = currentClass;
				while (loopNumber > 0) {
					mv.visitFieldInsn(GETFIELD, expressionIdentClass, "this$" + currentNestLevel, CodeGenUtils.toJVMClassDesc(expressionIdentClass.substring(0, expressionIdentClass.lastIndexOf("$"))));
					expressionIdentClass = expressionIdentClass.substring(0, expressionIdentClass.lastIndexOf("$"));
					currentNestLevel--;
					loopNumber--;
				}
			}
			mv.visitFieldInsn(GETFIELD, expressionIdentClass, ((VarDec) expressionIdent.getDec()).ident.getStringValue(), JVMType);
		}
		return null;
	}

	@Override
	public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
		mv.visitLdcInsn(expressionNumLit.getFirstToken().getIntValue());
		return null;
	}

	@Override
	public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
		mv.visitLdcInsn(expressionStringLit.getFirstToken().getStringValue());
		return null;
	}

	@Override
	public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
		mv.visitLdcInsn(expressionBooleanLit.getFirstToken().getBooleanValue());
		return null;
	}

	@Override
	public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
		String currentProcName = currentClass + "$" + procDec.ident.getStringValue();
		if (firstProcVisit) {
			procDecList.put(procDec, currentProcName);
			currentClass = currentProcName;
			procDec.block.visit(this, null);
			return null;
		}
		ClassWriter pWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		String classDesc1 = CodeGenUtils.toJVMClassDesc(currentClass);
		pWriter.visit(V16, ACC_SUPER, currentProcName, null, "java/lang/Object", new String[]{"java/lang/Runnable"});

		FieldVisitor fieldVisitor = pWriter.visitField(ACC_FINAL | ACC_SYNTHETIC, "this$" + procDec.getNest(), classDesc1, null, null);
		fieldVisitor.visitEnd();
		MethodVisitor methodVisitor = pWriter.visitMethod(0, "<init>", "(" + classDesc1 + ")V", null, null);
		//methodVisitor.visitCode();
		methodVisitor.visitVarInsn(ALOAD, 0);
		methodVisitor.visitVarInsn(ALOAD, 1);
		methodVisitor.visitFieldInsn(PUTFIELD, currentProcName, "this$" + procDec.getNest(), classDesc1);
		methodVisitor.visitVarInsn(ALOAD, 0);
		methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		methodVisitor.visitInsn(RETURN);
		methodVisitor.visitMaxs(-1, -1);
		methodVisitor.visitEnd();
		currentClass = currentProcName;
		procDec.block.visit(this, pWriter);
		currentClass = currentProcName;
		pWriter.visitEnd();

		GenClass procGenClass = new GenClass(currentProcName, pWriter.toByteArray());
		output.add(procGenClass);
		return null;
	}

	@Override
	public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
		return null;
	}

	@Override
	public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
		return null;
	}

	@Override
	public Object visitIdent(Ident ident, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
		mv.visitVarInsn(ALOAD, 0);
		String IdentClass = fullyQualifiedClassName;
		if (!currentClass.equals(fullyQualifiedClassName)) {
			int loopNumber = ident.getNest() - ident.getDec().getNest();
			int currentNestLevel = ident.getNest() - 1;
			IdentClass = currentClass;
			while (loopNumber > 0) {
				mv.visitFieldInsn(GETFIELD, IdentClass, "this$" + currentNestLevel, CodeGenUtils.toJVMClassDesc(IdentClass.substring(0, IdentClass.lastIndexOf("$"))));
				IdentClass = IdentClass.substring(0, IdentClass.lastIndexOf("$"));
				currentNestLevel--;
				loopNumber--;
			}
		}
		mv.visitInsn(SWAP);
		Type etype = ident.getDec().getType();
		String JVMType = (etype.equals(Type.NUMBER) ? "I" : (etype.equals(Type.BOOLEAN) ? "Z" : "Ljava/lang/String;"));
		mv.visitFieldInsn(PUTFIELD, IdentClass, ident.firstToken.getStringValue(), JVMType);
		return null;
	}
}
