package bootstrap;

import aircraftmodel.domain.AircraftModel;
import aircraftmodel.domain.AircraftModelMaker;
import aircraftmodel.domain.AircraftType;
import aircraftmodel.repositories.AircraftModelRepository;
import enginemodel.domain.EngineModel;
import enginemodel.repositories.EngineModelRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Optional;

public class AircraftModelBootstrap {

    private final AircraftModelRepository aircraftModelRepository;
    private final EngineModelRepository engineModelRepository;

    public AircraftModelBootstrap(final AircraftModelRepository aircraftModelRepository,
            final EngineModelRepository engineModelRepository) {
        this.aircraftModelRepository = aircraftModelRepository;
        this.engineModelRepository = engineModelRepository;
    }

    public void execute() {
        bootstrapFromXml("aisafe.app.bootstrap/TestSet00_AircraftModels.xml");
    }

    private void bootstrapFromXml(final String filePath) {
        try {
            final File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("XML file not found: " + filePath);
                return;
            }

            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            final NodeList aircraftNodes = doc.getElementsByTagName("aircraft");
            for (int i = 0; i < aircraftNodes.getLength(); i++) {
                final Element aircraftElement = (Element) aircraftNodes.item(i);
                final String modelName = aircraftElement.getAttribute("model_ID");
                final String makerName = aircraftElement.getElementsByTagName("maker").item(0).getTextContent();
                final AircraftType type = AircraftType
                        .valueOf(aircraftElement.getElementsByTagName("type").item(0).getTextContent().toUpperCase());

                final double emptyWeight = parseValueWithUnit(aircraftElement, "EWeight");
                final double maximumTakeOffWeight = parseValueWithUnit(aircraftElement, "MTOW");
                final double maxPayload = parseValueWithUnit(aircraftElement, "max_payload");
                final double maximumZeroFuelWeight = emptyWeight + maxPayload;
                final double maximumFuelCapacity = parseValueWithUnit(aircraftElement, "fuel_capacity");

                final Element motorization = (Element) aircraftElement.getElementsByTagName("motorization").item(0);
                final int numberOfEngines = Integer
                        .parseInt(motorization.getElementsByTagName("number_motors").item(0).getTextContent());
                final double serviceCeiling = parseValueWithUnit(motorization, "cruise_altitude");
                final double cruiseSpeed = parseValueWithUnit(motorization, "cruise_speed");

                final double wingArea = parseValueWithUnit(aircraftElement, "wing_area");
                final double wingSpan = parseValueWithUnit(aircraftElement, "wing_span");

                final Element dragFunc = (Element) aircraftElement.getElementsByTagName("Cdrag_function").item(0);
                final Element firstItem = (Element) dragFunc.getElementsByTagName("iten").item(0);
                final double dragCoefficient = Double
                        .parseDouble(firstItem.getElementsByTagName("Cdrag_0").item(0).getTextContent());
                final double liftCoefficient = 1.2; // Default if not in XML

                final String engineName = motorization.getElementsByTagName("motor").item(0).getTextContent();

                registerFromXml(modelName, makerName, type, emptyWeight, maximumTakeOffWeight, maximumZeroFuelWeight,
                        maximumFuelCapacity, serviceCeiling, cruiseSpeed, wingArea, wingSpan, dragCoefficient,
                        liftCoefficient, numberOfEngines, engineName);
            }
        } catch (final Exception e) {
            System.err.println("Error bootstrapping aircraft models from XML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private double parseValueWithUnit(final Element parent, final String tagName) {
        final String text = parent.getElementsByTagName(tagName).item(0).getTextContent().trim();
        final String[] parts = text.split(" ");
        double value = Double.parseDouble(parts[0]);
        if (parts.length > 1) {
            final String unit = parts[1];
            switch (unit) {
                case "US":
                    // If weight/mass, lbs to kg; if altitude, ft to m.
                    // We distinguish by tag name or context.
                    if (tagName.contains("Weight") || tagName.contains("MTOW") || tagName.contains("payload")
                            || tagName.contains("capacity")) {
                        value *= 0.45359237;
                    } else if (tagName.contains("altitude") || tagName.contains("Ceiling")) {
                        value *= 0.3048;
                    }
                    break;
                case "Knot":
                    value *= 0.514444;
                    break;
                case "M":
                    // Mach to m/s (using 295 m/s as estimate for high altitude)
                    value *= 295.0;
                    break;
            }
        }
        return value;
    }

    private void registerFromXml(final String modelName, final String makerName, final AircraftType type,
            final double emptyWeight, final double mtow, final double mzfw,
            final double fuelCapacity, final double ceiling, final double speed,
            final double area, final double span, final double drag,
            final double lift, final int engines, final String engineName) {

        final var existing = aircraftModelRepository.findByModelNameAndMaker(modelName, makerName);
        if (!existing.isEmpty()) {
            final AircraftModel existingModel = existing.get(0);
            if (existingModel.supportedEngineModels().isEmpty()) {
                engineModelRepository.findByName(engineName).ifPresent(existingModel::addSupportedEngineModel);
                aircraftModelRepository.save(existingModel);
                System.out.println("Engine association repaired for existing model: " + modelName);
            }
            return;
        }

        final var model = new AircraftModel(
                type, emptyWeight, mtow, mzfw, fuelCapacity, ceiling, speed, area, span, drag, lift, modelName,
                engines, new AircraftModelMaker(makerName, "Unknown"));

        // Try to find the engine by name (normalizing spaces)
        final String normalizedEngine = engineName.replace(" ", "");
        final Optional<EngineModel> engine = engineModelRepository.findByName(normalizedEngine);
        if (engine.isPresent()) {
            model.addSupportedEngineModel(engine.get());
        } else {
            // Fallback: try direct name
            engineModelRepository.findByName(engineName).ifPresent(model::addSupportedEngineModel);
        }

        aircraftModelRepository.save(model);
        System.out.println("Aircraft model bootstrapped from XML: " + modelName);
    }

}