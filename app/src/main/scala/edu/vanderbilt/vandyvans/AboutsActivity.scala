package edu.vanderbilt.vandyvans

import android.app.Activity
import android.content.{Context, Intent}
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.marsupial.eventhub.{ActorConversion, AppInjection}
import com.marsupial.eventhub.Helpers.EasyActivity
import edu.vanderbilt.vandyvans.models.Report
import edu.vanderbilt.vandyvans.services.{Global, Clients}

class AboutsActivity extends Activity
    with EasyActivity
    with AppInjection[Global]
    with ActorConversion
{
  val TAG_FORMTYPE = "formtype"
  val TAG_BUG = 1000
  val TAG_FEED = 1111

  def bugReport = component[Button](R.id.button1)
  def feedbackReport = component[Button](R.id.button2)
  def sourceCode = component[Button](R.id.button6)

  val clients: Clients = app

  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    setContentView(R.layout.activity_about)

    bugReport.setOnClickListener { v: View =>
      val i = new Intent(AboutsActivity.this, classOf[FormActivity])
      i.putExtra(TAG_FORMTYPE, TAG_BUG)
      i.putExtra(FormActivity.TAG_FORMTITLE, "Report a Bug")
      i.putExtra(FormActivity.TAG_FORMBODYHINT, "describe the bug")
      startActivityForResult(i, 1)
    }

    feedbackReport.setOnClickListener { v: View =>
      val i = new Intent(AboutsActivity.this, classOf[FormActivity])
      i.putExtra(TAG_FORMTYPE, TAG_FEED)
      i.putExtra(FormActivity.TAG_FORMTITLE, "Send Feedback")
      i.putExtra(FormActivity.TAG_FORMBODYHINT, "thoughts on the app")
      startActivityForResult(i, 1)
    }

    sourceCode.setOnClickListener { v: View =>
      CodeAccessActivity.open(AboutsActivity.this)
    }

  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    if (resultCode != Activity.RESULT_CANCELED) {
      clients.vandyVans ! new Report(
          data.getIntExtra(TAG_FORMTYPE, TAG_FEED) == TAG_BUG,
          data.getStringExtra(FormActivity.RESULT_EMAIL),
          data.getStringExtra(FormActivity.RESULT_BODY),
          false)
    }
  }

}

object AboutsActivity {
  def open(ctx: Context) {
    ctx.startActivity(new Intent(ctx, classOf[AboutsActivity]))
  }
}