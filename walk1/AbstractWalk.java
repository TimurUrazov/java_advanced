package info.kgeorgiy.ja.urazov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public abstract class AbstractWalk {
    private final Path inputFilePath;
    private final Path outputFilePath;

    public AbstractWalk(final String inputFileName, final String outputFileName) throws WalkException {
        try {
            this.inputFilePath = Path.of(inputFileName);
        } catch (InvalidPathException e) {
            throw new WalkException("Error parsing input file name. " + e.getMessage());
        }
        try {
            this.outputFilePath = Path.of(outputFileName);
        } catch (InvalidPathException e) {
            throw new WalkException("Error parsing output file name. " + e.getMessage());
        }
    }

    protected void walk() throws WalkException {
        createOutputFileParentDir();
        try (BufferedReader reader = Files.newBufferedReader(inputFilePath, StandardCharsets.UTF_8)) {
            try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8);) {
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        walkImpl(line, writer);
                    }
                } catch (IOException e) {
                    throw new WalkException("Error while reading input file. "  + e.getMessage());
                }
            } catch (IOException e) {
                throw new WalkException("Cannot open or create output file: '" + outputFilePath + "'.");
            }
        } catch (IOException e) {
            throw new WalkException("Cannot read input file '" + inputFilePath + "'.");
        }
    }

    private void createOutputFileParentDir() throws WalkException {
        final Path outputFileParentDir = outputFilePath.getParent();
        if (outputFileParentDir != null) {
            try {
                Files.createDirectories(outputFileParentDir);
            } catch (FileAlreadyExistsException e) {
                throw new WalkException(
                        "File with name '" + outputFileParentDir.toString()
                        + "' already exists." + e.getMessage()
                );
            } catch (IOException e) {
                throw new WalkException("Error creating parent directory." + e.getMessage());
            }
        }
    }

    protected abstract void walkImpl(String fileName, BufferedWriter writer) throws WalkException;
}
