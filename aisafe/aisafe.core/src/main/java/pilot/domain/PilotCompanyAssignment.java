package pilot.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import shared.domain.AirTransportCompanyId;
import shared.domain.UserId;

@Entity
public class PilotCompanyAssignment implements AggregateRoot<UserId> {

    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "pilot_user_id"))
    private UserId pilotUserId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "company_id"))
    private AirTransportCompanyId companyId;

    public PilotCompanyAssignment(final UserId pilotUserId, final AirTransportCompanyId companyId) {
        Preconditions.noneNull(pilotUserId, companyId);
        this.pilotUserId = pilotUserId;
        this.companyId   = companyId;
    }

    protected PilotCompanyAssignment() {
        // for ORM
    }

    @Override
    public UserId identity() {
        return pilotUserId;
    }

    public UserId pilotUserId() {
        return pilotUserId;
    }

    public AirTransportCompanyId companyId() {
        return companyId;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }
}
