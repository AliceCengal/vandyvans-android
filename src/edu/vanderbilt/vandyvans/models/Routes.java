package edu.vanderbilt.vandyvans.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * Created by athran on 3/15/14.
 */
public final class Routes {

    public static final Route BLUE  = new Route(745, "Blue",  RouteColor.BLUE);
    public static final Route RED   = new Route(746, "Red",   RouteColor.RED);
    public static final Route GREEN = new Route(749, "Green", RouteColor.GREEN);

    public static List<Route> getAll() {
        return Collections.unmodifiableList(Arrays.asList(BLUE, RED, GREEN));
    }

    public enum RouteColor {
        BLUE  ("#0000ff"),
        RED   ("#ff0000"),
        GREEN ("#00ff00");

        public final String colorCode;

        RouteColor(String _colorCode) {
            colorCode = _colorCode;
        }
    }

}