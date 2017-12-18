package sk.upjs.ics.state;

/**
 * Created by Tomas on 18.12.2017.
 */
public class FileInfo {

    protected String directory;
    protected String fileName;
    protected long fileLength;
    protected int chunkSize;
    protected int startingOffset;

    public FileInfo(String directory, String fileName, long fileLength, int chunkSize, int startingOffset) {
        this.directory = directory;
        this.fileName = fileName;
        this.fileLength = fileLength;
        this.chunkSize = chunkSize;
        this.startingOffset = startingOffset;
    }

    public String getDirectory() {
        return directory;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileLength() {
        return fileLength;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getStartingOffset() {
        return startingOffset;
    }
}
