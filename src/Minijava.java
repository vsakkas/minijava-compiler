import syntaxtree.*;
import visitor.*;
import java.io.*;

class Minijava {
    public static SymbolTable symbolTable;
    public static OutputWriter outputWriter;

    public static void main (String [] args) {
        if (args.length == 0) {
            System.err.println("Usage: java Main <inputFiles>");
            System.exit(1);
        }
        for (int i = 0; i < args.length; i++) {
            symbolTable = new SymbolTable();
            FileInputStream inputFile = null;
            try {
                inputFile = new FileInputStream(args[i]);
                MiniJavaParser parser = new MiniJavaParser(inputFile);
                SymbolTableBuilderVisitor symbolTableBuilder = new SymbolTableBuilderVisitor();
                Goal root = parser.Goal();
                try {
                    root.accept(symbolTableBuilder, null);
                    TypeCheckerVisitor typeChecker = new TypeCheckerVisitor();
                    root.accept(typeChecker, null);
                    outputWriter = new OutputWriter(args[i]);
                    outputWriter.EmitVTableInformation();
                    outputWriter.EmitHelperFunctions();
                    CodeGeneratorVisitor codeGeneratorVisitor = new CodeGeneratorVisitor();
                    try {
                        root.accept(codeGeneratorVisitor, null);
                        ClangHelper clangHelper = new ClangHelper(args[i]);
                        clangHelper.GenerateExecutable();
                    }
                    catch (Exception ex) {
                        System.err.println(ex.getMessage());
                    }
                    finally {
                        outputWriter.Close();
                    }
                }
                catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            }
            catch (ParseException ex) {
                System.out.println(ex.getMessage());
            }
            catch (FileNotFoundException ex) {

                System.err.println(ex.getMessage());
            }
            finally {
                try {
                    if (inputFile != null) inputFile.close();
                }
                catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
