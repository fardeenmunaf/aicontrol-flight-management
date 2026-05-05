package enginemodel.application;

import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import auth.domain.AISafeRoles;
import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineModelMaker;
import enginemodel.domain.EngineType;
import enginemodel.domain.FuelProperties;
import enginemodel.domain.FuelType;
import enginemodel.repositories.EngineModelRepository;

/**
 * Controller for registering new engine models.
 * Enforces authorization, uniqueness constraints, and validation rules.
 *
 * Acceptance Criteria:
 * AC01: Only authenticated Backoffice Operator can register
 * AC02: Name + manufacturer combination must be unique
 * AC03: Engine type must be valid (enum values)
 * AC04: Power, fuel type, efficiency must be positive
 * AC05-06: Proper error handling for duplicates and missing fields
 *
 * @author LAPR4 Team
 */
public class RegisterEngineModelController {

    private final AuthorizationService authz =
            AuthzRegistry.authorizationService();
    private final EngineModelRepository engineModelRepository;

    public RegisterEngineModelController(final EngineModelRepository engineModelRepository) {
        this.engineModelRepository = engineModelRepository;
    }

    /**
     * Registers a new engine model.
     *
     * @param name the engine model name
     * @param type the engine type (turbofan, turboprop, etc.)
     * @param power the power in appropriate units (must be positive)
     * @param efficiency the efficiency/TSFC value (must be positive)
     * @param thrustSeaLevelStopped thrust at sea level stopped (must be positive)
     * @param thrustSeaLevelCruise thrust at sea level cruise (must be positive)
     * @param thrustSpecificFuelConsumption TSFC value (must be positive)
     * @param makerName the manufacturer name
     * @param makerCountry the manufacturer country
     * @param fuelType the fuel type (JET_A, AVGAS, DIESEL, ELECTRIC)
     * @param fuelDensity the fuel density (must be positive)
     * @param fuelSpecificEnergy the fuel specific energy (must be positive)
     * @return the registered EngineModel
     * @throws IllegalArgumentException if validation fails or duplicate exists
     * @throws IllegalStateException if user is not authorized
     */
    public EngineModel registerEngineModel(
            final String name,
            final EngineType type,
            final Double power,
            final Double efficiency,
            final Double thrustSeaLevelStopped,
            final Double thrustSeaLevelCruise,
            final Double thrustSpecificFuelConsumption,
            final String makerName,
            final String makerCountry,
            final FuelType fuelType,
            final Double fuelDensity,
            final Double fuelSpecificEnergy) {

        // AC01: Authorization check — only Backoffice Operator
        authz.ensureAuthenticatedUserHasAnyOf(AISafeRoles.BACKOFFICE_OPERATOR);

        // AC06: Validate required fields are not null/empty
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Engine model name cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Engine type must be specified");
        }
        if (makerName == null || makerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Manufacturer name cannot be null or empty");
        }
        if (makerCountry == null || makerCountry.trim().isEmpty()) {
            throw new IllegalArgumentException("Manufacturer country cannot be null or empty");
        }
        if (fuelType == null) {
            throw new IllegalArgumentException("Fuel type must be specified");
        }

        // AC04: Validate numeric values are positive
        if (power == null || power <= 0) {
            throw new IllegalArgumentException("Power must be a positive numeric value");
        }
        if (efficiency == null || efficiency <= 0) {
            throw new IllegalArgumentException("Efficiency must be a positive numeric value");
        }
        if (thrustSeaLevelStopped == null || thrustSeaLevelStopped <= 0) {
            throw new IllegalArgumentException("Thrust at sea level stopped must be a positive numeric value");
        }
        if (thrustSeaLevelCruise == null || thrustSeaLevelCruise <= 0) {
            throw new IllegalArgumentException("Thrust at sea level cruise must be a positive numeric value");
        }
        if (thrustSpecificFuelConsumption == null || thrustSpecificFuelConsumption <= 0) {
            throw new IllegalArgumentException("Thrust Specific Fuel Consumption (TSFC) must be a positive numeric value");
        }
        if (fuelDensity == null || fuelDensity <= 0) {
            throw new IllegalArgumentException("Fuel density must be a positive numeric value");
        }
        if (fuelSpecificEnergy == null || fuelSpecificEnergy <= 0) {
            throw new IllegalArgumentException("Fuel specific energy must be a positive numeric value");
        }

        // AC02: Check duplicate using an atomic repository query on name + maker
        if (engineModelRepository.findByNameAndMakerName(name, makerName).isPresent()) {
            throw new IllegalArgumentException(String.format(
                    "Engine model '%s' by manufacturer '%s' already exists in the system",
                    name,
                    makerName
            ));
        }

        // Create domain objects
        final var maker = new EngineModelMaker(makerName, makerCountry);
        final var fuelProperties = new FuelProperties(fuelType, fuelDensity, fuelSpecificEnergy);

        // Create engine model (domain constructor handles additional validation)
        final var engineModel = new EngineModel(
                name,
                type,
                power,
                efficiency,
                thrustSeaLevelStopped,
                thrustSeaLevelCruise,
                thrustSpecificFuelConsumption,
                maker,
                fuelProperties
        );

        // Persist to repository
        return engineModelRepository.save(engineModel);
    }
}