import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import syntaxtree.*;
import visitor.GJDepthFirst;

public class CodeGeneratorVisitor extends GJDepthFirst<String[], String[]>
{
    int registerCounter = 0, outOfBoundsCounter = 0, arrayAllocationCounter = 0,
        ifCounter = 0, loopCounter = 0, andClauseCounter = 0;
    public Deque<ArrayList<String[]>> argumentRegisters = new ArrayDeque<>();

    /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    public String[] visit(Goal n, String[] argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
    public String[] visit(MainClass n, String[] argu) throws Exception {
        OutputWriter.Emit("define i32 @main() {\n");
        String className = n.f1.accept(this, argu)[0];
        n.f14.accept(this, new String[]{className, "main"});
        n.f15.accept(this, new String[]{className, "main"});
        OutputWriter.Emit("\tret i32 0\n");
        OutputWriter.Emit("}\n\n");
        return null;
    }

    /**
    * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
    public String[] visit(TypeDeclaration n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    public String[] visit(ClassDeclaration n, String[] argu) throws Exception {
        String className = n.f1.accept(this, argu)[0];
        n.f3.accept(this, new String[]{className});
        n.f4.accept(this, new String[]{className});
        return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
    public String[] visit(ClassExtendsDeclaration n, String[] argu) throws Exception {
        String className = n.f1.accept(this, argu)[0];
        n.f5.accept(this, new String[]{className});
        n.f6.accept(this, new String[]{className});
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String[] visit(VarDeclaration n, String[] argu) throws Exception {
        String type = n.f0.accept(this, argu)[0];
        String identifier = n.f1.accept(this, argu)[0];
        if (argu.length > 1) {
            OutputWriter.Emit("\t%" + identifier + " = alloca " + OutputWriter.GetIType(type) + "\n");
        }
        return null;
    }

    /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
    public String[] visit(MethodDeclaration n, String[] argu) throws Exception {
        registerCounter = 0;
        outOfBoundsCounter = 0;
        arrayAllocationCounter = 0;
        ifCounter = 0;
        loopCounter = 0;
        andClauseCounter = 0;
        n.f1.accept(this, argu);
        String functionName = n.f2.accept(this, argu)[0];
        FunctionTypeInfo functionInfo = SymbolTable.lookupFunction(argu[0], functionName);
        OutputWriter.Emit("define " + OutputWriter.GetIType(functionInfo.returnType) + " @" + argu[0] + "." + functionName + "(i8* %this");
        ArrayList<VariableTypeInfo> argumentRegistersList = new ArrayList<VariableTypeInfo>(functionInfo.argumentsType.values());
        for (int i = 0; i < argumentRegistersList.size(); i++) {
            OutputWriter.Emit(", " + OutputWriter.GetIType(argumentRegistersList.get(i).type) + " %." + argumentRegistersList.get(i).name);
        }
        OutputWriter.Emit(") {\n");
        for (int i = 0; i < argumentRegistersList.size(); i++) {
            String name = argumentRegistersList.get(i).name, type = OutputWriter.GetIType(argumentRegistersList.get(i).type);
            OutputWriter.Emit("\t%" + name + " = alloca " + type + "\n");
            OutputWriter.Emit("\tstore " + type + " %." + name + ", " + type + "* %" + name + "\n");
        }
        n.f4.accept(this, new String[]{argu[0], functionName});
        n.f7.accept(this, new String[]{argu[0], functionName});
        n.f8.accept(this, new String[]{argu[0], functionName});
        String[] expression = n.f10.accept(this, new String[]{argu[0], functionName});
        String[] expressionInfo = loadAndGetRegister(expression[0], new String[]{argu[0], functionName}, true);
        String expressionType = expressionInfo[1];
        if ((expression.length > 1 && expression[1].equals("MessageSend")) || expression[0].equals("int")) {
            expressionType = OutputWriter.GetIType(expression[0]);
        }
        OutputWriter.Emit("\n\tret " + expressionType + " " + expressionInfo[0] + "\n");
        OutputWriter.Emit("}\n\n");
        return null;
    }

    /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
    public String[] visit(FormalParameterList n, String[] argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String[] visit(FormalParameter n, String[] argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
    }

    /**
    * f0 -> ( FormalParameterTerm() )*
    */
    public String[] visit(FormalParameterTail n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> ","
    * f1 -> FormalParameter()
    */
    public String[] visit(FormalParameterTerm n, String[] argu) throws Exception {
        n.f1.accept(this, argu);
        return null;
    }

    /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    public String[] visit(Type n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public String[] visit(ArrayType n, String[] argu) throws Exception {
        return new String[]{"int[]"};
    }

    /**
    * f0 -> "boolean"
    */
    public String[] visit(BooleanType n, String[] argu) throws Exception {
        return new String[]{"boolean"};
    }

    /**
    * f0 -> "int"
    */
    public String[] visit(IntegerType n, String[] argu) throws Exception {
        return new String[]{"int"};
    }

    /**
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */
    public String[] visit(Statement n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
    public String[] visit(Block n, String[] argu) throws Exception {
        n.f1.accept(this, argu);
        return null;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String[] visit(AssignmentStatement n, String[] argu) throws Exception {
        String identifier = n.f0.accept(this, argu)[0];
        int tempRegisterCounter = registerCounter;
        String[] expression = n.f2.accept(this, argu);
        String[] expressionInfo = loadAndGetRegister(expression[0], argu, true);
        String[] identifierInfo = loadAndGetRegister(identifier, argu, false);
        if (expression.length == 2 && expression[1].equals("new")) {
            OutputWriter.Emit("\tstore " + identifierInfo[1] + " %_" + tempRegisterCounter + ", " + identifierInfo[1] + "* " + identifierInfo[0] + "\n");
        }
        else {
            OutputWriter.Emit("\tstore " + identifierInfo[1] + " " + expressionInfo[0] + ", " + identifierInfo[1] + "* " + identifierInfo[0] + "\n");
        }

        return null;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    public String[] visit(ArrayAssignmentStatement n, String[] argu) throws Exception {
        String identifier = n.f0.accept(this, argu)[0];
        String expression1 = n.f2.accept(this, argu)[0];
        String[] expressionInfo1 = loadAndGetRegister(expression1, argu, true);
        String[] identifierInfo = loadAndGetRegister(identifier, argu, true);
        OutputWriter.Emit("\t" + getNewRegister() + " = load i32, i32* %_" + (registerCounter - 2) + "\n");
        int tempRegisterCounter = (registerCounter - 2);
        OutputWriter.Emit("\t" + getNewRegister() + " = icmp ult i32 " + expressionInfo1[0] + ", %_" + (registerCounter - 2) + "\n");
        String oobLabel1 = getNewOutOfBoundsLabel(), oobLabel2 = getNewOutOfBoundsLabel(), oobLabel3 = getNewOutOfBoundsLabel();
        OutputWriter.Emit("\tbr i1 %_" + (registerCounter - 1) + ", label %" + oobLabel1 + ", label %" + oobLabel2 + "\n\n");
        OutputWriter.Emit(oobLabel1 + ":\n");
        OutputWriter.Emit("\t" + getNewRegister() + " = add i32 " + expressionInfo1[0] + ", 1\n");
        OutputWriter.Emit("\t" + getNewRegister() + " = getelementptr i32, i32* %_" + tempRegisterCounter + ", i32 %_" + (registerCounter - 2) + "\n");
        tempRegisterCounter = (registerCounter - 1);
        String expression2 = n.f5.accept(this, argu)[0];
        String[] expressionInfo2 = loadAndGetRegister(expression2, argu, true);
        OutputWriter.Emit("\tstore i32 " + expressionInfo2[0] + ", i32* %_" + tempRegisterCounter + "\n");
        OutputWriter.Emit("\tbr label %" + oobLabel3 + "\n\n");
        OutputWriter.Emit(oobLabel2 + ":\n");
        OutputWriter.Emit("\tcall void @throw_oob()\n");
        OutputWriter.Emit("\tbr label %" + oobLabel3 + "\n\n");
        OutputWriter.Emit(oobLabel3 + ":\n");

        return null;
    }

    /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    public String[] visit(IfStatement n, String[] argu) throws Exception {
        String expression = n.f2.accept(this, argu)[0];
        String[] expressionInfo = loadAndGetRegister(expression, argu, true);
        String ifLabel1 = getNewIfLabel(), ifLabel2 = getNewIfLabel(), ifLabel3 = getNewIfLabel();
        OutputWriter.Emit("\tbr i1 " + expressionInfo[0] + ", label %" + ifLabel1 + ", label %" + ifLabel2 + "\n");
        OutputWriter.Emit("\n" + ifLabel1 + ":\n");
        n.f4.accept(this, argu);
        OutputWriter.Emit("\n\tbr label %" + ifLabel3 + "\n");
        OutputWriter.Emit("\n" + ifLabel2 + ":\n");
        n.f6.accept(this, argu);
        OutputWriter.Emit("\n\tbr label %" + ifLabel3 + "\n");
        OutputWriter.Emit("\n" + ifLabel3 + ":\n");
        return null;
    }

    /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public String[] visit(WhileStatement n, String[] argu) throws Exception {

        String loopLabel1 = getNewLoopLabel(), loopLabel2 = getNewLoopLabel(), loopLabel3 = getNewLoopLabel();
        OutputWriter.Emit("\tbr label %" + loopLabel1 + "\n");
        OutputWriter.Emit("\n" + loopLabel1 + ":\n");
        String expression = n.f2.accept(this, argu)[0];
        String[] expressionInfo = loadAndGetRegister(expression, argu, true);
        OutputWriter.Emit("\tbr i1 " + expressionInfo[0] + ", label %" + loopLabel2 + ", label %" + loopLabel3 + "\n");
        OutputWriter.Emit("\n" + loopLabel2 + ":\n");
        n.f4.accept(this, argu);
        OutputWriter.Emit("\n\tbr label %" + loopLabel1 + "\n");
        OutputWriter.Emit("\n" + loopLabel3 + ":\n");

        return null;
    }

    /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String[] visit(PrintStatement n, String[] argu) throws Exception {
        String expression = n.f2.accept(this, argu)[0];
        String[] expressionInfo = loadAndGetRegister(expression, argu, true);
        OutputWriter.Emit("\tcall void (i32) @print_int(i32 " + expressionInfo[0] + ")\n");
        return null;
    }

    /**
    * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | Clause()
    */
    public String[] visit(Expression n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    public String[] visit(AndExpression n, String[] argu) throws Exception {
        String andClauseLabel1 = getNewAndClauseLabel(), andClauseLabel2 = getNewAndClauseLabel(),
               andClauseLabel3 = getNewAndClauseLabel(), andClauseLabel4 = getNewAndClauseLabel();
        String expression1 = n.f0.accept(this, argu)[0];
        String[] expressionInfo1 = loadAndGetRegister(expression1, argu, true);
        OutputWriter.Emit("\tbr label %" + andClauseLabel1 + "\n");
        OutputWriter.Emit("\n" + andClauseLabel1 + ":\n");
        OutputWriter.Emit("\t br i1 " + expressionInfo1[0] + ", label %" + andClauseLabel2 + ", label %" + andClauseLabel4 + "\n");
        OutputWriter.Emit("\n" + andClauseLabel2 + ":\n");
        String expression2 = n.f2.accept(this, argu)[0];
        String[] expressionInfo2 = loadAndGetRegister(expression2, argu, true);
        OutputWriter.Emit("\tbr label %" + andClauseLabel3 + "\n");
        OutputWriter.Emit("\n" + andClauseLabel3 + ":\n");
        OutputWriter.Emit("\tbr label %" + andClauseLabel4 + "\n");
        OutputWriter.Emit("\n" + andClauseLabel4 + ":\n");
        OutputWriter.Emit("\t" + getNewRegister() +  " = phi i1 [ 0, %" + andClauseLabel1 + " ], [ " + expressionInfo2[0] + ", %" + andClauseLabel3 + " ]\n");

        return new String[]{"boolean"};
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String[] visit(CompareExpression n, String[] argu) throws Exception {
        String primaryExpression1 = n.f0.accept(this, argu)[0];
        String[] primaryExpressionInfo1 = loadAndGetRegister(primaryExpression1, argu, true);
        String primaryExpression2 = n.f2.accept(this, argu)[0];
        String[] primaryExpressionInfo2 = loadAndGetRegister(primaryExpression2, argu, true);
        OutputWriter.Emit("\t" + getNewRegister() + " = icmp slt " + primaryExpressionInfo1[1] + " " + primaryExpressionInfo1[0] + ", " + primaryExpressionInfo2[0] + "\n");
        return new String[]{"boolean"};
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String[] visit(PlusExpression n, String[] argu) throws Exception {
        String expression1 = n.f0.accept(this, argu)[0];
        String[] expressionInfo1 = loadAndGetRegister(expression1, argu, true);
        String expression2 = n.f2.accept(this, argu)[0];
        String[] expressionInfo2 = loadAndGetRegister(expression2, argu, true);
        OutputWriter.Emit("\t" + getNewRegister() + " = add i32 " + expressionInfo1[0] + ", " + expressionInfo2[0] + "\n");
        return new String[]{"int"};
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String[] visit(MinusExpression n, String[] argu) throws Exception {
        String expression1 = n.f0.accept(this, argu)[0];
        String[] expressionInfo1 = loadAndGetRegister(expression1, argu, true);
        String expression2 = n.f2.accept(this, argu)[0];
        String[] expressionInfo2 = loadAndGetRegister(expression2, argu, true);
        OutputWriter.Emit("\t" + getNewRegister() + " = sub i32 " + expressionInfo1[0] + ", " + expressionInfo2[0] + "\n");
        return new String[]{"int"};
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String[] visit(TimesExpression n, String[] argu) throws Exception {
        String expression1 = n.f0.accept(this, argu)[0];
        String[] expressionInfo1 = loadAndGetRegister(expression1, argu, true);
        String expression2 = n.f2.accept(this, argu)[0];
        String[] expressionInfo2 = loadAndGetRegister(expression2, argu, true);
        OutputWriter.Emit("\t" + getNewRegister() + " = mul i32 " + expressionInfo1[0] + ", " + expressionInfo2[0] + "\n");
        return new String[]{"int"};
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String[] visit(ArrayLookup n, String[] argu) throws Exception {
        String primaryExpression1 = n.f0.accept(this, argu)[0];
        String primaryExpression2 = n.f2.accept(this, argu)[0];
        String[] primaryExpressionInfo1 = loadAndGetRegister(primaryExpression1, argu, true);
        int tempRegisterCounter = registerCounter;
        OutputWriter.Emit("\t" + getNewRegister() + " = load i32, i32* %_" + (registerCounter - 2) + "\n");
        String[] primaryExpressionInfo2 = loadAndGetRegister(primaryExpression2, argu, true);
        OutputWriter.Emit("\t" + getNewRegister() + " = icmp ult i32 " + primaryExpressionInfo2[0] + ", %_" + tempRegisterCounter + "\n");
        String oobLabel1 = getNewOutOfBoundsLabel(), oobLabel2 = getNewOutOfBoundsLabel(), oobLabel3 = getNewOutOfBoundsLabel();
        OutputWriter.Emit("\tbr i1 %_" + (registerCounter - 1) + ", label %" + oobLabel1 + ", label %" + oobLabel2 + "\n\n");
        OutputWriter.Emit(oobLabel1 + ":\n");
        OutputWriter.Emit("\t" + getNewRegister() + " = add i32 " + primaryExpressionInfo2[0] + ", 1\n");
        OutputWriter.Emit("\t" + getNewRegister() + " = getelementptr i32, i32* " + primaryExpressionInfo1[0] + ", i32 %_" + (registerCounter - 2) + "\n");
        OutputWriter.Emit("\t" + getNewRegister() + " = load i32, i32* %_" + (registerCounter - 2) + "\n");
        OutputWriter.Emit("\tbr label %" + oobLabel3 + "\n\n");
        OutputWriter.Emit(oobLabel2 + ":\n");
        OutputWriter.Emit("\tcall void @throw_oob()\n");
        OutputWriter.Emit("\tbr label %" + oobLabel3 + "\n\n");
        OutputWriter.Emit(oobLabel3 + ":\n");

        return new String[]{"int", "ArrayAccess"};
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String[] visit(ArrayLength n, String[] argu) throws Exception {
        String primaryExpression = n.f0.accept(this, argu)[0];
        String[] primaryExpressionInfo = loadAndGetRegister(primaryExpression, argu, true);
        OutputWriter.Emit("\t" + getNewRegister() + " = load i32, i32* %_" + (registerCounter - 2) + "\n");

        return new String[]{"int"};
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public String[] visit(MessageSend n, String[] argu) throws Exception {
        int primaryExpressionRegister = registerCounter;
        String[] primaryExpression = n.f0.accept(this, argu);
        String[] primaryExpressionInfo = loadAndGetRegister(primaryExpression[0], argu, true);
        String identifier = n.f2.accept(this, argu)[0];
        argumentRegisters.add(new ArrayList<>());
        OutputWriter.Emit("\t" + getNewRegister() + " = bitcast i8* ");
        if (primaryExpression.length > 1 && primaryExpression[1].equals("new")) {
            OutputWriter.Emit("%_" + primaryExpressionRegister);
        }
        else {
            OutputWriter.Emit(primaryExpressionInfo[0]);
        }
        OutputWriter.Emit(" to i8***\n");
        OutputWriter.Emit("\t" + getNewRegister() + " = load i8**, i8*** %_" + (registerCounter - 2) + "\n");
        int functionOffset = getFunctionOffset(identifier, getTypeNoExtend(primaryExpression[0], argu));
        OutputWriter.Emit("\t" + getNewRegister() + " = getelementptr i8*, i8** %_" + (registerCounter - 2) + ", i32 " + (functionOffset / 8) + "\n");
        OutputWriter.Emit("\t" + getNewRegister() + " = load i8*, i8** %_" + (registerCounter - 2) + "\n");
        ArrayList<String> argumentsList = SymbolTable.lookupFunctionArgumentTypes(getTypeNoExtend(primaryExpression[0], argu), identifier);
        String returnType = SymbolTable.lookupFunctionReturnType(getTypeNoExtend(primaryExpression[0], argu), identifier);
        int callRegister = registerCounter;
        OutputWriter.Emit("\t" + getNewRegister() + " = bitcast i8* %_" + (registerCounter - 2) + " to " + OutputWriter.GetIType(returnType) + " (i8*");
        for (int i = 0; i < argumentsList.size(); i++) {
            OutputWriter.Emit(", " + OutputWriter.GetIType(getTypeNoExtend(argumentsList.get(i), argu)));
        }
        OutputWriter.Emit(")*\n");
        n.f4.accept(this, argu);
        ArrayList<String[]> argumentRegistersList = argumentRegisters.pop();
        ArrayList<String[]> argumentInfo = new ArrayList<>();
        for (int i = 0; i < argumentRegistersList.size(); i++) {
            if (argumentRegistersList.get(i).length > 1 && argumentRegistersList.get(i)[1].equals("new")) {
                argumentInfo.add(new String[]{"%_" + argumentRegistersList.get(i)[0]});
            }
            else {
                argumentInfo.add(loadAndGetRegister(argumentRegistersList.get(i)[0], argu, true));
            }
        }
        OutputWriter.Emit("\t" + getNewRegister() + " = call " + OutputWriter.GetIType(returnType) + " %_" + callRegister + "(i8* ");
        if (primaryExpression.length > 1 && primaryExpression[1].equals("new")) {
            OutputWriter.Emit("%_" + primaryExpressionRegister);
        }
        else {
            OutputWriter.Emit(primaryExpressionInfo[0]);
        }
        for (int i = 0; i < argumentRegistersList.size(); i++) {
            OutputWriter.Emit(", " + OutputWriter.GetIType(argumentsList.get(i)) + " " + argumentInfo.get(i)[0]);
        }
        OutputWriter.Emit(")\n\n");

        return new String[]{returnType, "MessageSend"};
    }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String[] visit(ExpressionList n, String[] argu) throws Exception {
        int tempRegisterCounter = registerCounter;
        String[] expression = n.f0.accept(this, argu);
        if (expression.length > 1 && expression[1].equals("new")) {
            expression[0] = "" + tempRegisterCounter;
        }
        argumentRegisters.getFirst().add(expression);
        n.f1.accept(this, argu);
        return expression;
    }

    /**
    * f0 -> ( ExpressionTerm() )*
    */
    public String[] visit(ExpressionTail n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    public String[] visit(ExpressionTerm n, String[] argu) throws Exception {
        int tempRegisterCounter = registerCounter;
        String[] expression = n.f1.accept(this, argu);
        if (expression.length > 1 && expression[1].equals("new")) {
            expression[0] = "" + tempRegisterCounter;
        }
        argumentRegisters.getFirst().add(expression);
        return expression;
    }

    /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
    public String[] visit(Clause n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
    public String[] visit(PrimaryExpression n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> <INTEGER_LITERAL>
    */
    public String[] visit(IntegerLiteral n, String[] argu) throws Exception {
        return new String[]{n.f0.toString()};
    }

    /**
    * f0 -> "true"
    */
    public String[] visit(TrueLiteral n, String[] argu) throws Exception {
        return new String[]{"true"};
    }

    /**
    * f0 -> "false"
    */
    public String[] visit(FalseLiteral n, String[] argu) throws Exception {
        return  new String[]{"false"};
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    public String[] visit(Identifier n, String[] argu) throws Exception {
        return new String[]{n.f0.toString()};
    }

    /**
    * f0 -> "this"
    */
    public String[] visit(ThisExpression n, String[] argu) throws Exception {
        return new String[]{"this", argu[0]};
    }

    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String[] visit(ArrayAllocationExpression n, String[] argu) throws Exception {
        String expression = n.f3.accept(this, argu)[0];
        String[] expressionInfo = loadAndGetRegister(expression, argu, true);
        ClassTypeInfo classInfo = SymbolTable.lookupClass(argu[0]);

        OutputWriter.Emit("\t" + getNewRegister() + " = icmp slt i32 " + expressionInfo[0] + ", 0\n");
        String arrayAllocLabel1 = getNewArrayAllocationLabel(), arrayAllocLabel2 = getNewArrayAllocationLabel();
        OutputWriter.Emit("\tbr i1 %_" + (registerCounter - 1) + ",  label %" + arrayAllocLabel1 + ", label %" + arrayAllocLabel2 + "\n\n");
        OutputWriter.Emit(arrayAllocLabel1 + ":\n");
        OutputWriter.Emit("\tcall void @throw_oob()\n");
        OutputWriter.Emit("\tbr label %" + arrayAllocLabel2 + "\n\n");
        OutputWriter.Emit(arrayAllocLabel2 + ":\n");
        OutputWriter.Emit("\t" + getNewRegister() + " = add i32 " + expressionInfo[0] + ", 1\n");
        int functionsNumber = OutputWriter.GetFunctionsNumber(0, classInfo);
        OutputWriter.Emit("\t" + getNewRegister() + " = call i8* @calloc(i32 " + functionsNumber + ", i32 %_" + (registerCounter - 2) + ")\n");
        OutputWriter.Emit("\t" + getNewRegister() + " = bitcast i8* %_" + (registerCounter - 2) + " to i32*\n");
        OutputWriter.Emit("\tstore i32 " + expressionInfo[0] + ", i32* %_" + (registerCounter - 1) + "\n");

        return new String[]{"int[]", "new", "new[]"};
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String[] visit(AllocationExpression n, String[] argu) throws Exception {
        String identifier = n.f1.accept(this, argu)[0];
        OutputWriter.Emit("\t" + getNewRegister() + " = call i8* @calloc(i32 1, i32 " + SymbolTable.getBlockSize(identifier));
        OutputWriter.Emit(")\n");
        OutputWriter.Emit("\t" + getNewRegister() + " = bitcast i8* %_" + (registerCounter - 2) + " to i8***\n");
        ClassTypeInfo classInfo = SymbolTable.lookupClass(identifier);
        int functionsNumber = OutputWriter.GetFunctionsNumber(0, classInfo);
        OutputWriter.Emit("\t" + getNewRegister() + " = getelementptr [" + functionsNumber + " x i8*], [" + functionsNumber + " x i8*]* @." + identifier + "_vtable, i32 0, i32 0\n");
        OutputWriter.Emit("\tstore i8** %_" + (registerCounter - 1) + ", i8*** %_" + (registerCounter - 2) + "\n");
        return new String[]{identifier, "new"};
    }

    /**
    * f0 -> "!"
    * f1 -> Clause()
    */
    public String[] visit(NotExpression n, String[] argu) throws Exception {
        String clause = n.f1.accept(this, argu)[0];
        String[] clauseInfo = loadAndGetRegister(clause, argu, true);
        OutputWriter.Emit("\t" + getNewRegister() + " = xor i1 1, " + clauseInfo[0] + "\n");
        return new String[]{"boolean"};
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public String[] visit(BracketExpression n, String[] argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    private String getNewRegister() {
        String newRegister = "%_" + registerCounter;
        registerCounter++;
        return newRegister;
    }

    private String getNewOutOfBoundsLabel() {
        String newLabel = "oob" + outOfBoundsCounter;
        outOfBoundsCounter++;
        return newLabel;
    }

    private String getNewArrayAllocationLabel() {
        String newLabel = "array_alloc" + arrayAllocationCounter;
        arrayAllocationCounter++;
        return newLabel;
    }

    private String getNewIfLabel() {
        String newLabel = "if" + ifCounter;
        ifCounter++;
        return newLabel;
    }

    private String getNewLoopLabel() {
        String newLabel = "loop" + loopCounter;
        loopCounter++;
        return newLabel;
    }

    private String getNewAndClauseLabel() {
        String newLabel = "andClause" + andClauseCounter;
        andClauseCounter++;
        return newLabel;
    }

    private boolean isNumeric(String s) {
        return s != null && s.matches("-?\\d+");
    }

    public String getType(String identifier, String[] argu) throws Exception {
        if (identifier.equals("int") || identifier.equals("int[]") || identifier.equals("boolean")) {
            return identifier;
        }
        if (identifier.equals("true") || identifier.equals("false")) {
            return "boolean";
        }
        if (identifier.equals("this")) {
            return argu[0];
        }
        ClassTypeInfo classTypeInfo = SymbolTable.lookupClass(identifier);
        if (classTypeInfo != null) {
            return SymbolTable.getExtendedName(classTypeInfo.name);
        }
        VariableTypeInfo identifierTypeInfo;
        if (argu.length == 1) {
            identifierTypeInfo = SymbolTable.lookupVariableInClass(argu[0], identifier);
        }
        else {
            identifierTypeInfo = SymbolTable.lookupVariableInFunction(argu[0], argu[1], identifier);
        }
        if (identifierTypeInfo != null) {
            return SymbolTable.getExtendedName(identifierTypeInfo.type);
        }
        else {
            return null;
        }
    }

    public String getTypeNoExtend(String identifier, String[] argu) throws Exception {
        if (identifier.equals("int") || identifier.equals("int[]") || identifier.equals("boolean")) {
            return identifier;
        }
        if (identifier.equals("true") || identifier.equals("false")) {
            return "boolean";
        }
        if (isNumeric(identifier)) {
            return "int";
        }
        if (identifier.equals("this")) {
            return argu[0];
        }
        ClassTypeInfo classTypeInfo = SymbolTable.lookupClass(identifier);
        if (classTypeInfo != null) {
            return classTypeInfo.name;
        }
        VariableTypeInfo identifierTypeInfo;
        if (argu.length == 1) {
            identifierTypeInfo = SymbolTable.lookupVariableInClass(argu[0], identifier);
        }
        else {
            identifierTypeInfo = SymbolTable.lookupVariableInFunction(argu[0], argu[1], identifier);
        }
        if (identifierTypeInfo != null) {
            return identifierTypeInfo.type;
        }
        else {
            return null;
        }
    }

    private boolean isFunctionArgument(String name, String className, String functionName) {
        ArrayList<String> argumentRegistersList = null;
        try {
            argumentRegistersList = SymbolTable.lookupFunctionArgumentNames(className, functionName);
        }
        catch (Exception ex) {
            return false;
        }
        if (argumentRegistersList.contains(name)) {
            return true;
        }
        return false;
    }

    private boolean isFunctionVariable(String name, String className, String functionName) {
        VariableTypeInfo variableInfo = null;
        try {
            variableInfo = SymbolTable.lookupVariableInFunction(className, functionName, name);
        }
        catch (Exception ex) {
            return false;
        }
        if (variableInfo != null) {
            return true;
        }
        return false;
    }

    private boolean isClassVariable(String name, String className) {
        VariableTypeInfo variableInfo = null;
        try {
            variableInfo = SymbolTable.lookupVariableInClass(className, name);
        }
        catch (Exception ex) {
            return false;
        }
        if (variableInfo != null) {
            return true;
        }
        return false;
    }

    private int getClassVariableOffset(String name, String className) {
        ClassTypeInfo classInfo = SymbolTable.lookupClass(className);
        int offset = 0;
        for (int i = 0; i < classInfo.variablesOffsetTable.size(); i++) {
            if (classInfo.variablesOffsetTable.get(i).name.equals(name)) {
                offset = classInfo.variablesOffsetTable.get(i).offset + 8;
            }
        }
        if (classInfo.variablesOffsetTable.size() == 0 && classInfo.extendsName != null) {
            offset = getClassVariableOffset(name, classInfo.extendsName);
        }
        return offset;
    }

    private int getFunctionOffset(String name, String className) {
        ClassTypeInfo classInfo = SymbolTable.lookupClass(className);
        int offset = 0;
        boolean found = false;
        for (int i = 0; i < classInfo.functionsOffsetTable.size(); i++) {
            if (classInfo.functionsOffsetTable.get(i).name.equals(name)) {
                offset = classInfo.functionsOffsetTable.get(i).offset;
                found = true;
            }
        }
        if (!found && classInfo.extendsName != null) {
            offset = getFunctionOffset(name, classInfo.extendsName);
        }
        return offset;
    }

    private String[] loadAndGetRegister(String name, String[] argu, boolean loadFlag) throws Exception {
        String[] registerInfo = new String[]{null, null};
        registerInfo[0] = "%_" + (registerCounter - 1);
        if (name.equals("true")) {
            registerInfo[0] = "1";
            registerInfo[1] = "i1";
        }
        else if (name.equals("false")) {
            registerInfo[0] = "0";
            registerInfo[1] = "i1";
        }
        else if (isNumeric(name)) {
            registerInfo[0] = name;
            registerInfo[1] = "i32";
        }
        else if (name.equals("this")) {
            registerInfo[0] = "%this";
            registerInfo[1] = "i8*";
        }
        else {
            String type = OutputWriter.GetIType(getType(name, argu));
            if (isClassVariable(name, argu[0])) {
                OutputWriter.Emit("\t" + getNewRegister() + " = getelementptr i8, i8* %this, i32 " + getClassVariableOffset(name, argu[0]) + "\n");
                OutputWriter.Emit("\t" + getNewRegister() + " = bitcast i8* %_" + (registerCounter - 2) + " to " + type + "*\n");
                if (loadFlag) OutputWriter.Emit("\t" + getNewRegister() + " = load " + type + ", " + type + "* %_" + (registerCounter - 2) + "\n");
                registerInfo[0] = "%_" + (registerCounter - 1);
                registerInfo[1] = type;
            }
            else if (loadFlag && (isFunctionArgument(name, argu[0], argu[1]) || isFunctionVariable(name, argu[0], argu[1]))) {
                OutputWriter.Emit("\t" + getNewRegister() + " = load " + type + ", " + type + "* %" + name + "\n");
                registerInfo[0] = "%_" + (registerCounter - 1);
                registerInfo[1] = type;
            }
            else if (!loadFlag){
                registerInfo[0] = "%" + name;
                registerInfo[1] = type;
            }
        }
        return registerInfo;
    }
}