package user.domain;

import java.time.LocalDate;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import eapli.framework.domain.model.DomainEntities;
import eapli.framework.domain.model.DomainEntity;
import eapli.framework.validations.Preconditions;
import shared.domain.UserId;

@Entity
public class User implements DomainEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String email;
    private String name;
    private String phoneNumber;
    private LocalDate activeSecurityClearanceExpiry;
    private LocalDate skillsAssessmentDueDate;
    private String status;

    @Embedded
    private UserId userId;

    public User(final String email, final String name, final String phoneNumber,
            final LocalDate activeSecurityClearanceExpiry, final LocalDate skillsAssessmentDueDate,
            final String status, final UserId userId) {
        Preconditions.noneNull(email, name, phoneNumber, status, userId);
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.activeSecurityClearanceExpiry = activeSecurityClearanceExpiry;
        this.skillsAssessmentDueDate = skillsAssessmentDueDate;
        this.status = status;
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
    public Long identity() {
        return id;
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

    public UserId userId() {
        return userId;
    }
}
