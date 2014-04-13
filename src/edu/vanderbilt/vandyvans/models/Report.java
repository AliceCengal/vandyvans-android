package edu.vanderbilt.vandyvans.models;

/**
 * Data object that represents a bug report or a feedback from the user.
 *
 * All class in the model package are fully immutable and thread safe.
 *
 * Created by athran on 3/16/14.
 */
public final class Report {

    public final boolean isBugReport;
    public final String  senderAddress;
    public final String  bodyOfReport;
    public final boolean notifyWhenResolved;

    public Report(boolean _isBugReport,
                  String  _senderAddress,
                  String  _body,
                  boolean _notify) {
        isBugReport        = _isBugReport;
        senderAddress      = _senderAddress;
        bodyOfReport       = _body;
        notifyWhenResolved = _notify;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Report(")
                .append(isBugReport)
                .append(",")
                .append(senderAddress)
                .append(",")
                .append(bodyOfReport)
                .append(",")
                .append(notifyWhenResolved)
                .append(")")
                .toString();
    }

}
