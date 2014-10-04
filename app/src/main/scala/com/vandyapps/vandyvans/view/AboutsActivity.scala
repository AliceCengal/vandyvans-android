package com.vandyapps.vandyvans.view

import android.app.Activity
import android.content.{Context, Intent}
import android.os.Bundle
import android.widget.Button
import com.cengallut.asyncactivity.AsyncActivity
import com.marsupial.eventhub.Helpers.EasyActivity
import com.marsupial.eventhub.AppInjection
import com.vandyapps.vandyvans.R
import com.vandyapps.vandyvans.models.Report
import com.vandyapps.vandyvans.services.Global

class AboutsActivity extends Activity
    with EasyActivity
    with AppInjection[Global]
    with AsyncActivity
{
  val TAG_FORMTYPE = "formtype"
  val TAG_BUG = 1000
  val TAG_FEED = 1111

  def bugReport = component[Button](R.id.button1)
  def feedbackReport = component[Button](R.id.button2)
  def sourceCode = component[Button](R.id.button6)

  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    setContentView(R.layout.activity_about)

    bugReport.onClick {
      val i = new Intent(AboutsActivity.this, classOf[FormActivity])
      i.putExtra(TAG_FORMTYPE, TAG_BUG)
      i.putExtra(FormActivity.TAG_FORMTITLE, "Report a Bug")
      i.putExtra(FormActivity.TAG_FORMBODYHINT, "describe the bug")
      startActivityForResult(i, 1)
    }

    feedbackReport.onClick {
      val i = new Intent(AboutsActivity.this, classOf[FormActivity])
      i.putExtra(TAG_FORMTYPE, TAG_FEED)
      i.putExtra(FormActivity.TAG_FORMTITLE, "Send Feedback")
      i.putExtra(FormActivity.TAG_FORMBODYHINT, "thoughts on the app")
      startActivityForResult(i, 1)
    }

    sourceCode.onClick {
      CodeAccessActivity.open(AboutsActivity.this)
    }

  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    if (resultCode != Activity.RESULT_CANCELED) {
      app.services.postReport(
        new Report(
          isBugReport = data.getIntExtra(TAG_FORMTYPE, TAG_FEED) == TAG_BUG,
          senderAddress = data.getStringExtra(FormActivity.RESULT_EMAIL),
          bodyOfReport = data.getStringExtra(FormActivity.RESULT_BODY),
          notifyWhenResolved = false))
    }
  }

}

object AboutsActivity {
  def open(ctx: Context) {
    ctx.startActivity(new Intent(ctx, classOf[AboutsActivity]))
  }
}