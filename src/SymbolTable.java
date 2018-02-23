import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;

public class SymbolTable {
    static Map<String, ClassTypeInfo> symbolTable;

    public SymbolTable() {
        symbolTable = new LinkedHashMap<String, ClassTypeInfo>();
    }

    public static void insertClass(String className, String extendsClassName) throws Exception {
        ClassTypeInfo classToAdd = new ClassTypeInfo(className, extendsClassName);
        if (extendsClassName != null) {
            ClassTypeInfo classToExtend = lookupClass(extendsClassName);
            if (classToExtend == null) {
                throw new Exception("Error: cannot find symbol " + extendsClassName);
            }
        }
        if (lookupClass(className) != null) {
            throw new Exception("Error: redeclaration of class " + className);
        }
        symbolTable.put(className, classToAdd);
    }

    public static void insertFunction(String className, String functionName, String returnName) throws Exception {
        ClassTypeInfo classToAdd = lookupClass(className);
        if (classToAdd != null) {
            if (classToAdd.lookupFunction(functionName) != null) {
                throw new Exception("Error: redeclaration of function " + functionName + " inside class " + className);
            }
            classToAdd.insertFunction(functionName, returnName);
        }
        else {
            throw new Exception("Error: cannot find symbol " + className);
        }
        if (returnName != "void") {
            if (classToAdd.extendsName != null) {
                ClassTypeInfo extendedClass = lookupClass(classToAdd.extendsName);
                classToAdd.functionOffset = Math.max(extendedClass.functionOffset, classToAdd.functionOffset);
                if (extendedClass.lookupFunction(functionName) == null) {
                    classToAdd.functionsOffsetTable.add(new OffsetInfo(className, functionName, classToAdd.functionOffset));
                    classToAdd.functionOffset = classToAdd.functionOffset + 8;
                }
            }
            else {
                classToAdd.functionsOffsetTable.add(new OffsetInfo(className, functionName, classToAdd.functionOffset));
                classToAdd.functionOffset += 8;
            }
        }
    }

    public static void insertVariable(String className, String variableName, String variableType) throws Exception {
        ClassTypeInfo classToAdd = lookupClass(className);
        if (classToAdd != null) {
            if (classToAdd.lookupVariable(variableName) != null) {
                throw new Exception("Error: redeclaration of variable " + variableName + " inside class " + className);
            }
            classToAdd.insertVariable(variableName, variableType);
        }
        else {
            throw new Exception("Error: cannot find symbol " + className);
        }
        if (classToAdd.extendsName != null) {
            ClassTypeInfo extendedClass = lookupClass(classToAdd.extendsName);
            classToAdd.variableOffset = Math.max(extendedClass.variableOffset, classToAdd.variableOffset);
        }
        classToAdd.variablesOffsetTable.add(new OffsetInfo(className, variableName, classToAdd.variableOffset));
        if (variableType == "boolean") {
            classToAdd.variableOffset += 1;
        }
        else if (variableType == "int") {
            classToAdd.variableOffset += 4;
        }
        else {
            classToAdd.variableOffset += 8;
        }
    }

    public static void insertArgumentInFunction(String className, String functionName, String argumentName, String argumentType) throws Exception {
        ClassTypeInfo classToAdd = lookupClass(className);
        if (classToAdd != null) {
            FunctionTypeInfo functionToAdd = classToAdd.lookupFunction(functionName);
            if (functionToAdd != null) {
                if (functionToAdd.lookupArgument(argumentName) != null) {
                    throw new Exception("Error: redeclaration of argument " + argumentName + " in function " + functionName + " inside class " + className);
                }
                functionToAdd.insertArgument(argumentName, argumentType);
            }
            else {
                throw new Exception("Error: cannot find symbol " + functionName);
            }
        }
        else {
            throw new Exception("Error: cannot find symbol " + className);
        }
    }

    public static void insertLocalVariableInFunction(String className, String functionName, String variableName, String variableType) throws Exception {
        ClassTypeInfo classToAdd = lookupClass(className);
        if (classToAdd != null) {
            FunctionTypeInfo functionToAdd = classToAdd.lookupFunction(functionName);
            if (functionToAdd != null) {
                if (functionToAdd.lookupVariable(variableName) != null || functionToAdd.lookupArgument(variableName) != null) {
                    throw new Exception("Error: redeclaration of local variable " + variableName + " in function " + functionName + " inside class " + className);
                }
                functionToAdd.insertLocalVariable(variableName, variableType);
            }
            else {
                throw new Exception("Error: cannot find symbol " + functionName);
            }
        }
        else {
            throw new Exception("Error: cannot find symbol " + className);
        }
    }

