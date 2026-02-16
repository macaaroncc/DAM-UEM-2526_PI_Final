package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.pi2dam.model.EmployeeProfile.Companion.ROLE_ADMIN
import com.google.android.material.button.MaterialButton

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        AppMenu.bind(this)

        findViewById<View>(R.id.btnHelp).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnMenuHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            finish()
        }

        val btnUsers = findViewById<MaterialButton>(R.id.btnMenuUsers)
        val btnProducts = findViewById<MaterialButton>(R.id.btnMenuProducts)
        val btnOrders = findViewById<MaterialButton>(R.id.btnMenuOrders)

        btnUsers.setOnClickListener { startActivity(Intent(this, UsersActivity::class.java)) }
        btnProducts.setOnClickListener { startActivity(Intent(this, ProductsActivity::class.java)) }
        btnOrders.setOnClickListener { startActivity(Intent(this, OrdersActivity::class.java)) }

        // Si no tenemos sesión aún, se ajustará en onStart.
        btnUsers.visibility = if (Session.employee?.role == ROLE_ADMIN) View.VISIBLE else View.GONE

        findViewById<MaterialButton>(R.id.btnMenuPayment).setOnClickListener {
            startActivity(Intent(this, PaymentActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnMenuWarehousesMap).setOnClickListener {
            startActivity(Intent(this, WarehousesMapActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnMenuHelp).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnMenuLogout).setOnClickListener {
            FirebaseRefs.auth.signOut()
            Session.clear()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        val current = FirebaseRefs.auth.currentUser ?: return

        PiRepository.ensureEmployeeAccess(current.uid)
            .addOnSuccessListener { me ->
                Session.setEmployee(me)
                findViewById<MaterialButton>(R.id.btnMenuUsers).visibility =
                    if (me.role == ROLE_ADMIN) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                // ignore
            }
    }
}
