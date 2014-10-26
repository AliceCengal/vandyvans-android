package com.vandyapps.vandyvans.services

import android.content.Context
import com.parse.{ParseObject, Parse}
import com.vandyapps.vandyvans.models.Report

/**
 * Sends user feedback to the server.
 *
 * Created by athran on 10/26/14.
 */
private[services] object ParseClient {
  private val REPORT_CLASSNAME = "VVReport"
  private val REPORT_USEREMAIL = "userEmail"
  private val REPORT_BODY      = "body"
  private val REPORT_ISBUG     = "isBugReport"
  private val REPORT_NOTIFY    = "notifyWhenResolved"
}

private[services] class ParseClient(ctx: Context) {
  import ParseClient._

  Parse.initialize(ctx,
    "6XOkxBODp8HZANJaxFhEfSFPZ8H93Pt9531Htt1X",
    "61wOewMMN0YISmX3UM79PGssnTsz1NfkOOMOsHMm")

  def postReportUsingParseApi(report: Report) {
    val reportObj = new ParseObject(REPORT_CLASSNAME)
    reportObj.put(REPORT_USEREMAIL, report.senderAddress)
    reportObj.put(REPORT_BODY     , report.bodyOfReport)
    reportObj.put(REPORT_ISBUG    , report.isBugReport)
    reportObj.put(REPORT_NOTIFY   , report.notifyWhenResolved)
    reportObj.save()
  }

}