    public static ClassTypeInfo lookupClass(String className) {
        return symbolTable.get(className);
    }

    public static FunctionTypeInfo lookupFunction(String className, String functionName) throws Exception {
        ClassTypeInfo classToLookup = lookupClass(className);
        if (classToLookup != null) {
            FunctionTypeInfo functionToLookup = classToLookup.lookupFunction(functionName);
            if (functionToLookup != null) {
                return functionToLookup;
            }
            else if (classToLookup.extendsName != null) {
                ClassTypeInfo extendsClassToLookup = lookupClass(classToLookup.extendsName);
                if (extendsClassToLookup != null) {
                    return lookupFunction(extendsClassToLookup.name, functionName);
                }
                else {
                    throw new Exception("Error: undeclared function " + functionName);
                }
            }
            else {
                throw new Exception("Error: undeclared function " + functionName);
            }
        }
        else {
            throw new Exception("Error: undeclared function " + functionName);
        }
    }

    public static VariableTypeInfo lookupVariableInClass(String className, String variableName) throws Exception {
        ClassTypeInfo classToLookup = lookupClass(className);
        if (classToLookup != null) {
            VariableTypeInfo variableToLookup = classToLookup.lookupVariable(variableName);
            if (variableToLookup != null) {
                return variableToLookup;
            }
            else if (classToLookup.extendsName != null) {
                ClassTypeInfo extendsClassToLookup = lookupClass(classToLookup.extendsName);
                if (extendsClassToLookup != null) {
                    return lookupVariableInClass(extendsClassToLookup.name, variableName);
                }
                else {
                    throw new Exception("Error: undeclared symbol " + variableName);
                }
            }
            else {
                throw new Exception("Error: undeclared symbol " + variableName);
            }
        }
        else {
            throw new Exception("Error: undeclared symbol " + variableName);
        }
    }

    public static VariableTypeInfo lookupVariableInFunction(String className, String functionName, String variableName) throws Exception {
        ClassTypeInfo classToLookup = lookupClass(className);
        if (classToLookup != null) {
            FunctionTypeInfo functionToLookup = lookupFunction(className, functionName);
            if (functionToLookup != null) {
                VariableTypeInfo variableToLookup = functionToLookup.lookupVariable(variableName);
                if (variableToLookup != null) {
                    return variableToLookup;
                }
                else {
                    variableToLookup = functionToLookup.lookupArgument(variableName);
                    if (variableToLookup != null) {
                        return variableToLookup;
                    }
                    else {
                        variableToLookup = lookupVariableInClass(className, variableName);
                        if (variableToLookup != null) {
                            return variableToLookup;
                        }
                        else {
                            throw new Exception("Error: undeclared symbol " + variableName);
                        }
                    }
                }
            }
            else {
                throw new Exception("Error: undeclared symbol " + variableName);
            }
        }
        else {
            throw new Exception("Error: undeclared symbol " + variableName);
        }
    }

    public static ArrayList<VariableTypeInfo> lookupFunctionArguments(String className, String functionName) throws Exception {
        FunctionTypeInfo functionToLookup = lookupFunction(className, functionName);
        if (functionToLookup != null) {
            ArrayList<VariableTypeInfo> argumentsList = new ArrayList<VariableTypeInfo>(functionToLookup.argumentsType.values());
            return argumentsList;
        }
        else {
            throw new Exception("Error: undeclared function " + functionName);
        }
    }

    public static ArrayList<String> lookupFunctionArgumentTypes(String className, String functionName) throws Exception {
        FunctionTypeInfo functionToLookup = lookupFunction(className, functionName);
        if (functionToLookup != null) {
            ArrayList<VariableTypeInfo> tempList = new ArrayList<VariableTypeInfo>(functionToLookup.argumentsType.values());
            ArrayList<String> argumentsList = new ArrayList<String>();
            for (int i = 0; i < functionToLookup.argumentsType.size(); i++) {
                argumentsList.add(getExtendedName(tempList.get(i).type));
            }
            return argumentsList;
        }
        else {
            throw new Exception("Error: undeclared function " + functionName);
        }
    }

