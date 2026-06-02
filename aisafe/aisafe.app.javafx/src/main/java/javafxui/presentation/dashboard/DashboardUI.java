package javafxui.presentation.dashboard;

import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafxui.SessionContext;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardUI {

    @FXML private VBox      sidebar;
    @FXML private StackPane contentArea;

    private final List<Button> navButtons = new ArrayList<>();
    private Button activeButton;

    @FXML
    public void initialize() {
        buildSidebar();
        showHomeScreen();
    }

    private void buildSidebar() {
        // Brand header
        final Label brand = new Label("✈  AISAFE");
        brand.getStyleClass().add("sidebar-brand");
        final VBox header = new VBox(brand);
        header.getStyleClass().add("sidebar-header");
        header.setMinWidth(220);
        sidebar.getChildren().add(header);

        addNavButton("Simulation",    "/javafxui/presentation/simulation/SimulationView.fxml");
        addNavButton("Airports Map",  "/javafxui/presentation/airports/AirportsMapView.fxml");

        final var authz = AuthzRegistry.authorizationService();

        if (authz.isAuthenticatedUserAuthorizedTo(AISafeRoles.ADMIN)) {
            addNavButton("User Management", "/javafxui/presentation/user/UsersManagementView.fxml");
        }

        if (authz.isAuthenticatedUserAuthorizedTo(AISafeRoles.BACKOFFICE_OPERATOR)) {
            addNavButton("Engine Models",           "/javafxui/presentation/enginemodel/EngineModelsView.fxml");
            addNavButton("Aircraft Models",         "/javafxui/presentation/aircraftmodel/AircraftModelsView.fxml");
            addNavButton("Air Transport Companies", "/javafxui/presentation/airtransportcompany/AirTransportCompaniesView.fxml");
            addNavButton("Air Control Areas",       "/javafxui/presentation/aircontrolarea/AirControlAreasView.fxml");
            addNavButton("Customer Collaborators",  "/javafxui/presentation/customercollaborator/CustomerCollaboratorsView.fxml");
        }

        if (authz.isAuthenticatedUserAuthorizedTo(AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
            addNavButton("Fleet",             "/javafxui/presentation/fleet/FleetView.fxml");
            addNavButton("Flight Routes",     "/javafxui/presentation/flightroute/FlightRoutesView.fxml");
            addNavButton("Available Pilots",  "/javafxui/presentation/pilot/AvailablePilotsView.fxml");
            addNavButton("Company Pilots",    "/javafxui/presentation/pilot/CompanyPilotsView.fxml");
        }

        if (authz.isAuthenticatedUserAuthorizedTo(AISafeRoles.PILOT)) {
            addNavButton("Flight Plans", "/javafxui/presentation/flightplan/FlightPlanView.fxml");
        }

        final Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        if (SessionContext.isAuthenticated()) {
            final Button logoutBtn = new Button("↪  Logout");
            logoutBtn.getStyleClass().add("nav-btn");
            logoutBtn.setOnAction(e -> onLogout());
            sidebar.getChildren().add(logoutBtn);
        } else {
            final Button loginBtn = new Button("↩  Login");
            loginBtn.getStyleClass().add("nav-btn");
            loginBtn.setOnAction(e -> goToLogin());
            sidebar.getChildren().add(loginBtn);
        }
    }

    private void showHomeScreen() {
        final String name;
        try {
            name = AuthzRegistry.authorizationService().session()
                    .map(s -> s.authenticatedUser().name().firstName())
                    .orElse(null);
        } catch (final Exception ignored) {
            buildHomeScreen(null);
            return;
        }
        buildHomeScreen(name);
    }

    private void buildHomeScreen(final String firstName) {
        // Watermark
        final Label watermark = new Label("✈");
        watermark.getStyleClass().add("home-watermark");

        // Welcome card
        final Label icon = new Label("✈");
        icon.getStyleClass().add("logo-icon");
        icon.setStyle("-fx-font-size:32px;-fx-min-width:48px;-fx-max-width:48px;-fx-min-height:48px;-fx-max-height:48px;");

        final String greeting = (firstName != null && !firstName.isBlank())
                ? "Welcome back, " + firstName
                : "Welcome";
        final Label title = new Label(greeting);
        title.getStyleClass().add("home-title");

        final Label subtitle = new Label("Air Traffic Safety System");
        subtitle.getStyleClass().add("home-subtitle");

        final String dateStr = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"));
        final Label date = new Label(dateStr);
        date.getStyleClass().add("home-subtitle");

        final Region divider = new Region();
        divider.getStyleClass().add("home-divider");
        divider.setMaxWidth(Double.MAX_VALUE);

        final VBox card = new VBox(12, icon, title, subtitle, divider, date);
        card.getStyleClass().add("home-card");
        card.setAlignment(Pos.CENTER);

        final StackPane home = new StackPane(watermark, card);
        home.setAlignment(Pos.CENTER);
        home.setStyle("-fx-background-color:#f5f7fa;");

        contentArea.getChildren().setAll(home);
    }

    private void addNavButton(final String label, final String fxmlPath) {
        final Button btn = new Button(label);
        btn.getStyleClass().add("nav-btn");
        btn.setOnAction(e -> navigate(fxmlPath, btn));
        sidebar.getChildren().add(btn);
        navButtons.add(btn);
    }

    private void navigate(final String fxmlPath, final Button btn) {
        try {
            final var resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                final Label err = new Label("View not found: " + fxmlPath);
                err.getStyleClass().add("error-label");
                contentArea.getChildren().setAll(err);
                return;
            }
            final FXMLLoader loader = new FXMLLoader(resource);
            final Node view = loader.load();
            contentArea.getChildren().setAll(view);

            if (activeButton != null) activeButton.getStyleClass().remove("nav-btn-active");
            if (btn != null) {
                btn.getStyleClass().add("nav-btn-active");
                activeButton = btn;
            }
        } catch (final IOException e) {
            final Label err = new Label("Failed to load view: " + e.getMessage());
            err.getStyleClass().add("error-label");
            contentArea.getChildren().setAll(err);
        }
    }

    private void goToLogin() {
        loadAuthScene();
    }

    private void onLogout() {
        AuthzRegistry.authorizationService().clearSession();
        SessionContext.clear();
        loadAuthScene();
    }

    private void loadAuthScene() {
        try {
            final var resource = getClass().getResource("/javafxui/presentation/auth/LoginView.fxml");
            if (resource == null) return;
            final FXMLLoader loader = new FXMLLoader(resource);
            final Stage stage = (Stage) sidebar.getScene().getWindow();
            final Scene scene = new Scene(loader.load(), stage.getScene().getWidth(), stage.getScene().getHeight());
            final var css = getClass().getResource("/javafxui/styles/theme.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
        } catch (final IOException e) {
            final Label err = new Label("Navigation failed: " + e.getMessage());
            err.getStyleClass().add("error-label");
            contentArea.getChildren().setAll(err);
        }
    }
}
