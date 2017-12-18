package sk.upjs.ics.file;

/**
 * Created by Tomas on 9.12.2017.
 */
public class FileChunk {

    private int offset;
    private int length;

    public FileChunk(int offset, int length) {
        this.offset = offset;
        this.length = length;
    }

    public static FileChunk createPoisonPill() {
        return new FileChunk(-1, -1);
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public boolean isPoisonPill() {
        return offset == -1 && length == -1;
    }
}
