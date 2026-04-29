package enginemodel.domain;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.strings.util.StringPredicates;

@Embeddable
public class EngineModelMaker implements ValueObject {

    private static final long serialVersionUID = 1L;

    private String name;
    private String country;

    public EngineModelMaker(final String name, final String country) {
        if (StringPredicates.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Maker name should neither be null nor empty");
        }
        if (StringPredicates.isNullOrEmpty(country)) {
            throw new IllegalArgumentException("Maker country should neither be null nor empty");
        }

        this.name = name;
        this.country = country;
    }

    protected EngineModelMaker() {
        // for ORM
    }

    public String name() {
        return name;
    }

    public String country() {
        return country;
    }
}