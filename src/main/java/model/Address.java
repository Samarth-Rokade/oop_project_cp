package model;

public class Address {

    private int addressId;
    private int userId;
    private String addressLine;
    private String city;
    private String district;
    private String state;
    private String postalCode;
    private boolean defaultAddress;

    public Address() {
    }

    public Address(int addressId, int userId, String addressLine, String city, String district,
                   String state, String postalCode, boolean defaultAddress) {
        this.addressId = addressId;
        this.userId = userId;
        this.addressLine = addressLine;
        this.city = city;
        this.district = district;
        this.state = state;
        this.postalCode = postalCode;
        this.defaultAddress = defaultAddress;
    }

    public int getAddressId() {
        return addressId;
    }

    public void setAddressId(int addressId) {
        this.addressId = addressId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }

    /** One line, used in combo boxes and order lists. */
    @Override
    public String toString() {
        return addressLine + ", " + city + " - " + postalCode + (defaultAddress ? "  (default)" : "");
    }
}
