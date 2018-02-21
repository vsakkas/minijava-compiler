import java.util.Arrays;

public class ClangHelper {
    static String[] clangArgs;

    public ClangHelper(String commandAndArgs) {
        String executableName = GenerateExecutableName(commandAndArgs);
        String codeName = GenerateIRCodeName(commandAndArgs);
        commandAndArgs = commandAndArgs.substring(0, commandAndArgs.lastIndexOf("."));
        clangArgs = new String[]{"clang-4.0", "-o", executableName, codeName};
    }

    public void GenerateExecutable() {
        try {
            Process clangProcess = new ProcessBuilder(clangArgs).start();
            clangProcess.waitFor();
        }
        catch (Exception ex) {
            System.err.println(ex);
        }
    }

    private String GenerateExecutableName(String commandAndArgs) {
        String executableName = commandAndArgs.substring(0, commandAndArgs.lastIndexOf("."));
        return executableName;
    }

    private String GenerateIRCodeName(String commandAndArgs) {
        String codeName = commandAndArgs.substring(0, commandAndArgs.lastIndexOf(".")) + ".ll";
        return codeName;
    }
}