package aircraftmodel.domain;

import enginemodel.domain.EngineModel;
import enginemodel.domain.EngineModelMaker;
import enginemodel.domain.EngineType;
import enginemodel.domain.FuelProperties;
import enginemodel.domain.FuelType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AircraftModelTest {

    private static final AircraftModelMaker MAKER = new AircraftModelMaker("Boeing", "USA");

    private AircraftModel validModel() {
        return new AircraftModel(
                AircraftType.PASSENGER,
                40000.0, 75000.0, 65000.0, 20000.0,
                12000.0, 850.0, 125.0, 35.0, 0.025, 1.2,
                "B737", 2, MAKER);
    }

    // --- Construction: happy path ---

    @Test
    void ensureAircraftModelIsCreatedWithExpectedState() {
        final AircraftModel model = validModel();
        assertAll(
                () -> assertEquals(AircraftType.PASSENGER, model.type()),
                () -> assertEquals(40000.0, model.emptyWeight()),
                () -> assertEquals(75000.0, model.maximumTakeOffWeight()),
                () -> assertEquals(65000.0, model.maximumZeroFuelWeight()),
                () -> assertEquals(20000.0, model.maximumFuelCapacity()),
                () -> assertEquals(12000.0, model.serviceCeiling()),
                () -> assertEquals(850.0, model.cruiseSpeed()),
                () -> assertEquals(125.0, model.wingArea()),
                () -> assertEquals(35.0, model.wingSpan()),
                () -> assertEquals(0.025, model.dragCoefficient()),
                () -> assertEquals(1.2, model.liftCoefficient()),
                () -> assertEquals("B737", model.modelName()),
                () -> assertEquals(2, model.numberOfEngines()),
                () -> assertEquals(MAKER, model.maker()),
                () -> assertNotNull(model.identity())
        );
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullTypeIsRejected() {
        assertThrows(Exception.class, () ->
                new AircraftModel(null, 40000.0, 75000.0, 65000.0, 20000.0,
                        12000.0, 850.0, 125.0, 35.0, 0.025, 1.2, "B737", 2, MAKER));
    }

    @Test
    void ensureZeroEmptyWeightIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel(AircraftType.PASSENGER,
                        0.0, 75000.0, 65000.0, 20000.0,
                        12000.0, 850.0, 125.0, 35.0, 0.025, 1.2, "B737", 2, MAKER));
    }

    @Test
    void ensureMaxTakeoffWeightLessThanEmptyWeightIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel(AircraftType.PASSENGER,
                        75000.0, 40000.0, 35000.0, 20000.0,
                        12000.0, 850.0, 125.0, 35.0, 0.025, 1.2, "B737", 2, MAKER));
    }

    @Test
    void ensureMaxZeroFuelWeightGreaterThanMaxTakeoffWeightIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel(AircraftType.PASSENGER,
                        40000.0, 75000.0, 80000.0, 20000.0,
                        12000.0, 850.0, 125.0, 35.0, 0.025, 1.2, "B737", 2, MAKER));
    }

    @Test
    void ensureNegativeNumberOfEnginesIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel(AircraftType.PASSENGER,
                        40000.0, 75000.0, 65000.0, 20000.0,
                        12000.0, 850.0, 125.0, 35.0, 0.025, 1.2, "B737", -1, MAKER));
    }

    // --- addSupportedEngineModel ---

    @Test
    void ensureEngineModelCanBeAdded() {
        final AircraftModel model = validModel();
        final EngineModel engine = new EngineModel(
                "CFM56", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                new EngineModelMaker("CFM", "USA"),
                new FuelProperties(FuelType.JET_A, 0.8, 43.2));

        model.addSupportedEngineModel(engine);
        assertTrue(model.supportedEngineModels().contains(engine));
    }

    @Test
    void ensureNullEngineModelIsRejected() {
        final AircraftModel model = validModel();
        assertThrows(Exception.class, () -> model.addSupportedEngineModel(null));
    }

    @Test
    void ensureSupportedEngineModelsIsDefensiveCopy() {
        final AircraftModel model = validModel();
        assertTrue(model.supportedEngineModels().isEmpty());
    }

    // --- equals / hashCode ---

    @Test
    void ensureModelIsEqualToItself() {
        final AircraftModel model = validModel();
        assertEquals(model, model);
        assertEquals(model.hashCode(), model.hashCode());
    }

    @Test
    void ensureModelIsNotEqualToNull() {
        assertNotEquals(validModel(), null);
    }

    @Test
    void ensureTwoDifferentModelsAreNotEqual() {
        final AircraftModel m1 = validModel();
        final AircraftModel m2 = validModel();
        assertNotEquals(m1, m2);
    }

    @Test
    void ensureModelSameAsItself() {
        final AircraftModel model = validModel();
        assertTrue(model.sameAs(model));
    }

    @Test
    void ensureModelSameAsNullIsFalse() {
        final AircraftModel model = validModel();
        assertFalse(model.sameAs(null));
    }

    @Test
    void ensureAircraftModelIdAccessorReturnsSameAsIdentity() {
        final AircraftModel model = validModel();
        assertEquals(model.identity(), model.aircraftModelId());
    }

    @Test
    void ensureRemoveEngineModelWorksWhenMultipleEnginesExist() {
        final AircraftModel model = validModel();
        final EngineModel engine1 = new EngineModel(
                "CFM56-A", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                new EngineModelMaker("CFM", "USA"),
                new FuelProperties(FuelType.JET_A, 0.8, 43.2));
        final EngineModel engine2 = new EngineModel(
                "CFM56-B", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                new EngineModelMaker("CFM", "USA"),
                new FuelProperties(FuelType.JET_A, 0.8, 43.2));
        model.addSupportedEngineModel(engine1);
        model.addSupportedEngineModel(engine2);
        model.removeEngineModel(engine1);
        assertEquals(1, model.supportedEngineModels().size());
    }

    @Test
    void ensureRemoveLastEngineModelIsRejected() {
        final AircraftModel model = validModel();
        final EngineModel engine = new EngineModel(
                "CFM56-C", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                new EngineModelMaker("CFM", "USA"),
                new FuelProperties(FuelType.JET_A, 0.8, 43.2));
        model.addSupportedEngineModel(engine);
        assertThrows(IllegalStateException.class, () -> model.removeEngineModel(engine));
    }

    @Test
    void ensureRemoveNonAssociatedEngineModelIsRejected() {
        final AircraftModel model = validModel();
        final EngineModel engine1 = new EngineModel(
                "CFM56-D", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                new EngineModelMaker("CFM", "USA"),
                new FuelProperties(FuelType.JET_A, 0.8, 43.2));
        final EngineModel engine2 = new EngineModel(
                "CFM56-E", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                new EngineModelMaker("CFM", "USA"),
                new FuelProperties(FuelType.JET_A, 0.8, 43.2));
        model.addSupportedEngineModel(engine1);
        model.addSupportedEngineModel(engine2);
        final EngineModel notAssociated = new EngineModel(
                "CFM56-F", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06,
                new EngineModelMaker("CFM", "USA"),
                new FuelProperties(FuelType.JET_A, 0.8, 43.2));
        assertThrows(IllegalArgumentException.class, () -> model.removeEngineModel(notAssociated));
    }
}
