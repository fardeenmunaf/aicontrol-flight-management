package backoffice.presentation.aircontrolarea;

import aircontrolarea.application.RegisterAirControlAreaController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import infrastructure.persistence.PersistenceContext;

public class RegisterAirControlAreaUI extends AbstractUI {

    private final RegisterAirControlAreaController controller =
            new RegisterAirControlAreaController(
                    PersistenceContext.repositories().airControlAreas());

    @Override
    protected boolean doShow() {
        try {
            final String areaCode = Console.readLine("Area code");
            final String fuelStr = Console.readLine("Minimum required arrival fuel (e.g. 500.0)");
            final double minimumRequiredArrivalFuel = Double.parseDouble(fuelStr);
            final String geographicBoundaries = Console.readLine("Geographic boundaries (e.g. lat1,lon1;lat2,lon2;...)");

            controller.registerAirControlArea(areaCode, minimumRequiredArrivalFuel, geographicBoundaries);

            System.out.println("\nAir Control Area registered successfully.");
            return true;
        } catch (final NumberFormatException ex) {
            System.out.println("\nError: Minimum required arrival fuel must be a valid number.");
            return false;
        } catch (final IllegalArgumentException ex) {
            System.out.println("\nError: " + ex.getMessage());
            return false;
        } catch (final Exception ex) {
            System.out.println("\nError registering air control area: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String headline() {
        return "Register New Air Control Area";
    }
}