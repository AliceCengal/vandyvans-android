package com.vandyapps.vandyvans.view

import android.app.Activity
import android.content.{Context, Intent}
import android.net.Uri
import android.os.Bundle
import android.widget.{Button, Toast}
import com.marsupial.eventhub.Helpers.EasyActivity
import com.vandyapps.vandyvans.R

class CodeAccessActivity extends Activity with EasyActivity {

  lazy val LINK_TO_REPOSITORY = getString(R.string.sourcecode_link)
  def openBrowser = component[Button](R.id.btn1)
  def emailLink = component[Button](R.id.btn2)

  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    setContentView(R.layout.source_code_access)
    openBrowser.onClick {
        startActivity(new Intent(Intent.ACTION_VIEW,
                                 Uri.parse(LINK_TO_REPOSITORY))) }

    emailLink.onClick {
        try {
          startActivity(Intent.createChooser(
            new Intent(Intent.ACTION_SEND)
              .setType("message/rfc822")
              .putExtra(Intent.EXTRA_SUBJECT, "Link to Vandy Vans source code")
              .putExtra(Intent.EXTRA_TEXT, LINK_TO_REPOSITORY),
            "Send mail..."))
        } catch {
          case e: android.content.ActivityNotFoundException =>
            Toast.makeText(this,
                           "There are no email clients installed",
                           Toast.LENGTH_SHORT)
                           .show()
        }}
  }

}

object CodeAccessActivity {
  def open(ctx: Context) {
    ctx.startActivity(new Intent(ctx, classOf[CodeAccessActivity]))
  }
}