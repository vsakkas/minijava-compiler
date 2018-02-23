import syntaxtree.*;
import visitor.GJDepthFirst;

public class SymbolTableBuilderVisitor extends GJDepthFirst<String, String[]>
{
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
        SymbolTable.insertClass(className, null);

        SymbolTable.insertFunction(className, "main", "void");

        String functionArgumentName = n.f11.accept(this, new String[]{className});
        SymbolTable.insertArgumentInFunction(className, "main", functionArgumentName, "String[]");

        n.f14.accept(this, new String[]{className, "main"});
        n.f15.accept(this, new String[]{className, "main"});
        return null;
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
        SymbolTable.insertClass(className, null);

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
        String _ret=null;
        String className = n.f1.accept(this, argu);
        String extendsName = n.f3.accept(this, argu);
        SymbolTable.insertClass(className, extendsName);

        n.f5.accept(this, new String[]{className});
        n.f6.accept(this, new String[]{className});
        return _ret;
    }

    /**
     * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n, String[] argu) throws Exception {
        String variableType = n.f0.accept(this, argu);
        String variableName = n.f1.accept(this, argu);
        if (argu.length == 1)
            SymbolTable.insertVariable(argu[0], variableName, variableType);
        else
            SymbolTable.insertLocalVariableInFunction(argu[0], argu[1], variableName, variableType);
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
        SymbolTable.insertFunction(argu[0], functionName, returnType);

        n.f4.accept(this, new String[]{argu[0], functionName});    // if present
        n.f7.accept(this, new String[]{argu[0], functionName});
        n.f8.accept(this, new String[]{argu[0], functionName});
        n.f10.accept(this, new String[]{argu[0], functionName});
        return null;
    }

    /**
     * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(FormalParameter n, String[] argu) throws Exception {
        String argumentType = n.f0.accept(this, argu);
        String argumentName = n.f1.accept(this, argu);
        SymbolTable.insertArgumentInFunction(argu[0], argu[1], argumentName, argumentType);
        return null;
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
     * f0 -> ( ExpressionTerm() )*
    */
    public String visit(ExpressionTail n, String[] argu) throws Exception {
        return n.f0.accept(this, argu);
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
        return n.f0.toString();
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
     * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(ArrayAllocationExpression n, String[] argu) throws Exception {
        n.f3.accept(this, argu);
        return "int[]";
    }
}