package sk.upjs.ics.file;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;

/**
 * Created by Tomas on 7.12.2017.
 */
public class FileAccessor {

    private final Object lock = new Object();
    private RandomAccessFile fileAccess;
    private File file;

    private boolean isClosed;

    private Logger logger = Logger.getLogger(getClass());

    public FileAccessor(File file, String mode) {
        this.file = file;

        try {
            fileAccess = new RandomAccessFile(file, mode);
            logger.info("Created file access to " + file.getName());
        } catch (FileNotFoundException e) {
            logger.error(e);
        }
    }

    public byte[] read(int offset, int length) {
        if (offset < 0 || length < 1) {
            logger.error("Invalid byte read");
            return new byte[0];
        }

        byte[] buffer = new byte[length];

        try {
            synchronized (lock) {
                fileAccess.seek(offset);
                fileAccess.read(buffer, 0, length);
            }
        } catch (IOException e) {
            logger.error(e);
        }

        return buffer;
    }

    public void write(int offset, byte[] buffer) {
        try {
            synchronized (lock) {
                if (fileAccess == null) return;

                fileAccess.seek(offset);
                fileAccess.write(buffer);
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public boolean deleteFile() {
        synchronized (lock) {
            try {
                close();
                Files.delete(file.toPath());
                logger.info("Deleted " + file.toPath());
            } catch (IOException e) {
                logger.error(e);
                return false;
            }

            return true;
        }
    }

    public void close() {
        try {
            synchronized (lock) {
                if (!isClosed) {
                    fileAccess.close();
                    isClosed = true;
                    logger.info("File access closed");
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }
}
