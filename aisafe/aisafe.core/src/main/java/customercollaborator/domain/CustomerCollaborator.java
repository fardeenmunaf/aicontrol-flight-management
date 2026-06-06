package customercollaborator.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Version;
import shared.domain.AirControlAreaId;
import shared.domain.AirTransportCompanyId;
import shared.domain.CustomerCollaboratorId;
import shared.domain.UserId;

@Entity
public class CustomerCollaborator implements AggregateRoot<CustomerCollaboratorId> {

    @Version
    private Long version;

    @EmbeddedId
    private CustomerCollaboratorId id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "user_id"))
    private UserId userId;

    @Enumerated(EnumType.STRING)
    private CustomerType customerType;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "air_transport_company_id"))
    private AirTransportCompanyId airTransportCompanyId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "air_control_area_id"))
    private AirControlAreaId airControlAreaId;

    private String position;

    private boolean active;

    protected CustomerCollaborator() {
    }

    public CustomerCollaborator(final UserId userId,
            final CustomerType customerType,
            final AirTransportCompanyId airTransportCompanyId,
            final AirControlAreaId airControlAreaId,
            final String position) {
        Preconditions.noneNull(userId, customerType);
        if (position == null || position.trim().isEmpty()) {
            throw new IllegalArgumentException("Position cannot be null or empty");
        }

        this.id = CustomerCollaboratorId.newId();
        this.userId = userId;
        this.customerType = customerType;
        this.airTransportCompanyId = airTransportCompanyId;
        this.airControlAreaId = airControlAreaId;
        this.position = position.trim();
        this.active = true;

        validateInvariants();
    }

    private void validateInvariants() {
        if (customerType == CustomerType.AIR_TRANSPORT_COMPANY) {
            Preconditions.nonNull(airTransportCompanyId, "Air transport company id must be specified");
            if (airControlAreaId != null) {
                throw new IllegalArgumentException("Air control area id must not be specified");
            }
        }

        if (customerType == CustomerType.AIR_CONTROL_AREA) {
            Preconditions.nonNull(airControlAreaId, "Air control area id must be specified");
            if (airTransportCompanyId != null) {
                throw new IllegalArgumentException("Air transport company id must not be specified");
            }
        }
    }

    public UserId userId() {
        return userId;
    }

    public CustomerType customerType() {
        return customerType;
    }

    public AirTransportCompanyId airTransportCompanyId() {
        return airTransportCompanyId;
    }

    public AirControlAreaId airControlAreaId() {
        return airControlAreaId;
    }

    public String position() {
        return position;
    }

    public boolean isActive() {
        return active;
    }

    public void disable() {
        if (!active) {
            throw new IllegalStateException("Customer collaborator is already disabled");
        }
        this.active = false;
    }

    @Override
    public CustomerCollaboratorId identity() {
        return id;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }
}