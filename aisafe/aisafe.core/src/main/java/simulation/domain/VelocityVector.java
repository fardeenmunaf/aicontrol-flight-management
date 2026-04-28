package simulation.domain;

import jakarta.persistence.Embeddable;

import eapli.framework.domain.model.ValueObject;

@Embeddable
public class VelocityVector implements ValueObject {

    private static final long serialVersionUID = 1L;

    private Double speed;
    private Double direction;

    public VelocityVector(final Double speed, final Double direction) {
        if (speed == null || direction == null) {
            throw new IllegalArgumentException("Speed and direction are required");
        }
        if (speed < 0) {
            throw new IllegalArgumentException("Speed must be non-negative");
        }
        if (direction < 0 || direction > 360) {
            throw new IllegalArgumentException("Direction must be between 0 and 360 degrees");
        }
        this.speed = speed;
        this.direction = direction;
    }

    protected VelocityVector() {
        // for ORM
    }

    public static VelocityVector valueOf(final Double speed, final Double direction) {
        return new VelocityVector(speed, direction);
    }

    public Double speed() {
        return speed;
    }

    public Double direction() {
        return direction;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VelocityVector)) {
            return false;
        }

        final VelocityVector other = (VelocityVector) o;
        return speed.equals(other.speed) && direction.equals(other.direction);
    }

    @Override
    public int hashCode() {
        int result = speed.hashCode();
        result = 31 * result + direction.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "VelocityVector{speed=" + speed + ", direction=" + direction + "}";
    }
}
