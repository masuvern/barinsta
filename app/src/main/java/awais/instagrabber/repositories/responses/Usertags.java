package awais.instagrabber.repositories.responses;

import java.io.Serializable;
import java.util.List;

public class Usertags implements Serializable {
    private final List<UsertagIn> in;

    public Usertags(final List<UsertagIn> in) {
        this.in = in;
    }

    public List<UsertagIn> getIn() {
        return in;
    }
}
