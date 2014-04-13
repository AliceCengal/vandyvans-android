package edu.vanderbilt.vandyvans;

/**
 * The interface exposed by the services package to allow the UI to
 * query and update the state of the ReminderSystem. Use `@Inject` to get
 * an object that implements this interface.
 *
 * See `Global::initializeGlobalState()` for more on the injection.
 * See: `/docs/architecture_for_the_reminder_system.jpg`
 *
 * Created by athran on 4/7/14.
 */
public interface ReminderController {
    void    subscribeReminderForStop(int stopId);
    void    unsubscribeReminderForStop(int stopId);
    boolean isSubscribedToStop(int stopId);
}
