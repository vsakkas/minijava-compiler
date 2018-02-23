import java.io.*;
import java.util.ArrayList;

public class OutputWriter {
    static PrintStream writer;

    public OutputWriter(String javaPath) throws IOException {
        try {
            File f = new File(javaPath);
            writer = new PrintStream(f.getCanonicalPath().substring(0, f.getCanonicalPath().lastIndexOf(".")) + ".ll");
        }
        catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void EmitVTableInformation() {
        ArrayList<ClassTypeInfo> classesList = new ArrayList<ClassTypeInfo>(SymbolTable.symbolTable.values());
        writer.print("@." + classesList.get(0).name + "_vtable = global ");
        writer.println("[0 x i8*] []");
        for (int i = 1; i < classesList.size(); i++) {
            ArrayList<FunctionTypeInfo> functionsList = new ArrayList<>(classesList.get(i).functionsType.values());
            int functionsNumber =  GetFunctionsNumber(0, classesList.get(i));
            writer.print("@." + classesList.get(i).name + "_vtable = global ");
            writer.print("[" + functionsNumber + " x i8*] [");
            EmitFunctionInformation(classesList.get(i), classesList.get(i).name, functionsNumber, functionsNumber);
            writer.println("]");
        }
        writer.print("\n");
    }

    public static int GetFunctionsNumber(int functionsNumber, ClassTypeInfo classInfo) {
        functionsNumber += classInfo.functionsOffsetTable.size();
        if (classInfo.extendsName != null) {
            functionsNumber += GetFunctionsNumber(0, SymbolTable.lookupClass(classInfo.extendsName));
        }
        return functionsNumber;
    }

    public static void EmitFunctionInformation(ClassTypeInfo classInfo, String className, int currentFunctionNumber, int functionsNumber) {
        ArrayList<FunctionTypeInfo> functionsList = new ArrayList<>(classInfo.functionsType.values());
        if (classInfo.extendsName != null) {
            EmitFunctionInformation(SymbolTable.lookupClass(classInfo.extendsName), className, currentFunctionNumber - classInfo.functionsOffsetTable.size(), functionsNumber);
        }
        for (int i = 0; i < classInfo.functionsOffsetTable.size(); i++){
            writer.print("i8* bitcast (");
            writer.print(GetIType(functionsList.get(i).returnType) + " ");
            writer.print("(i8*");
            ArrayList<VariableTypeInfo> argumentsList = new ArrayList<>(functionsList.get(i).argumentsType.values());
            for (int j = 0; j < argumentsList.size(); j++) {
                writer.print("," + GetIType(argumentsList.get(j).type));
            }
            writer.print(")* ");
            String name = classInfo.name;
            ClassTypeInfo classNameInfo = SymbolTable.lookupClass(className);
            try {
                name = SymbolTable.lookupFunctionsClassName(className, classInfo.functionsOffsetTable.get(i).name);
            }
            catch (Exception ex) {
                ;
            }
            writer.print("@" + name + "." + classInfo.functionsOffsetTable.get(i).name + " to i8*)");
            if (functionsNumber > currentFunctionNumber || i < functionsList.size() - 1) {
                writer.print(", ");
            }
        }
    }

    public void EmitHelperFunctions() {
        writer.println("declare i8* @calloc(i32, i32)\n" +
                        "declare i32 @printf(i8*, ...)\n" +
                        "declare void @exit(i32)\n" +
                        "\n" +
                        "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n" +
                        "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n" +
                        "define void @print_int(i32 %i) {\n" +
                        "\t%_str = bitcast [4 x i8]* @_cint to i8*\n" +
                        "\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
                        "\tret void\n" +
                        "}\n" +
                        "\n" +
                        "define void @throw_oob() {\n" +
                        "\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
                        "\tcall i32 (i8*, ...) @printf(i8* %_str)\n" +
                        "\tcall void @exit(i32 1)\n" +
                        "\tret void\n" +
                        "}\n");
    }

    public static String GetIType(String type) {
        if (type.equals("boolean")) {
            return "i1";
        }
        else if (type.equals("int")) {
            return "i32";
        }
        else if (type.equals("int[]")) {
            return "i32*";
        }
        else {
            return "i8*";
        }
    }

    public static void Emit(String lineToPrint) {
        writer.print(lineToPrint);
    }

    public void Close() {
        writer.close();
    }
}