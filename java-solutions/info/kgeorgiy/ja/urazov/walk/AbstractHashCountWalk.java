package info.kgeorgiy.ja.urazov.walk;

public abstract class AbstractHashCountWalk extends AbstractWalk {
    private final static String ALGORITHM = "SHA-1";

    protected final HashCounter hashCounter;

    AbstractHashCountWalk(final String inputFileName, final String outputFileName) throws WalkException {
        super(inputFileName, outputFileName);
        hashCounter = new HashCounter(ALGORITHM);
    }

    protected static boolean incorrectInput(String[] args) {
        if (args == null  || args.length != 2) {
            System.err.println("Incorrect number of arguments. Usage: <inputFile> <outputFile>.");
            return true;
        }
        if (args[0] == null ||  args[1] == null) {
            System.err.println("Non-null file names are required.");
            return true;
        }
        return false;
    }
}
