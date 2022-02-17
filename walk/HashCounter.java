package info.kgeorgiy.ja.urazov.walk;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashCounter {
    private static final int BUFFER_SIZE = 1024;
    private static final int HASH_SIZE = 20;
    private static final byte[] ERROR_HASH = new byte[HASH_SIZE];

    private final MessageDigest digest;

    public HashCounter(final String algorithm) throws WalkException {
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new WalkException(algorithm + " is not available in the environment.");
        }
    }

    private byte[] countHash(Path path) {
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytes;
            while ((bytes = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytes);
            }
            byte[] hash = digest.digest();
            digest.reset();
            return hash;
        } catch (IOException e) {
            System.err.println("Cannot read '" + path + "'.");
            return ERROR_HASH;
        }
    }

    private static void writeHash(String fileName, byte[] hash, BufferedWriter writer) throws WalkException {
        try {
            writer.write(String.format("%0" + (hash.length << 1) + "x %s", new BigInteger(1, hash), fileName));
            writer.newLine();
        } catch (IOException e) {
            throw new WalkException("Cannot write hash of '" + fileName + "'. " + e.getMessage());
        }
    }

    public void processFailedWriting(String fileName, BufferedWriter writer) throws WalkException {
        writeHash(fileName, ERROR_HASH, writer);
    }

    private void processWriting(Path path, BufferedWriter writer) throws WalkException {
        writeHash(path.toString(), countHash(path), writer);
    }

    public void processHash(Path path, boolean visitFailed, BufferedWriter writer) throws WalkException {
        if (visitFailed) {
            processFailedWriting(path.toString(), writer);
        } else {
            processWriting(path, writer);
        }
    }
}
