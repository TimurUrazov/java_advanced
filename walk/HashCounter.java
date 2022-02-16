package info.kgeorgiy.ja.urazov.walk;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashCounter {
    private static final int HASH_SIZE = 20;

    public static byte[] countHash(Path path) throws WalkException {
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                byte[] buffer = new byte[1024];
                int bytes;
                while ((bytes = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytes);
                }
                return digest.digest();
            } catch (NoSuchAlgorithmException e) {
                throw new WalkException("SHA-1 is not available in the environment.");
            }
        } catch (IOException e) {
            System.err.println("Cannot read '" + path + "' " + e.getMessage());
            return new byte[HASH_SIZE];
        }
    }

    public static void writeHash(String fileName, byte[] hash, BufferedWriter writer) throws WalkException {
        try {
            writer.write(String.format("%0" + (hash.length << 1) + "x %s", new BigInteger(1, hash), fileName));
            writer.newLine();
        } catch (IOException e) {
            throw new WalkException("Cannot write hash '" + fileName + "': " + e.getMessage());
        }
    }

    private static void processFailedWriting(String fileName, BufferedWriter writer) throws WalkException {
        writeHash(fileName, new byte[HASH_SIZE], writer);
    }

    private static void processWriting(String fileName, BufferedWriter writer) throws WalkException {
        try {
            Path path = Path.of(fileName);
            writeHash(fileName, countHash(path), writer);
        } catch (InvalidPathException e) {
            System.err.println("Error parsing file name '" + fileName + "': " + e.getMessage());
            processFailedWriting(fileName, writer);
        }
    }

    public static void processHash(String fileName, boolean visitFailed, BufferedWriter writer) throws WalkException {
        if (visitFailed) {
            processFailedWriting(fileName, writer);
        } else {
            processWriting(fileName, writer);
        }
    }
}
