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

class AppMenuSheet : BottomSheetDialogFragment() {

    var onDismissCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.sheet_menu, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val host = activity ?: return

        fun go(cls: Class<*>) {
            startActivity(Intent(host, cls))
            dismiss()
        }

        val cardUsers = view.findViewById<View>(R.id.cardSheetUsers)
        cardUsers.visibility = if (Session.employee?.role == ROLE_ADMIN) View.VISIBLE else View.GONE

        view.findViewById<View>(R.id.cardSheetHome).setOnClickListener {
            startActivity(Intent(host, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            dismiss()
        }
        cardUsers.setOnClickListener { go(UsersActivity::class.java) }
        view.findViewById<View>(R.id.cardSheetProducts).setOnClickListener { go(ProductsActivity::class.java) }
        view.findViewById<View>(R.id.cardSheetSuppliers).setOnClickListener { go(SuppliersActivity::class.java) }
        view.findViewById<View>(R.id.cardSheetOrders).setOnClickListener { go(OrdersActivity::class.java) }
        view.findViewById<View>(R.id.cardSheetPayment).setOnClickListener { go(PaymentActivity::class.java) }
        view.findViewById<View>(R.id.cardSheetWarehouses).setOnClickListener { go(WarehousesMapActivity::class.java) }
        view.findViewById<View>(R.id.cardSheetHelp).setOnClickListener { go(HelpActivity::class.java) }
        view.findViewById<View>(R.id.btnSheetHelpIcon).setOnClickListener { go(HelpActivity::class.java) }

        view.findViewById<View>(R.id.btnSheetLogout).setOnClickListener {
            FirebaseRefs.auth.signOut()
            Session.clear()
            startActivity(Intent(host, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            dismiss()
        }

        view.findViewById<View>(R.id.chipSheetCreateOrder).setOnClickListener { go(CreateOrderActivity::class.java) }
        view.findViewById<View>(R.id.chipSheetCreateProduct).setOnClickListener { go(ProductFormActivity::class.java) }
        view.findViewById<View>(R.id.chipSheetResetPassword).setOnClickListener { go(ResetPasswordActivity::class.java) }

        // Si no hay sesión, mostramos ayuda y logout como "volver a login".
        if (FirebaseRefs.auth.currentUser == null) {
            fun disable(v: View) {
                v.isEnabled = false
                v.alpha = 0.5f
            }

            cardUsers.visibility = View.GONE
            disable(view.findViewById(R.id.cardSheetProducts))
            disable(view.findViewById(R.id.cardSheetSuppliers))
            disable(view.findViewById(R.id.cardSheetOrders))
            disable(view.findViewById(R.id.cardSheetPayment))
            disable(view.findViewById(R.id.cardSheetWarehouses))
            disable(view.findViewById(R.id.chipSheetCreateOrder))
            disable(view.findViewById(R.id.chipSheetCreateProduct))

            view.findViewById<View>(R.id.btnSheetLogout).setOnClickListener {
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
