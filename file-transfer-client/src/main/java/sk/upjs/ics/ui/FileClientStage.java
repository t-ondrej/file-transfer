package sk.upjs.ics.ui;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import sk.upjs.ics.TransferModule;
import sk.upjs.ics.ui.controllers.ClientSceneController;


/**
 * Created by Tomas on 9.12.2017.
 */
public class FileClientStage extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Logger logger = Logger.getLogger(getClass());
        logger.info("Starting client GUI...");

        Injector injector = Guice.createInjector(new TransferModule());

        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(injector::getInstance);
        loader.setLocation(getClass().getResource("/client-scene.fxml"));
        Scene scene = new Scene(loader.load());
        ClientSceneController clientSceneController = loader.getController();
        clientSceneController.setCurrentStage(primaryStage);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        Platform.exit();
        System.exit(0);
    }
}
