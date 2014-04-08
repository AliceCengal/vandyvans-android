package edu.vanderbilt.vandyvans;

/**
 * Created by athran on 4/7/14.
 */
public interface ReminderController {
    void    subscribeReminderForStop(int stopId);
    void    unsubscribeReminderForStop(int stopId);
    boolean isSubscribedToStop(int stopId);
}
