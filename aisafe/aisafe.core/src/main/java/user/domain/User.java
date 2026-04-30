package user.domain;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.strings.util.StringPredicates;
import eapli.framework.validations.Preconditions;

@Entity
public class User implements AggregateRoot<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Long version;

    private String email;
    private String name;
    private String phoneNumber;
    private LocalDate activeSecurityClearanceExpiry;
    private LocalDate skillsAssessmentDueDate;
    private String status;

    public User(final String email, final String name, final String phoneNumber,
            final LocalDate activeSecurityClearanceExpiry, final LocalDate skillsAssessmentDueDate,
            final String status) {
        Preconditions.noneNull(email, name, phoneNumber, status);
        if (StringPredicates.isNullOrEmpty(email)) {
            throw new IllegalArgumentException("Email should neither be null nor empty");
        }
        if (StringPredicates.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Name should neither be null nor empty");
        }
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.activeSecurityClearanceExpiry = activeSecurityClearanceExpiry;
        this.skillsAssessmentDueDate = skillsAssessmentDueDate;
        this.status = status;
    }

    protected User() {
        // for ORM
    }

    @Override
    public Long identity() {
        return id;
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
}
