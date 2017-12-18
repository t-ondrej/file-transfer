package sk.upjs.ics;

import sk.upjs.ics.commons.Config;
import sk.upjs.ics.commons.Dialect;
import sk.upjs.ics.file.FileAccessor;
import sk.upjs.ics.tasks.SendFileTask;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class FileServer {

    private ServerSocket serverSocket;
    private File file;
    private FileAccessor fileAccessor;

    private ExecutorService executorService;

    private Logger logger;

    private FileServer(int port, String filePath) {
        try {
            logger = Logger.getLogger(FileServer.class.getName());
            logger.info("Starting server...");

            serverSocket = new ServerSocket(port);
            executorService = Executors.newCachedThreadPool();

            this.file = new File(filePath);
            fileAccessor = new FileAccessor(this.file, "r");

            logger.info("Opened server socket on port: " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        FileServer.start(Config.SERVER_PORT, "C:\\Users\\Tomas\\Downloads\\The.Hateful.Eight\\The.Hateful.Eight.mkv");
        //	FileServer.start(Config.SERVER_PORT, "C:\\Users\\Tomas\\Downloads\\specifikaciaProjektu.docx");
    }

    public static void start(int port, String filePath) {
        FileServer fileServer = new FileServer(port, filePath);

        while (true) {
            Socket clientSocket;

            try {
                clientSocket = fileServer.serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            fileServer.logger.info("Accepted: " + clientSocket.getInetAddress());
            fileServer.handleRequest(clientSocket);
        }
    }

    private void handleRequest(Socket socket) {
        String operation = "none";

        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            operation = dis.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Received message: " + operation + " From: " + socket.getInetAddress());

        switch (operation) {
            case (Dialect.REQUEST_INFO):
                sendInfo(socket);
                break;
            case (Dialect.REQUEST_LISTENING):
                startListening(socket);
                break;
            default:
                logger.info("Unknown operation");
                break;
        }
    }

    private void sendInfo(Socket socket) {
        System.out.println("Server sending info");

        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            dos.writeInt(Config.SERVER_PORT);
            dos.writeUTF(file.getName());
            dos.writeInt((int) file.length());
            dos.writeInt(Config.CHUNK_SIZE);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startListening(Socket socket) {
        logger.info("Starting to listen for a chunk requests of the file: " + file.getName());
        executorService.submit(new SendFileTask(fileAccessor, socket));
    }

    private void freeAllResources() {
        logger.info("Server freed all resources");
        fileAccessor.close();

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}