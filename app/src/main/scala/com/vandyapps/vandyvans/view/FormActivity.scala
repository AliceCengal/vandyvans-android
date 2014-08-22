package com.vandyapps.vandyvans.view

import android.app.Activity
import android.os.Bundle
import android.widget.{Button, EditText, TextView, Toast}
import com.marsupial.eventhub.Helpers.EasyActivity
import com.vandyapps.vandyvans.R

class FormActivity extends Activity with EasyActivity {
  import com.vandyapps.vandyvans.view.FormActivity._

  def formTitle = component[TextView](R.id.textView1)
  def emailField = component[EditText](R.id.editText)
  def bodyField = component[EditText](R.id.editText2)
  def submit = component[Button](R.id.button3)

  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    setContentView(R.layout.report_form)

    val conf = readConfig(getIntent.getExtras)
    formTitle.setText(conf.title)
    bodyField.setText(conf.hint)

    setResult(Activity.RESULT_CANCELED)

    submit.onClick {
      val email = emailField.getText.toString
      val body = bodyField.getText.toString

      if (email == null || email.isEmpty) {
        showMessage("Please fill in your email address")
      } else if (body == null || body.isEmpty) {
        showMessage("Please fill in the description")
      } else {
        setResult(RESULT_EXIST,
                  getIntent.putExtra(RESULT_EMAIL, email)
                           .putExtra(RESULT_BODY, body))
        FormActivity.this.finish()
      }

    }
  }

  def showMessage(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
  }

}

object FormActivity {
  val TAG_FORMTITLE     = "form_title"
  val TAG_FORMBODYHINT = "form_body_hint"
  val RESULT_EMAIL      = "result_email"
  val RESULT_BODY       = "result_body"
  val RESULT_EXIST      = 9090

  case class FormConfig(title: String, hint: String)

  def readConfig(args: Bundle) =
    FormConfig(title = args.getString(TAG_FORMTITLE),
               hint  = args.getString(TAG_FORMBODYHINT))

}
