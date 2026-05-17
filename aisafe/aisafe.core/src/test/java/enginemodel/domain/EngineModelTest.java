package enginemodel.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EngineModelTest {

    private static final EngineModelMaker MAKER = new EngineModelMaker("CFM", "USA");
    private static final FuelProperties FUEL = new FuelProperties(FuelType.JET_A, 0.8, 43.2);

    private EngineModel validEngine() {
        return new EngineModel(
                "CFM56", EngineType.TURBOFAN,
                100000.0, 0.35, 130000.0, 90000.0, 0.06,
                MAKER, FUEL);
    }

    // --- Construction: happy path ---

    @Test
    void ensureEngineModelIsCreatedWithExpectedState() {
        final EngineModel engine = validEngine();
        assertAll(
                () -> assertEquals("CFM56", engine.name()),
                () -> assertEquals(EngineType.TURBOFAN, engine.type()),
                () -> assertEquals(100000.0, engine.power()),
                () -> assertEquals(0.35, engine.efficiency()),
                () -> assertEquals(130000.0, engine.thrustSeaLevelStopped()),
                () -> assertEquals(90000.0, engine.thrustSeaLevelCruise()),
                () -> assertEquals(0.06, engine.thrustSpecificFuelConsumption()),
                () -> assertEquals(MAKER, engine.maker()),
                () -> assertEquals(FUEL, engine.fuelProperties()),
                () -> assertNotNull(engine.identity())
        );
    }

    // --- Construction: invalid arguments ---

    @Test
    void ensureNullNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new EngineModel(null, EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06, MAKER, FUEL));
    }

    @Test
    void ensureEmptyNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new EngineModel("", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06, MAKER, FUEL));
    }

    @Test
    void ensureNullTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new EngineModel("CFM56", null, 100000.0, 0.35, 130000.0, 90000.0, 0.06, MAKER, FUEL));
    }

    @Test
    void ensureZeroPowerIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new EngineModel("CFM56", EngineType.TURBOFAN, 0.0, 0.35, 130000.0, 90000.0, 0.06, MAKER, FUEL));
    }

    @Test
    void ensureNegativePowerIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new EngineModel("CFM56", EngineType.TURBOFAN, -1.0, 0.35, 130000.0, 90000.0, 0.06, MAKER, FUEL));
    }

    @Test
    void ensureZeroEfficiencyIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new EngineModel("CFM56", EngineType.TURBOFAN, 100000.0, 0.0, 130000.0, 90000.0, 0.06, MAKER, FUEL));
    }

    @Test
    void ensureNullMakerIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new EngineModel("CFM56", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06, null, FUEL));
    }

    @Test
    void ensureNullFuelPropertiesIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new EngineModel("CFM56", EngineType.TURBOFAN, 100000.0, 0.35, 130000.0, 90000.0, 0.06, MAKER, null));
    }

    // --- equals / hashCode ---

    @Test
    void ensureEngineModelIsEqualToItself() {
        final EngineModel engine = validEngine();
        assertEquals(engine, engine);
        assertEquals(engine.hashCode(), engine.hashCode());
    }

    @Test
    void ensureEngineModelIsNotEqualToNull() {
        assertNotEquals(validEngine(), null);
    }

    @Test
    void ensureTwoDifferentEngineModelsAreNotEqual() {
        final EngineModel e1 = validEngine();
        final EngineModel e2 = validEngine();
        assertNotEquals(e1, e2);
    }

    @Test
    void ensureEngineModelSameAsItself() {
        final EngineModel engine = validEngine();
        assertTrue(engine.sameAs(engine));
    }

    @Test
    void ensureEngineModelSameAsNullIsFalse() {
        final EngineModel engine = validEngine();
        assertFalse(engine.sameAs(null));
    }

    @Test
    void ensureEngineModelIdAccessorReturnsSameAsIdentity() {
        final EngineModel engine = validEngine();
        assertEquals(engine.identity(), engine.engineModelId());
    }
}
