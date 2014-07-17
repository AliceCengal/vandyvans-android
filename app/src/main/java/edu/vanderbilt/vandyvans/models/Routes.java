package edu.vanderbilt.vandyvans.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Hold static references to some premade Route objects. The Route objects
 * are fully immutable, and are thread-safe.
 * Created by athran on 3/15/14.
 */
public final class Routes {

    public static final Route BLUE  = new Route(745, "Blue");
    public static final Route RED   = new Route(746, "Red");
    public static final Route GREEN = new Route(749, "Green");

    public static Route getForId(int id) {
        switch (id) {
            case 745: return BLUE;
            case 746: return RED;
            case 749: return GREEN;
            default: throw new IllegalArgumentException(
                    "No route with that Id exists.");
        }
    }

    public static List<Route> getAll() {
        return Collections.unmodifiableList(Arrays.asList(BLUE, RED, GREEN));
    }

}
