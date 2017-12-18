package sk.upjs.ics.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by Tomas on 7.12.2017.
 */
public class FileAccessor {

    private RandomAccessFile fileAccess;
    private File file;

    public FileAccessor(File file, String mode) {
        this.file = file;

        try {
            fileAccess = new RandomAccessFile(file, mode);
            System.out.println("Created file access to " + file.getName());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public synchronized byte[] read(int offset, int length) {
        System.out.println("Reading start " + offset + " " + length);

        if (offset < 0 || length < 1) {
            System.out.println("Returned");
            return new byte[0];
        }

        byte[] buffer = new byte[length];

        try {
            fileAccess.seek(offset);
            fileAccess.read(buffer, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer;
    }

    public synchronized void write(int offset, byte[] buffer) {
        System.out.println("Writing start");
        try {
            fileAccess.seek(offset);
            fileAccess.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean deleteFile() {
        return file.delete();
    }

    public synchronized void close() {
        try {
            if (fileAccess != null) {
                System.out.println("Closing fileaccess");

                fileAccess.close();
                fileAccess = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
