import java.util.LinkedHashMap;
import java.util.Map;

public class FunctionTypeInfo {
    String name, returnType;
    Map<String, VariableTypeInfo> argumentsType;
    Map<String, VariableTypeInfo> localVariablesType;

    public FunctionTypeInfo(String functionName, String returnName) {
        name = functionName;
        returnType = returnName;
        argumentsType = new LinkedHashMap<String, VariableTypeInfo>();
        localVariablesType = new LinkedHashMap<String, VariableTypeInfo>();
    }

    public void insertArgument(String argumentName, String argumentType) {
        VariableTypeInfo argumentToAddTypeInfo = new VariableTypeInfo(argumentName, argumentType);
        argumentsType.put(argumentName, argumentToAddTypeInfo);
    }

    public void insertLocalVariable(String localVariableName, String localVariableType) {
        VariableTypeInfo localVariableToAddTypeInfo = new VariableTypeInfo(localVariableName, localVariableType);
        localVariablesType.put(localVariableName, localVariableToAddTypeInfo);
    }

    public VariableTypeInfo lookupArgument(String argumentName) {
        return argumentsType.get(argumentName);
    }

    public VariableTypeInfo lookupVariable(String localVariableName) {
        return localVariablesType.get(localVariableName);
    }
}