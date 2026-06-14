package javafxui.presentation.dashboard;

import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafxui.SessionContext;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardUI {

    private record Module(String icon, String name, String description, String fxmlPath) { }

    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        showHub();
    }

    /* Same gating as the old sidebar: roles decide which modules appear. */
    private List<Module> availableModules() {
        final List<Module> modules = new ArrayList<>();
        modules.add(new Module("🛰", "Simulation", "Run & watch live flights",
                "/javafxui/presentation/simulation/SimulationView.fxml"));
        modules.add(new Module("🗺", "Airports Map", "Browse the airport network",
                "/javafxui/presentation/airports/AirportsMapView.fxml"));

        final var authz = AuthzRegistry.authorizationService();

        if (authz.isAuthenticatedUserAuthorizedTo(AISafeRoles.ADMIN)) {
            modules.add(new Module("👪", "User Management", "Manage system users",
                    "/javafxui/presentation/user/UsersManagementView.fxml"));
        }
        if (authz.isAuthenticatedUserAuthorizedTo(AISafeRoles.BACKOFFICE_OPERATOR)) {
            modules.add(new Module("⚙", "Engine Models", "Engine catalogue",
                    "/javafxui/presentation/enginemodel/EngineModelsView.fxml"));
            modules.add(new Module("✈", "Aircraft Models", "Aircraft catalogue",
                    "/javafxui/presentation/aircraftmodel/AircraftModelsView.fxml"));
            modules.add(new Module("🏛", "Air Transport Companies", "Manage companies",
                    "/javafxui/presentation/airtransportcompany/AirTransportCompaniesView.fxml"));
            modules.add(new Module("◎", "Air Control Areas", "Control area registry",
                    "/javafxui/presentation/aircontrolarea/AirControlAreasView.fxml"));
            modules.add(new Module("⚭", "Customer Collaborators", "Customer staff accounts",
                    "/javafxui/presentation/customercollaborator/CustomerCollaboratorsView.fxml"));
        }
        if (authz.isAuthenticatedUserAuthorizedTo(AISafeRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
            modules.add(new Module("🛩", "Fleet", "Manage aircraft",
                    "/javafxui/presentation/fleet/FleetView.fxml"));
            modules.add(new Module("⚑", "Flight Routes", "Create & manage routes",
                    "/javafxui/presentation/flightroute/FlightRoutesView.fxml"));
            modules.add(new Module("🔍", "Available Pilots", "Browse free pilots",
                    "/javafxui/presentation/pilot/AvailablePilotsView.fxml"));
            modules.add(new Module("🎖", "Company Pilots", "Company roster",
                    "/javafxui/presentation/pilot/CompanyPilotsView.fxml"));
        }
        if (authz.isAuthenticatedUserAuthorizedTo(AISafeRoles.PILOT)) {
            modules.add(new Module("📋", "Flight Plans", "Create & validate plans",
                    "/javafxui/presentation/flightplan/FlightPlanView.fxml"));
        }
        if (authz.isAuthenticatedUserAuthorizedTo(AISafeRoles.FLIGHT_CONTROL_OPERATOR)) {
            modules.add(new Module("📊", "Monthly Report", "Generate monthly statistics",
                    "/javafxui/presentation/report/MonthlyReportView.fxml"));
        }
        if (SessionContext.isWeatherClient()) {
            modules.add(new Module("🌦", "Weather Client", "Weather data feed",
                    "/javafxui/presentation/weather/WeatherClientView.fxml"));
        }
        return modules;
    }

    /* ── Hub screen ──────────────────────────────────────────────── */

    private void showHub() {
        final HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        final Label brand = new Label("✈ AISAFE");
        brand.getStyleClass().add("topbar-brand");
        final Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final Label meta = new Label(userMeta());
        meta.getStyleClass().add("topbar-meta");
        header.getChildren().addAll(brand, spacer, meta, sessionButton());

        final Label greeting = new Label(greetingText());
        greeting.getStyleClass().add("hub-greeting");
        final Label sub = new Label("Air Traffic Safety System · "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));
        sub.getStyleClass().add("hub-sub");

        final TilePane grid = new TilePane(16, 16);
        grid.setPrefColumns(4);
        for (final Module m : availableModules()) {
            grid.getChildren().add(buildTile(m));
        }

        final Region headerGap = new Region();
        headerGap.setMinHeight(24);
        final Region gridGap = new Region();
        gridGap.setMinHeight(16);
        final VBox hub = new VBox(8, header, headerGap, greeting, sub, gridGap, grid);
        hub.getStyleClass().add("hub-root");

        final ScrollPane scroller = new ScrollPane(hub);
        scroller.setFitToWidth(true);
        scroller.setFitToHeight(true);
        contentArea.getChildren().setAll(scroller);
    }

    private VBox buildTile(final Module m) {
        final Label icon = new Label(m.icon());
        icon.getStyleClass().add("hub-tile-icon");
        final Label name = new Label(m.name());
        name.getStyleClass().add("hub-tile-name");
        final Label desc = new Label(m.description());
        desc.getStyleClass().add("hub-tile-desc");
        desc.setWrapText(true);

        final VBox tile = new VBox(icon, name, desc);
        tile.getStyleClass().add("hub-tile");
        tile.setOnMouseClicked(e -> openModule(m));
        return tile;
    }

    /* ── Module screen ───────────────────────────────────────────── */

    private void openModule(final Module m) {
        try {
            final var resource = getClass().getResource(m.fxmlPath());
            if (resource == null) {
                showError("View not found: " + m.fxmlPath());
                return;
            }
            final Node view = new FXMLLoader(resource).load();

            final Button back = new Button("← Hub");
            back.getStyleClass().add("topbar-back");
            back.setOnAction(e -> showHub());

            final Label title = new Label(m.icon() + " " + m.name());
            title.getStyleClass().add("topbar-brand");

            final Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            final Label meta = new Label(userMeta());
            meta.getStyleClass().add("topbar-meta");

            final HBox topbar = new HBox(12, back, title, spacer, meta, sessionButton());
            topbar.getStyleClass().add("module-topbar");
            topbar.setAlignment(Pos.CENTER_LEFT);

            final BorderPane page = new BorderPane();
            page.setTop(topbar);
            page.setCenter(view);
            contentArea.getChildren().setAll(page);
        } catch (final IOException e) {
            showError("Failed to load view: " + e.getMessage());
        }
    }

    /* ── Shared bits ─────────────────────────────────────────────── */

    private Button sessionButton() {
        final Button btn;
        if (SessionContext.isAuthenticated()) {
            btn = new Button("Logout");
            btn.setOnAction(e -> onLogout());
        } else {
            btn = new Button("Login");
            btn.setOnAction(e -> loadAuthScene());
        }
        btn.getStyleClass().add(SessionContext.isAuthenticated() ? "btn-danger" : "btn-primary");
        return btn;
    }

    private String greetingText() {
        final String name = currentFirstName();
        final int hour = LocalTime.now().getHour();
        final String part = hour < 12 ? "Good morning" : hour < 19 ? "Good afternoon" : "Good evening";
        return name == null || name.isBlank() ? part : part + ", " + name;
    }

    private String currentFirstName() {
        try {
            return AuthzRegistry.authorizationService().session()
                    .map(s -> s.authenticatedUser().name().firstName())
                    .orElse(null);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private String userMeta() {
        final String name = currentFirstName();
        if (name == null || name.isBlank()) {
            return SessionContext.isWeatherClient() ? "Weather client" : "Guest";
        }
        return name;
    }

    private void showError(final String message) {
        final Label err = new Label(message);
        err.getStyleClass().add("error-label");
        contentArea.getChildren().setAll(err);
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
            final Stage stage = (Stage) contentArea.getScene().getWindow();
            final Scene scene = new Scene(loader.load(),
                    stage.getScene().getWidth(), stage.getScene().getHeight());
            final var css = getClass().getResource("/javafxui/styles/theme.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
        } catch (final IOException e) {
            showError("Navigation failed: " + e.getMessage());
        }
    }
}
