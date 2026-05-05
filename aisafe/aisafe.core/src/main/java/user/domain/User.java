package user.domain;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.JoinColumn;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.validations.Preconditions;
import shared.domain.UserId;

@Entity
public class User implements AggregateRoot<UserId> {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private String email;
    private String name;
    private String phoneNumber;
    private LocalDate activeSecurityClearanceExpiry;
    private LocalDate skillsAssessmentDueDate;
    private String status;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    private Set<Role> roles = new HashSet<>();

    @EmbeddedId
    private UserId userId;

    public User(final String email, final String name, final String phoneNumber,
            final LocalDate activeSecurityClearanceExpiry, final LocalDate skillsAssessmentDueDate,
            final String status, final Set<Role> roles, final UserId userId) {
        Preconditions.noneNull(email, name, phoneNumber, status, roles, userId);
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.activeSecurityClearanceExpiry = activeSecurityClearanceExpiry;
        this.skillsAssessmentDueDate = skillsAssessmentDueDate;
        this.status = status;
        this.roles.addAll(roles);
        this.userId = userId;
    }

    protected User() {
        // for ORM
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public UserId identity() {
        return userId;
    }

    public String email() {
        return email;
    }

    public String name() {
        return name;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public LocalDate activeSecurityClearanceExpiry() {
        return activeSecurityClearanceExpiry;
    }

    public LocalDate skillsAssessmentDueDate() {
        return skillsAssessmentDueDate;
    }

    public String status() {
        return status;
    }

    public void enable() {
        this.status = STATUS_ACTIVE;
    }

    public void disable() {
        this.status = STATUS_DISABLED;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public Set<Role> roles() {
        return Set.copyOf(roles);
    }

    public UserId userId() {
        return userId;
    }
}
