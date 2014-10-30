package com.vandyapps.vandyvans.models

/** A user feedback report. This class is a union of BugReport and Comment,
  * combined for convenience. If splitting the two roles makes more sense, feel free to do so.
  *
  * @param isBugReport Is this a bug report or just feedback?
  * @param senderAddress Email address of the report-maker
  * @param bodyOfReport What the user has to say
  * @param notifyWhenResolved Should we notify the user when the topic of the report is resolved
  */
case class Report(isBugReport: Boolean,
                  senderAddress: String,
                  bodyOfReport: String,
                  notifyWhenResolved: Boolean)
