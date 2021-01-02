package awais.instagrabber.repositories.responses.directmessages;

public class DirectItemLocation {
    private final long pk;
    private final String shortName;
    private final String name;
    private final String address;
    private final String city;
    private final float lng;
    private final float lat;

    public DirectItemLocation(final long pk,
                              final String shortName,
                              final String name,
                              final String address,
                              final String city,
                              final float lng,
                              final float lat) {
        this.pk = pk;
        this.shortName = shortName;
        this.name = name;
        this.address = address;
        this.city = city;
        this.lng = lng;
        this.lat = lat;
    }

    public long getPk() {
        return pk;
    }

    public String getShortName() {
        return shortName;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public float getLng() {
        return lng;
    }

    public float getLat() {
        return lat;
    }
}
