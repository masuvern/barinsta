package awais.instagrabber.repositories.responses.search;

import awais.instagrabber.repositories.responses.Location;

public class Place {
    private final Location location;
    private final String title; // those are repeated within location
    private final String subtitle; // address
    private final String slug; // browser only; for end of address

    public Place(final Location location,
                 final String title,
                 final String subtitle,
                 final String slug) {
        this.location = location;
        this.title = title;
        this.subtitle = subtitle;
        this.slug = slug;
    }

    public Location getLocation() {
        return location;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getSlug() {
        return slug;
    }
}
