package edu.vanderbilt.vandyvans.models

case class Report(isBugReport: Boolean,
                  senderAddress: String,
                  bodyOfReport: String,
                  notifyWhenResolved: Boolean)