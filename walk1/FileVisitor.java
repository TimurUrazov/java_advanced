package info.kgeorgiy.ja.urazov.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileVisitor extends SimpleFileVisitor<Path> {
    private final HashCounter hashCounter;
    private final BufferedWriter writer;
    private boolean visitFailed;
    private String cause;

    public FileVisitor(HashCounter hashCounter, BufferedWriter writer) {
        this.hashCounter = hashCounter;
        this.writer = writer;
    }

    public String getCause() {
        return cause;
    }

    public boolean isVisitFailed() {
        return visitFailed;
    }

    private FileVisitResult processVisit(Path fileName, boolean visitFileFailed) {
        try {
            hashCounter.processHash(fileName, visitFileFailed, writer);
            return FileVisitResult.CONTINUE;
        } catch (WalkException e) {
            visitFailed = true;
            cause = e.getMessage();
            return FileVisitResult.TERMINATE;
        }
    }
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)  {
        return processVisit(file, false);
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return processVisit(file, true);
    }
}
