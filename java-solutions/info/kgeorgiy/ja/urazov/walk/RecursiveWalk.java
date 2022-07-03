package info.kgeorgiy.ja.urazov.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;

public class RecursiveWalk extends AbstractHashCountWalk {
    RecursiveWalk(final String inputFileName, final String outputFileName) throws WalkException {
        super(inputFileName, outputFileName);
    }

    @Override
    protected void walkImpl(String dirOrFileName, BufferedWriter writer) throws WalkException {
        try {
            FileVisitor fileVisitor = new FileVisitor(hashCounter, writer);
            Files.walkFileTree(Path.of(dirOrFileName), fileVisitor);
            if (fileVisitor.isVisitFailed()) {
                throw new WalkException("Cannot visit directory or file. " + fileVisitor.getCause());
            }
        } catch (IOException e) {
            throw new WalkException("Cannot visit directory or file. " + e.getMessage());
        } catch (InvalidPathException e) {
            hashCounter.processFailedWriting(dirOrFileName, writer);
        }
    }

    public static void main(String[] args) {
        if (incorrectInput(args)) {
            return;
        }
        try {
            new RecursiveWalk(args[0], args[1]).walk();
        } catch (WalkException e) {
            System.err.println(e.getMessage());
        }
    }
}
