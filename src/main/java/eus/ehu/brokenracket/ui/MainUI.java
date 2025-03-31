package eus.ehu.brokenracket.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainUI extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load the FXML file from the resources folder
        URL fxmlLocation = getClass().getResource("mainui.fxml");
        if (fxmlLocation == null) {
            System.err.println("Cannot find FXML file");
            return;
        }
        Parent root = FXMLLoader.load(fxmlLocation);

        // Set up the scene and stage
        Scene scene = new Scene(root, 600, 400); // Adjust size as needed
        primaryStage.setTitle("Book Court Test UI");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
} 