package model;

/** Unit of sale, e.g. KG or DOZEN. */
public class Unit {

    private final String unitCode;
    private final String unitName;

    public Unit(String unitCode, String unitName) {
        this.unitCode = unitCode;
        this.unitName = unitName;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getUnitName() {
        return unitName;
    }

    @Override
    public String toString() {
        return unitName + " (" + unitCode + ")";
    }
}
