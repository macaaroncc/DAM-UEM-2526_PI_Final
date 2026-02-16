package com.example.pi2dam

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

class HelpActivity : AppCompatActivity() {

    private data class FaqItem(
        val card: MaterialCardView,
        val answer: View
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_help)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        AppMenu.bind(this)

        findViewById<ImageView>(R.id.btnAppbarHelp).setOnClickListener {
            Toast.makeText(this, "Ya estás en Ayuda", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnHelpResetPassword).setOnClickListener {
            startActivity(Intent(this, ResetPasswordActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnHelpOpenProducts).setOnClickListener {
            val current = FirebaseRefs.auth.currentUser
            if (current == null) {
                Toast.makeText(this, R.string.help_need_login, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, R.string.help_opening_products, Toast.LENGTH_SHORT).show()
            PiRepository.ensureEmployeeAccess(current.uid)
                .addOnSuccessListener { me ->
                    Session.setEmployee(me)
                    startActivity(Intent(this, ProductsActivity::class.java))
                }
                .addOnFailureListener {
                    Toast.makeText(this, R.string.help_not_authorized, Toast.LENGTH_SHORT).show()
                }
        }

        findViewById<MaterialButton>(R.id.btnHelpOpenMenu).setOnClickListener {
            AppMenuSheet.show(supportFragmentManager)
        }

        findViewById<MaterialButton>(R.id.btnHelpContact).setOnClickListener {
            val email = getString(R.string.help_support_email)
            val uri = Uri.parse("mailto:" + Uri.encode(email))
            val i = Intent(Intent.ACTION_SENDTO, uri)
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.help_support_subject))
                .putExtra(Intent.EXTRA_TEXT, getString(R.string.help_support_body))

            try {
                startActivity(Intent.createChooser(i, getString(R.string.help_contact_chooser)))
            } catch (_: ActivityNotFoundException) {
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("support_email", email))
                Toast.makeText(this, R.string.help_no_email_app, Toast.LENGTH_SHORT).show()
            }
        }

        val tvResults = findViewById<TextView>(R.id.tvHelpResults)

        fun toggle(v: View) {
            v.visibility = if (v.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val faqs = listOf(
            FaqItem(findViewById(R.id.cardFaqLogin), findViewById(R.id.tvFaqALogin)),
            FaqItem(findViewById(R.id.cardFaqReset), findViewById(R.id.tvFaqAReset)),
            FaqItem(findViewById(R.id.cardFaqProducts), findViewById(R.id.tvFaqAProducts)),
            FaqItem(findViewById(R.id.cardFaqOrders), findViewById(R.id.tvFaqAOrders)),
            FaqItem(findViewById(R.id.cardFaqUsers), findViewById(R.id.tvFaqAUsers)),
            FaqItem(findViewById(R.id.cardFaqMap), findViewById(R.id.tvFaqAMap))
        )

        faqs.forEach { item ->
            item.card.setOnClickListener { toggle(item.answer) }
        }

        fun applyFilter(raw: String) {
            val q = raw.trim().lowercase()
            var shown = 0
            faqs.forEach { item ->
                val hay = item.card.tag?.toString()?.lowercase().orEmpty()
                val show = q.isBlank() || hay.contains(q)
                item.card.visibility = if (show) View.VISIBLE else View.GONE
                if (!show) item.answer.visibility = View.GONE
                if (show) shown++
            }
            tvResults.text = getString(R.string.help_results, shown)
        }

        findViewById<TextInputEditText>(R.id.etSearchHelp).doAfterTextChanged {
            applyFilter(it?.toString().orEmpty())
        }

        applyFilter("")
    }
}
