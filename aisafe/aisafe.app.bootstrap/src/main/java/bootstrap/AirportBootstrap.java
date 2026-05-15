
package bootstrap;

import aircontrolarea.domain.AirControlArea;
import aircontrolarea.repositories.AirControlAreaRepository;
import airport.domain.Airport;
import airport.domain.AirportCoordinates;
import airport.repositories.AirportRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.List;
import java.util.Optional;

public class AirportBootstrap {

    private final AirportRepository airportRepository;
    private final AirControlAreaRepository airControlAreaRepository;

    public AirportBootstrap(
            final AirportRepository airportRepository,
            final AirControlAreaRepository airControlAreaRepository) {
        this.airportRepository = airportRepository;
        this.airControlAreaRepository = airControlAreaRepository;
    }

    public void execute() {
        bootstrapFromXml("aisafe.app.bootstrap/TestSet00_Airports.xml");
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

            final NodeList airportNodes = doc.getElementsByTagName("airport");
            for (int i = 0; i < airportNodes.getLength(); i++) {
                final Element airportElement = (Element) airportNodes.item(i);
                final String iataCode = airportElement.getAttribute("id");
                final String name = airportElement.getElementsByTagName("name").item(0).getTextContent();
                final String town = airportElement.getElementsByTagName("town").item(0).getTextContent();
                final String country = airportElement.getElementsByTagName("country").item(0).getTextContent();

                final Element locationElement = (Element) airportElement.getElementsByTagName("location").item(0);
                final double latitude = Double.parseDouble(locationElement.getElementsByTagName("latitude").item(0).getTextContent());
                final double longitude = Double.parseDouble(locationElement.getElementsByTagName("longitude").item(0).getTextContent());
                
                final String altitudeText = locationElement.getElementsByTagName("altitude").item(0).getTextContent();
                final double elevation = Double.parseDouble(altitudeText.split(" ")[0]);

                final String icaoCode = "X" + iataCode; // Placeholder ICAO

                registerFromXml(name, town, country, iataCode, icaoCode, elevation, latitude, longitude);
            }
        } catch (final Exception e) {
            System.err.println("Error bootstrapping airports from XML: " + e.getMessage());
        }
    }

    private void registerFromXml(
            final String name,
            final String town,
            final String country,
            final String iataCode,
            final String icaoCode,
            final Double elevation,
            final Double latitude,
            final Double longitude) {

        if (airportRepository.findByIataCode(iataCode).isPresent()) {
            return;
        }

        final Optional<AirControlArea> area = airControlAreaRepository.findContaining(latitude, longitude);
        if (area.isEmpty()) {
            System.out.println("No air control area found for airport: " + iataCode + " at (" + latitude + ", " + longitude + ")");
            return;
        }

        final Airport airport = new Airport(
                name, town, country, iataCode, icaoCode, elevation,
                new AirportCoordinates(latitude, longitude), area.get());

        airportRepository.save(airport);
        System.out.println("Airport bootstrapped from XML: " + iataCode + " (" + icaoCode + ") in area " + area.get().areaCode());
    }

}