    public static ArrayList<String> lookupFunctionArgumentNames(String className, String functionName) throws Exception {
        FunctionTypeInfo functionToLookup = lookupFunction(className, functionName);
        if (functionToLookup != null) {
            ArrayList<VariableTypeInfo> tempList = new ArrayList<VariableTypeInfo>(functionToLookup.argumentsType.values());
            ArrayList<String> argumentsList = new ArrayList<String>();
            for (int i = 0; i < functionToLookup.argumentsType.size(); i++) {
                argumentsList.add(getExtendedName(tempList.get(i).name));
            }
            return argumentsList;
        }
        else {
            throw new Exception("Error: undeclared function " + functionName);
        }
    }

    public static ArrayList<String> lookupFunctionVariableNames(String className, String functionName) throws Exception {
        FunctionTypeInfo functionToLookup = lookupFunction(className, functionName);
        if (functionToLookup != null) {
            ArrayList<VariableTypeInfo> tempList = new ArrayList<VariableTypeInfo>(functionToLookup.localVariablesType.values());
            ArrayList<String> localVariablesList = new ArrayList<String>();
            for (int i = 0; i < functionToLookup.argumentsType.size(); i++) {
                localVariablesList.add(getExtendedName(tempList.get(i).name));
            }
            return localVariablesList;
        }
        else {
            throw new Exception("Error: undeclared function " + functionName);
        }
    }

    public static String lookupFunctionReturnType(String className, String functionName) throws Exception {
        FunctionTypeInfo functionToLookup = lookupFunction(className, functionName);
        if (functionToLookup != null) {
            return getExtendedName(functionToLookup.returnType);
        }
        else {
            return null;
        }
    }

    public static String getExtendedName(String className) {
        ClassTypeInfo classToLookup = lookupClass(className);
        if (classToLookup != null) {
            if (classToLookup.extendsName != null) {
                return getExtendedName(classToLookup.extendsName);
            }
            else {
                return classToLookup.name;
            }
        }
        else {
            return className;
        }
    }

    public static void printOffsets() {
        ArrayList<ClassTypeInfo> classesList = new ArrayList<ClassTypeInfo>(symbolTable.values());
        for (int i = 0; i < classesList.size(); i++) {
            System.out.println("-----------Class " + classesList.get(i).name + "-------------");
            System.out.println("--Variables---");
            for (int j = 0; j < classesList.get(i).variablesOffsetTable.size(); j++){
               System.out.println(classesList.get(i).variablesOffsetTable.get(j).className + "." +
                                  classesList.get(i).variablesOffsetTable.get(j).name + " : " +
                                  classesList.get(i).variablesOffsetTable.get(j).offset);
            }
            System.out.println("---Methods---");
            for (int j = 0; j < classesList.get(i).functionsOffsetTable.size(); j++){
                System.out.println(classesList.get(i).functionsOffsetTable.get(j).className + "." +
                                   classesList.get(i).functionsOffsetTable.get(j).name + " : " +
                                   classesList.get(i).functionsOffsetTable.get(j).offset);
             }
             System.out.println("");
        }
    }

    public static int getBlockSize(String className) {
        ClassTypeInfo classInfo = lookupClass(className);
        int blockSize = 0;
        if (classInfo.variablesOffsetTable.size() > 0) {
            blockSize += classInfo.variablesOffsetTable.get(classInfo.variablesOffsetTable.size() - 1).offset;
            String lastVariableName = classInfo.variablesOffsetTable.get(classInfo.variablesOffsetTable.size() - 1).name;
            VariableTypeInfo lastVariableInfo = null;
            try {
                lastVariableInfo = lookupVariableInClass(className, lastVariableName);
            }
            catch(Exception ex) {
                System.err.println(ex.getMessage());
            }
            if (lastVariableInfo.type.equals("boolean")) {
                blockSize += 1;
            }
            else if (lastVariableInfo.type.equals("int")) {
                blockSize += 4;
            }
            else {
                blockSize += 8;
            }
        }
        return blockSize + 8;
    }

    public static String lookupFunctionsClassName(String className, String functionName) throws Exception {
        ClassTypeInfo classToLookup = lookupClass(className);
        if (classToLookup != null) {
            FunctionTypeInfo functionToLookup = classToLookup.lookupFunction(functionName);
            if (functionToLookup != null) {
                return classToLookup.name;
            }
            else if (classToLookup.extendsName != null) {
                ClassTypeInfo extendsClassToLookup = lookupClass(classToLookup.extendsName);
                if (extendsClassToLookup != null) {
                    return lookupFunctionsClassName(extendsClassToLookup.name, functionName);
                }
                else {
                    throw new Exception("Error: undeclared function " + functionName);
                }
            }
            else {
                throw new Exception("Error: undeclared function " + functionName);
            }
        }
        else {
            throw new Exception("Error: undeclared function " + functionName);
        }
    }
}