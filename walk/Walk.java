package info.kgeorgiy.ja.urazov.walk;

import java.io.BufferedWriter;

public class Walk extends AbstractWalk {
    Walk(final String inputFileName, final String outputFileName) throws WalkException {
        super(inputFileName, outputFileName);
    }

    @Override
    protected void walkImpl(String fileName, BufferedWriter writer) throws WalkException {
        HashCounter.processHash(fileName, false, writer);
    }

    public static boolean incorrectInput(String[] args) {
        if (args == null  || args.length != 2) {
            System.err.println("Incorrect number of arguments. Usage: <inputFile> <outputFile>.");
            return true;
        }
        if (args[0] == null ||  args[1] == null) {
            System.err.println("Non-null file names are required");
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        if (incorrectInput(args)) {
            return;
        }
        try {
            new Walk(args[0], args[1]).walk();
        } catch (WalkException e) {
            System.err.println(e.getMessage());
        }
    }
}
