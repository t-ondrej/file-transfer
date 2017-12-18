package sk.upjs.ics;

import com.google.inject.Inject;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

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
        logger = Logger.getLogger(FileClient.class.getName());
        openConnection();

        if (stateManager.existsPrevious()) {
            state = stateManager.getLastState();
            chunkRegistry = FileChunkRegistry.fromState(state);
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
        if (sockets.size() < 0) return true;

        requestInfo();
        waitAndProcessInfo(socketCount, directory);
        openSockets();

        CompletionService<Boolean> completionService = new ExecutorCompletionService<>(executorService);

        sockets.forEach(socket ->
                futures.add(completionService.submit(new RequestFileTask(fileAccessor,
                        socket,
                        chunkRegistry.getFileChunks(),
                        chunkRegistry.getWritten())))
        );

        boolean successfullyDownloaded = true;

        for (int i = 0; i < socketCount; i++) {
            try {
                Future<Boolean> done = completionService.take();

                if (!done.get()) {
                    successfullyDownloaded = false;
                    break;
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        fileAccessor.close();
        freeAllResources();

        return successfullyDownloaded;
    }

    /**
     * Stop downloading
     * Save current state
     * Free all resources
     */
    public void pauseDownloading() {
        logger.info("PAUSING: Thread: " + Thread.currentThread().getName());
        state.setPaused(true);
        futures.forEach(future -> future.cancel(true));
        futures = Collections.emptyList();

        stateManager.persist(state);
        freeAllResources();
    }

    /**
     * Delete the currently downloaded file
     * Free all resources
     */
    public void cancelDownloading() {
        logger.info("Thread: " + Thread.currentThread().getName());
        freeAllResources();
        fileAccessor.deleteFile();
    }

    public void resumeDownloading() {
        logger.info("Thread: " + Thread.currentThread().getName());
        state.setPaused(false);

        openSockets();

        sockets.forEach(socket ->
                futures.add(executorService
                            .submit(new RequestFileTask(fileAccessor,
                                    socket,
                                    chunkRegistry.getFileChunks(),
                                    chunkRegistry.getWritten()))));
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
        return state.addAndGetElapsed();
    }

    private void openConnection() {
        try {
            Socket managementSocket = new Socket(Config.SERVER_ADDRESS, Config.SERVER_PORT);
            dis = new DataInputStream(managementSocket.getInputStream());
            dos = new DataOutputStream(managementSocket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        logger.info("Connected to " + Config.SERVER_ADDRESS + " on " + Config.SERVER_PORT);
    }

    private void requestInfo() {
        try {
            dos.writeUTF(Dialect.REQUEST_INFO);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        logger.info("Client requested info");
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
            e.printStackTrace();
            return;
        }

        logger.info("Client obtained info");

        state = new TransferState(directory, fileName, fileLength, chunkSize, -1, 0);
        chunkRegistry = FileChunkRegistry.fromState(state);
        state.setSocketCount(socketCount);
        state.setServerTransferPort(serverTransferPort);

        fileAccessor = new FileAccessor(new File(directory + fileName), "rw");
        logger.info("Client processed info");
    }

    private void openSockets() {
        for (int i = 0; i < state.getSocketCount(); i++) {
            try {
                sockets.add(new Socket(Config.SERVER_ADDRESS, state.getServerTransferPort()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        logger.info("Client opened " + state.getSocketCount() + " transfer sockets on " + state.getServerTransferPort());
    }

    private void freeAllResources() {
        logger.info("Client freed all resources");
        logger.info("Thread: " + Thread.currentThread().getName());
        futures.forEach(future -> future.cancel(true));
        executorService.shutdownNow();

        closeDataStreams();

        sockets.forEach(socket -> {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        fileAccessor.close();
    }
}