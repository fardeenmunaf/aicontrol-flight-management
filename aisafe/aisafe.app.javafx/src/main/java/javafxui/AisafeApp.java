package javafxui;

import auth.infrastructure.AuthzConfigurator;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jpa.PersistenceSettings;

public class AisafeApp extends Application {

    @Override
    public void init() {
        AuthzConfigurator.configure(new JpaAutoTxUserRepository(
                PersistenceSettings.getPersistenceUnitName(),
                PersistenceSettings.getExtendedPersistenceProperties()));
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/javafxui/presentation/auth/LoginView.fxml"));
        final Scene scene = new Scene(loader.load(), 1280, 780);
        scene.getStylesheets().add(
                getClass().getResource("/javafxui/styles/theme.css").toExternalForm());
        primaryStage.setTitle("AISafe");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(final String[] args) {
        launch(args);
    }
}
