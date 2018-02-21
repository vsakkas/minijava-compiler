import java.util.List;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import syntaxtree.*;
import visitor.GJDepthFirst;

public class TypeCheckerVisitor extends GJDepthFirst<String, String[]>
{
    public Deque<ArrayList<String>> expressionList = new ArrayDeque<>();

    /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    public String visit(Goal n, String[] argu) throws Exception {
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
    public String visit(MainClass n, String[] argu) throws Exception {
        String className = n.f1.accept(this, argu);
        String functionArgumentName = n.f11.accept(this, new String[]{className});
        n.f14.accept(this, new String[]{className, "main"});
        n.f15.accept(this, new String[]{className, "main"});
        return null;
    }

    /**
     * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
    public String visit(TypeDeclaration n, String[] argu) throws Exception {
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
    public String visit(ClassDeclaration n, String[] argu) throws Exception {
        String className = n.f1.accept(this, argu);
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
    public String visit(ClassExtendsDeclaration n, String[] argu) throws Exception {
        String className = n.f1.accept(this, argu);
        String extendsName = n.f3.accept(this, argu);
        n.f5.accept(this, new String[]{className});
        n.f6.accept(this, new String[]{className});
        return null;
    }

    /**
     * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n, String[] argu) throws Exception {
        String variableType = n.f0.accept(this, argu);
        String variableName = n.f1.accept(this, argu);
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
    public String visit(MethodDeclaration n, String[] argu) throws Exception {
        String returnType = n.f1.accept(this, argu);
        String functionName = n.f2.accept(this, argu);
        String extendedReturnType = SymbolTable.lookupFunctionReturnType(argu[0], functionName);
        if (extendedReturnType != null) {
            if (SymbolTable.getExtendedName(returnType).equals(extendedReturnType) == false) {
                throw new Exception("Error: incompatible return type in function shadowing");
            }
        }
        if (n.f4.present()) {
            ArrayList<VariableTypeInfo> formalParameterList = SymbolTable.lookupFunctionArguments(argu[0], functionName);
            String extendedName = SymbolTable.getExtendedName(argu[0]);
            if (extendedName.equals(argu[0]) == false) {
                ClassTypeInfo extendedClass = SymbolTable.lookupClass(extendedName);
                if (extendedClass.functionsType.get(functionName) != null) {
                    ArrayList<VariableTypeInfo> extendedParameterList = SymbolTable.lookupFunctionArguments(extendedName, functionName);
                    if (formalParameterList.size() == extendedParameterList.size()) {
                        for (int i = 0; i < extendedParameterList.size(); i++) {
                            if (formalParameterList.get(i).name.equals(extendedParameterList.get(i).name) == false ||
                                formalParameterList.get(i).type.equals(extendedParameterList.get(i).type) == false) {
                                    throw new Exception("Error: invalid type of parameter in function shadowing");
                            }
                        }
                    }
                    else {
                        throw new Exception("Error: invalid number of parameters in function shadowing");
                    }
                }
            }
        }
        n.f7.accept(this, new String[]{argu[0], functionName});
        n.f8.accept(this, new String[]{argu[0], functionName});
        String returnExpression = n.f10.accept(this, new String[]{argu[0], functionName});
        String returnExpressionType = getTypeNoExtend(returnExpression, new String[]{argu[0], functionName});
        if (isSubclass(returnExpressionType, returnType) == false) {
            throw new Exception("Error: invalid type in return expression");
        }
        return null;
    }

    /**
     * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
    public String visit(FormalParameterList n, String[] argu) throws Exception {
        String formalParameter = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return formalParameter;
    }

    /**
     * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(FormalParameter n, String[] argu) throws Exception {
        String argumentType = n.f0.accept(this, argu);
        String argumentName = n.f1.accept(this, argu);
        return argumentType;
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
    */
    public String visit(FormalParameterTail n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> ","
    * f1 -> FormalParameter()
    */
    public String visit(FormalParameterTerm n, String[] argu) throws Exception {
        String formalParameter = n.f1.accept(this, argu);
        return formalParameter;
    }

    /**
     * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    public String visit(Type n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(ArrayType n, String[] argu) throws Exception {
        return "int[]";
    }

    /**
     * f0 -> "boolean"
    */
    public String visit(BooleanType n, String[] argu) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "int"
    */
    public String visit(IntegerType n, String[] argu) throws Exception {
        return "int";
    }

    /**
     * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */
    public String visit(Statement n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
    public String visit(Block n, String[] argu) throws Exception {
        n.f1.accept(this, argu);
        return null;
    }

    /**
     * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n, String[] argu) throws Exception {
        String identifierType = getTypeNoExtend(n.f0.accept(this, argu), argu);
        String expressionType = getTypeNoExtend(n.f2.accept(this, argu), argu);
        if (isSubclass(expressionType, identifierType) == false) {
            throw new Exception("Error: incompatible types in assignment");
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
    public String visit(ArrayAssignmentStatement n, String[] argu) throws Exception {
        String identifier = n.f0.accept(this, argu);
        if (checkType(identifier, "int[]", argu) == false) {
            throw new Exception("Error: " + identifier + " is not an array");
        }
        if (checkType(n.f2.accept(this, argu), "int", argu) == false) {
            throw new Exception("Error: invalid type inside brackets");
        }
        if (checkType(n.f5.accept(this, argu), "int", argu) == false) {
            throw new Exception("Error: icompatible type in array assignment");
        }
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
    public String visit(IfStatement n, String[] argu) throws Exception {
        if (checkType(n.f2.accept(this, argu), "boolean", argu) == false) {
            throw new Exception("Error: invalid type in if expression");
        }
        n.f4.accept(this, argu);
        n.f6.accept(this, argu);
        return null;
    }

    /**
     * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public String visit(WhileStatement n, String[] argu) throws Exception {
        if (checkType(n.f2.accept(this, argu), "boolean", argu) == false) {
            throw new Exception("Error: invalid type in while expression");
        }
        n.f4.accept(this, argu);
        return null;
    }

    /**
     * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n, String[] argu) throws Exception {
        if (checkType(n.f2.accept(this, argu), "int", argu) == false) {
            throw new Exception("Error: invalid argument in System.out.println");
        }
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
    public String visit(Expression n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    public String visit(AndExpression n, String[] argu) throws Exception {
        if (checkType(n.f0.accept(this, argu), "boolean", argu) == false) {
            throw new Exception("Error: invalid type in logical 'and'");
        }
        if (checkType(n.f2.accept(this, argu), "boolean", argu) == false) {
            throw new Exception("Error: undeclared variable in logical 'and'");
        }
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String visit(CompareExpression n, String[] argu) throws Exception {
        if (checkType(n.f0.accept(this, argu), "int", argu) == false ){
            throw new Exception("Error: invalid type in comparison");
        }
        if (checkType(n.f2.accept(this, argu), "int", argu) == false) {
            throw new Exception("Error: invalid type in comparison");
        }
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n, String[] argu) throws Exception {
        if (checkType(n.f0.accept(this, argu), "int", argu) == false) {
            throw new Exception("Error: invalid type in addition");
        }
        if (checkType(n.f2.accept(this, argu), "int", argu) == false) {
            throw new Exception("Error: invalid type in addition");
        }
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n, String[] argu) throws Exception {
        if (checkType(n.f0.accept(this, argu), "int", argu) == false) {
            throw new Exception("Error: invalid type in subtraction");
        }
        if (checkType(n.f2.accept(this, argu), "int", argu) == false) {
            throw new Exception("Error: invalid type in subtraction");
        }
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n, String[] argu) throws Exception {
        if (checkType(n.f0.accept(this, argu), "int", argu) == false) {
            throw new Exception("Error: invalid type in multiplication");
        }
        if (checkType(n.f2.accept(this, argu), "int", argu) == false) {
            throw new Exception("Error: invalid type in multiplication");
        }
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n, String[] argu) throws Exception {
        if (checkType(n.f0.accept(this, argu), "int[]", argu) == false) {
            throw new Exception("Error: invalid type inside brackets");
        }
        if (checkType(n.f2.accept(this, argu), "int", argu) == false) {
            throw new Exception("Error: invalid type inside brackets");
        }
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n, String[] argu) throws Exception {
        String primaryExpression =  n.f0.accept(this, argu);
        if (checkType(primaryExpression, "int[]", argu) == false) {
            throw new Exception("Error: " + primaryExpression + " does not have length field");
        }
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public String visit(MessageSend n, String[] argu) throws Exception {
        String primaryExpression = n.f0.accept(this, argu);
        String identifier = n.f2.accept(this, argu);
        ClassTypeInfo primaryExpressionClassTypeInfo = SymbolTable.lookupClass(primaryExpression);
        if (primaryExpressionClassTypeInfo != null) {
           primaryExpression = primaryExpressionClassTypeInfo.name;
        }
        else {
            VariableTypeInfo primaryExpressionVariableTypeInfo = SymbolTable.lookupVariableInFunction(argu[0], argu[1], primaryExpression);
            if (primaryExpressionVariableTypeInfo != null) {
                if (SymbolTable.lookupClass(primaryExpressionVariableTypeInfo.type) != null) {
                    primaryExpression = primaryExpressionVariableTypeInfo.type;
                }
                else {
                    throw new Exception("Error: " + primaryExpression + " is not a class");
                }
            }
            else {
                throw new Exception("Error: undeclared class");
            }
        }
        FunctionTypeInfo identifierTypeInfo = SymbolTable.lookupFunction(primaryExpression, identifier);
        ArrayList<String> identifierArgumentList = SymbolTable.lookupFunctionArgumentTypes(primaryExpression, identifier);
        if (identifierTypeInfo != null) {
            if (n.f4.present()) {
                expressionList.add(new ArrayList<>());
                n.f4.accept(this, argu);
                ArrayList<String> tempExpressionList = expressionList.pop();
                if (identifierArgumentList.size() == tempExpressionList.size()) {
                    for (int i = 0; i < identifierArgumentList.size(); i++) {
                        if (identifierArgumentList.get(i) != tempExpressionList.get(i)) {
                            throw new Exception("Error: invalid type of parameter in function call");
                        }
                    }
                }
                else {
                    throw new Exception("Error: invalid number of parameters in function call");
                }
            }
            else if (identifierArgumentList.size() != 0) {
                throw new Exception("Error: invalid number of parameters in function call");
            }
            return identifierTypeInfo.returnType;
        }
        else {
            throw new Exception("Error: " + primaryExpression + " does not contain " + identifier + " function");
        }
    }

    /**
     * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String visit(ExpressionList n, String[] argu) throws Exception {
        String expression = n.f0.accept(this, argu);
        expressionList.getFirst().add(getType(expression, argu));
        n.f1.accept(this, argu);
        return expression;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
    */
    public String visit(ExpressionTail n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> ","
    * f1 -> Expression()
    */
    public String visit(ExpressionTerm n, String[] argu) throws Exception {
        String expression = n.f1.accept(this, argu);
        expressionList.getFirst().add(getType(expression, argu));
        return expression;
    }

    /**
     * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
    public String visit(Clause n, String[] argu) throws Exception {
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
    public String visit(PrimaryExpression n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n, String[] argu) throws Exception {
        return "int";
    }

    /**
     * f0 -> "true"
    */
    public String visit(TrueLiteral n, String[] argu) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "false"
    */
    public String visit(FalseLiteral n, String[] argu) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n, String[] argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "this"
    */
    public String visit(ThisExpression n, String[] argu) throws Exception {
        String thisExpression = getThis(argu[0]);
        if (thisExpression != null) {
            return thisExpression;
        }
        else {
            throw new Exception("Error: invalid use of keyword this");
        }
    }

    /**
     * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(ArrayAllocationExpression n, String[] argu) throws Exception {
        if (checkType(n.f3.accept(this, argu), "int", argu) == false) {
            throw new Exception("Error: invalid type expression inside brackets");
        }
        return "int[]";
    }

    /**
     * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n, String[] argu) throws Exception {
        String identifierType = n.f1.accept(this, argu);
        if (SymbolTable.lookupClass(identifierType) != null) {
            return identifierType;
        }
        else {
            throw new Exception("Error: invalid symbol in new expression");
        }
    }

    /**
     * f0 -> "!"
    * f1 -> Clause()
    */
    public String visit(NotExpression n, String[] argu) throws Exception {
        if (checkType(n.f1.accept(this, argu), "boolean", argu) == false) {
            throw new Exception("Error: invalid type in not clause");
        }
        return "boolean";
    }

    /**
     * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public String visit(BracketExpression n, String[] argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    public Boolean checkType(String identifier, String type, String[] argu) throws Exception {
        if (identifier.equals(type)) {
            return true;
        }
        VariableTypeInfo identifierTypeInfo;
        if (argu.length == 1) {
            identifierTypeInfo = SymbolTable.lookupVariableInClass(argu[0], identifier);
        }
        else {
            identifierTypeInfo = SymbolTable.lookupVariableInFunction(argu[0], argu[1], identifier);
        }
        if (identifierTypeInfo == null) {
            return true;
        }
        if (identifierTypeInfo.type.equals(type)) {
            return true;
        }
        return false;
    }

    public String getType(String identifier, String[] argu) throws Exception {
        if (identifier.equals("int") || identifier.equals("int[]") || identifier.equals("boolean")) {
            return identifier;
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
        return null;
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

    public String getThis(String identifier) throws Exception {
        ClassTypeInfo classTypeInfo = SymbolTable.lookupClass(identifier);
        if (classTypeInfo == null) {
            throw new Exception("Error: invalid use of 'this' keyword");
        }
        if (classTypeInfo.extendsName != null) {
            return getThis(classTypeInfo.extendsName);
        }
        return classTypeInfo.name;
    }

    private boolean isNumeric(String s) {
        return s != null && s.matches("-?\\d+");
    }

    public boolean isSubclass(String firstClass, String secondClass) {
        while (secondClass.equals(firstClass) == false) {
            ClassTypeInfo classTypeInfo = SymbolTable.lookupClass(firstClass);
            if (classTypeInfo != null && classTypeInfo.extendsName != null) {
                firstClass = classTypeInfo.extendsName;
            }
            else {
                return false;
            }
        }
        return true;
    }
}