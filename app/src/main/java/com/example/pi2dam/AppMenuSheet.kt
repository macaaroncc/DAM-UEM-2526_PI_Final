package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.example.pi2dam.model.EmployeeProfile.Companion.ROLE_ADMIN
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class AppMenuSheet : BottomSheetDialogFragment() {

    var onDismissCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.sheet_menu, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val host = activity ?: return

        val btnUsers = view.findViewById<MaterialButton>(R.id.btnSheetUsers)
        btnUsers.visibility = if (Session.employee?.role == ROLE_ADMIN) View.VISIBLE else View.GONE

        fun go(cls: Class<*>) {
            startActivity(Intent(host, cls))
            dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btnSheetHome).setOnClickListener {
            startActivity(Intent(host, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            dismiss()
        }

        btnUsers.setOnClickListener { go(UsersActivity::class.java) }
        view.findViewById<MaterialButton>(R.id.btnSheetProducts).setOnClickListener { go(ProductsActivity::class.java) }
        view.findViewById<MaterialButton>(R.id.btnSheetDashboard).setOnClickListener { go(DashboardActivity::class.java) }
        view.findViewById<MaterialButton>(R.id.btnSheetChat).setOnClickListener { go(ChatActivity::class.java) }
        view.findViewById<MaterialButton>(R.id.btnSheetOrders).setOnClickListener { go(OrdersActivity::class.java) }
        view.findViewById<MaterialButton>(R.id.btnSheetPayment).setOnClickListener { go(PaymentActivity::class.java) }
        view.findViewById<MaterialButton>(R.id.btnSheetWarehouses).setOnClickListener { go(WarehousesMapActivity::class.java) }
        view.findViewById<MaterialButton>(R.id.btnSheetHelp).setOnClickListener { go(HelpActivity::class.java) }

        view.findViewById<MaterialButton>(R.id.btnSheetLogout).setOnClickListener {
            FirebaseRefs.auth.signOut()
            Session.clear()
            startActivity(Intent(host, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            dismiss()
        }

        // Si no hay sesión, mostramos ayuda y logout como "volver a login".
        if (FirebaseRefs.auth.currentUser == null) {
            btnUsers.visibility = View.GONE
            view.findViewById<MaterialButton>(R.id.btnSheetProducts).isEnabled = false
            view.findViewById<MaterialButton>(R.id.btnSheetDashboard).isEnabled = false
            view.findViewById<MaterialButton>(R.id.btnSheetChat).isEnabled = false
            view.findViewById<MaterialButton>(R.id.btnSheetOrders).isEnabled = false
            view.findViewById<MaterialButton>(R.id.btnSheetPayment).isEnabled = false
            view.findViewById<MaterialButton>(R.id.btnSheetWarehouses).isEnabled = false
            view.findViewById<MaterialButton>(R.id.btnSheetLogout).setOnClickListener {
                Toast.makeText(host, R.string.help_need_login, Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        onDismissCallback?.invoke()
        onDismissCallback = null
        super.onDestroyView()
    }

    companion object {
        fun show(fm: FragmentManager, onDismiss: (() -> Unit)? = null) {
            val f = AppMenuSheet()
            f.onDismissCallback = onDismiss
            f.show(fm, "app_menu")
        }
    }
}
