package model;

/** A farm that currently has products on sale (used by the buyer's farm filter). */
public record FarmSummary(int farmerId, String farmName) {

    @Override
    public String toString() {
        return farmName;
    }
}
