package sk.upjs.ics;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import sk.upjs.ics.commons.Config;
import sk.upjs.ics.commons.Dialect;
import sk.upjs.ics.file.FileAccessor;
import sk.upjs.ics.file.FileChunkRegistry;
import sk.upjs.ics.state.FileInfo;
import sk.upjs.ics.state.StateManager;
import sk.upjs.ics.state.TransferState;
import sk.upjs.ics.tasks.RequestFileTask;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FileClient extends CommunicationBase {

    private List<Socket> sockets = new ArrayList<>();
    private List<Future<Boolean>> futures = new ArrayList<>();
    private FileAccessor fileAccessor;
    private ExecutorService executorService = Executors.newCachedThreadPool();

    private StateManager stateManager;
    private TransferState state;
    private FileChunkRegistry chunkRegistry;

    @Inject
    public FileClient(StateManager stateManager) {
        this.stateManager = stateManager;
        logger = Logger.getLogger(getClass());

        if (stateManager.existsPrevious()) {
            state = stateManager.getLastState();
            chunkRegistry = FileChunkRegistry.fromState(state);
            fileAccessor = new FileAccessor(new File(state.getFullPath()), "rw");
        } else {
            state = TransferState.createDefault();
        }
    }

    /**
     * Requests file info, wait and process it, open sockets, start tasks
     *
     * @param socketCount number of sockets that will be available for transfer
     *                    its set by the user
     * @param directory directory where the downloaded file will be stored
     * @return true if the download succeded
     */
    public boolean startDownloading(int socketCount, String directory) {
        openConnection();
        requestInfo();
        waitAndProcessInfo(socketCount, directory);
        openSockets();

        return submitTasksAndAwait();
    }

    /**
     * Stop downloading
     * Save current state
     * Free all resources
     */
    public void pauseDownloading() {
        logger.info("CLIENT: pausing");
        stateManager.persist(state);

        stop();
    }

    /**
     * Delete the currently downloaded file
     * Free all resources
     */
    public void cancelDownloading() {
        logger.info("CLIENT: cancelling");
        fileAccessor.deleteFile();
        stateManager.deleteBackup();

        state = TransferState.createDefault();
        shutDown();
    }

    public boolean resumeDownloading() {
        logger.info("CLIENT: resuming");
        openSockets();
        return submitTasksAndAwait();
    }

    public boolean resumeAvailable() {
        return state != null && state.getProgress() > 0;
    }

    public double getProgress() {
        return state != null ? state.getProgress() : 0;
    }

    public FileInfo getFileInfo() {
        return state;
    }

    public int addAndGetElapsed() {
        return state != null ? state.addAndGetElapsed() : 0;
    }

    public int getElapsed() {
        return state != null ? state.getElapsed() : 0;
    }

    private boolean submitTasksAndAwait() {
        executorService = Executors.newCachedThreadPool();
        CompletionService<Boolean> completionService = new ExecutorCompletionService<>(executorService);

        fileAccessor = new FileAccessor(new File(state.getFullPath()), "rw");

        try {
            sockets.forEach(socket ->
                    futures.add(completionService.submit(new RequestFileTask(fileAccessor,
                            socket,
                            chunkRegistry.getFileChunks())))
            );
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            return false;
        }

        logger.info("CLIENT: tasks submitted");

        boolean successfullyDownloaded = true;

        for (int i = 0; i < state.getSocketCount(); i++) {
            try {
                Future<Boolean> done = completionService.take();
                done.get();

            } catch (CancellationException | InterruptedException | ExecutionException e) {
                logger.error(e);
                successfullyDownloaded = false;
            }
        }

        if (successfullyDownloaded) {
            stateManager.deleteBackup();
            logger.info("CLIENT: download successful");
            shutDown();
        } else {
            stateManager.persist(state);
            logger.info("CLIENT: download unsuccessful");
        }

        return successfullyDownloaded;
    }

    private void openConnection() {
        try {
            Socket managementSocket = new Socket(Config.SERVER_ADDRESS, Config.SERVER_PORT);
            dis = new DataInputStream(managementSocket.getInputStream());
            dos = new DataOutputStream(managementSocket.getOutputStream());
        } catch (Exception e) {
            logger.error(e);
            return;
        }

        logger.info("CLIENT: Connected to " + Config.SERVER_ADDRESS + " on " + Config.SERVER_PORT);
    }

    private void requestInfo() {
        try {
            dos.writeUTF(Dialect.REQUEST_INFO);
            dos.flush();
        } catch (IOException e) {
            logger.error(e);
            return;
        }

        logger.info("CLIENT: requested info");
    }

    private void waitAndProcessInfo(int socketCount, String directory) {
        String fileName;
        int serverTransferPort, fileLength, chunkSize;

        try {
            serverTransferPort = dis.readInt();
            fileName = dis.readUTF();
            fileLength = dis.readInt();
            chunkSize = dis.readInt();
        } catch (IOException e) {
            logger.error(e);
            return;
        }

        logger.info("CLIENT: obtained info");

        if (stateManager.existsPrevious())
                stateManager.deleteBackup();
        state = new TransferState(directory, fileName, fileLength,
                chunkSize, -1, 0, socketCount, serverTransferPort);
        chunkRegistry = FileChunkRegistry.fromState(state);

        logger.info("CLIENT: processed info");
    }

    private void openSockets() {
        sockets = new ArrayList<>();

        for (int i = 0; i < state.getSocketCount(); i++) {
            try {
                sockets.add(new Socket(Config.SERVER_ADDRESS, state.getServerTransferPort()));
            } catch (Exception e) {
                logger.error(e);
            }
        }

        logger.info("Client opened " + state.getSocketCount() + " transfer sockets " +
                    "on " + state.getServerTransferPort());
    }

    private void stop() {
        futures.forEach(future -> future.cancel(true));

        logger.info("CLIENT: stopped");
    }

    private void shutDown() {
        stop();
        executorService.shutdownNow();
        closeDataStreams();

        sockets.forEach(socket -> {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error(e);
            }
        });

        fileAccessor.close();

        logger.info("CLIENT: shutdown");
    }
}