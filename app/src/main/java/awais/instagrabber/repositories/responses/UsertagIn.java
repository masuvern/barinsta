package awais.instagrabber.repositories.responses;

import java.io.Serializable;
import java.util.List;

public class UsertagIn implements Serializable {
    private final User user;
    private final List<String> position;

    public UsertagIn(final User user, final List<String> position) {
        this.user = user;
        this.position = position;
    }

    public User getUser() {
        return user;
    }

    public List<String> getPosition() {
        return position;
    }
}
