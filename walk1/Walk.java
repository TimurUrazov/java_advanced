package info.kgeorgiy.ja.urazov.walk;

import java.io.BufferedWriter;

public class Walk extends AbstractHashCountWalk {
    Walk(final String inputFileName, final String outputFileName) throws WalkException {
        super(inputFileName, outputFileName);
    }

    @Override
    protected void walkImpl(String fileName, BufferedWriter writer) throws WalkException {
        hashCounter.processHash(createPath(fileName, writer), false, writer);
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
