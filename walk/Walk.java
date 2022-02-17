package info.kgeorgiy.ja.urazov.walk;

import java.io.BufferedWriter;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class Walk extends AbstractHashCountWalk {
    Walk(final String inputFileName, final String outputFileName) throws WalkException {
        super(inputFileName, outputFileName);
    }

    @Override
    protected void walkImpl(String fileName, BufferedWriter writer) throws WalkException {
        try {
            hashCounter.processHash(Path.of(fileName), false, writer);
        } catch (InvalidPathException e) {
            hashCounter.processFailedWriting(fileName, writer);
        }
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
