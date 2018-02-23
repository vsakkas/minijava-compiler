import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;

public class ClassTypeInfo {
    String name, extendsName;
    Map<String, VariableTypeInfo> variablesType;
    Map<String, FunctionTypeInfo> functionsType;
    ArrayList<OffsetInfo> variablesOffsetTable;
    ArrayList<OffsetInfo> functionsOffsetTable;
    int functionOffset, variableOffset;

    public ClassTypeInfo(String className, String extendsClassName) {
        name = className;
        extendsName = extendsClassName;
        variablesType = new LinkedHashMap<String, VariableTypeInfo>();
        functionsType = new LinkedHashMap<String, FunctionTypeInfo>();
        variablesOffsetTable = new ArrayList<OffsetInfo>();
        functionsOffsetTable = new ArrayList<OffsetInfo>();
        functionOffset = 0;
        variableOffset = 0;
    }

    public void insertVariable(String variableName, String variableType) {
        VariableTypeInfo variableToAddTypeInfo = new VariableTypeInfo(variableName, variableType);
        variablesType.put(variableName, variableToAddTypeInfo);
    }

    public void insertFunction(String functionName, String returnName) {
        FunctionTypeInfo functionToAddTypeInfo = new FunctionTypeInfo(functionName, returnName);
        functionsType.put(functionName, functionToAddTypeInfo);
    }

    public VariableTypeInfo lookupVariable(String variableName) {
        return variablesType.get(variableName);
    }

    public FunctionTypeInfo lookupFunction(String functionName) {
        return functionsType.get(functionName);
    }
}
