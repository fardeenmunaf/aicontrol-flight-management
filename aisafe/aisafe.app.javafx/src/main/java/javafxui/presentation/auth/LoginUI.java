package javafxui.presentation.auth;

import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafxui.SessionContext;
import jpa.PersistenceSettings;

import java.io.IOException;

public class LoginUI {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Label dbBadge;
    @FXML private Button loginButton;

    @FXML
    private void initialize() {
        final boolean isPostgres = PersistenceSettings.getPersistenceUnitName().contains("postgres");
        dbBadge.setText(isPostgres ? "PostgreSQL" : "H2 In-Memory");
    }

    @FXML
    private void onLogin() {
        final String email    = emailField.getText().trim();
        final String password = passwordField.getText().trim();
        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Email and password are required.");
            return;
        }
        final boolean ok = AuthzRegistry.authenticationService()
                .authenticate(email, password)
                .isPresent();
        if (!ok) {
            errorLabel.setText("Invalid credentials. Please try again.");
            passwordField.clear();
            return;
        }
        SessionContext.setAuthenticated(true);
        openDashboard();
    }

    @FXML
    private void onSkip() {
        SessionContext.setAuthenticated(false);
        openDashboard();
    }

    private void openDashboard() {
        try {
            final var resource = getClass().getResource("/javafxui/presentation/dashboard/DashboardView.fxml");
            if (resource == null) {
                errorLabel.setText("Dashboard view not found.");
                return;
            }
            final FXMLLoader loader = new FXMLLoader(resource);
            final Stage stage = (Stage) loginButton.getScene().getWindow();
            final Scene scene = new Scene(loader.load(), stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(
                    getClass().getResource("/javafxui/styles/theme.css").toExternalForm());
            stage.setScene(scene);
        } catch (final IOException e) {
            errorLabel.setText("Error loading dashboard: " + e.getMessage());
        }
    }
}
