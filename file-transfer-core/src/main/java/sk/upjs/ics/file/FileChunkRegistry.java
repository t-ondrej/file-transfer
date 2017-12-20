package sk.upjs.ics.file;

import org.apache.log4j.Logger;
import sk.upjs.ics.state.TransferState;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Tomas on 9.12.2017.
 */
public class FileChunkRegistry {

    private BlockingQueue<FileChunk> fileChunks;

    private int totalChunks;

    private Logger logger = Logger.getLogger(getClass());

    public FileChunkRegistry(int fileLength, int chunkSize) {
        fileChunks = createFileChunks(fileLength, chunkSize);
    }

    public static FileChunkRegistry fromState(TransferState state) {
        FileChunkRegistry fileChunkRegistry = new FileChunkRegistry((int) state.getFileLength(),
                                                                    state.getChunkSize());

        fileChunkRegistry.fileChunks.removeIf(chunk -> chunk.getOffset() <= state.getStartingOffset());
        fileChunkRegistry.addPoisonPills(state.getSocketCount());
        state.setChunksRegistry(fileChunkRegistry);

        return fileChunkRegistry;
    }

    public BlockingQueue<FileChunk> getFileChunks() {
        return fileChunks;
    }

    public void addPoisonPills(int pillsCount) {
        for (int i = 0; i < pillsCount; i++)
            fileChunks.add(FileChunk.createPoisonPill());
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public int getChunksLeft() {
        return fileChunks.size();
    }

    public int firstUnreadOffset() {
        try {
            return fileChunks.take().getOffset();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return -1;
    }

    private BlockingQueue<FileChunk> createFileChunks(int fileLength, int chunkSize) {
        BlockingQueue<FileChunk> chunks = new LinkedBlockingDeque<>();

        int fileChunkCount = fileLength > chunkSize ? fileLength / chunkSize : 0;
        int fileResidue = fileLength % chunkSize;

        for (int i = 0; i < fileChunkCount; i++)
            chunks.add(new FileChunk(i * chunkSize, chunkSize));

        if (fileResidue > 0)
            chunks.add(new FileChunk(fileChunkCount * chunkSize, fileResidue));

        totalChunks = chunks.size();

        logger.info("File chunks created");
        return chunks;
    }
}
