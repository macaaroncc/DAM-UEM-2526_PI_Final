package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.pi2dam.model.EmployeeProfile.Companion.ROLE_ADMIN
import com.google.android.material.textfield.TextInputLayout

class MenuActivity : AppCompatActivity() {

    private enum class Category { QUICK, SECTION, ACCOUNT }

    private data class MenuEntry(
        val view: View,
        val keywords: List<String>,
        val category: Category,
        val requiresLogin: Boolean = false,
        val requiresAdmin: Boolean = false
    )

    private var isLoggedIn = false
    private var isAdmin = false
    private var currentQuery = ""

    private lateinit var entries: List<MenuEntry>
    private lateinit var tvMenuUserName: TextView
    private lateinit var tvMenuUserRole: TextView
    private lateinit var tvMenuQuickActions: TextView
    private lateinit var tvMenuSections: TextView
    private lateinit var tvMenuAccount: TextView
    private lateinit var chipGroupMenuQuick: View
    private lateinit var tvMenuEmpty: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        AppMenu.bind(this)

        findViewById<View>(R.id.btnHelp).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
        findViewById<View>(R.id.btnMenuHelpIcon).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
        tvMenuUserName = findViewById(R.id.tvMenuUserName)
        tvMenuUserRole = findViewById(R.id.tvMenuUserRole)
        tvMenuQuickActions = findViewById(R.id.tvMenuQuickActions)
        tvMenuSections = findViewById(R.id.tvMenuSections)
        tvMenuAccount = findViewById(R.id.tvMenuAccount)
        chipGroupMenuQuick = findViewById(R.id.chipGroupMenuQuick)
        tvMenuEmpty = findViewById(R.id.tvMenuEmpty)

        val goHome = {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            finish()
        }

        findViewById<View>(R.id.cardMenuHome).setOnClickListener { goHome() }
        findViewById<View>(R.id.cardMenuUsers).setOnClickListener { startActivity(Intent(this, UsersActivity::class.java)) }
        findViewById<View>(R.id.cardMenuProducts).setOnClickListener { startActivity(Intent(this, ProductsActivity::class.java)) }
        findViewById<View>(R.id.cardMenuOrders).setOnClickListener { startActivity(Intent(this, OrdersActivity::class.java)) }
        findViewById<View>(R.id.cardMenuPayment).setOnClickListener { startActivity(Intent(this, PaymentActivity::class.java)) }
        findViewById<View>(R.id.cardMenuWarehouses).setOnClickListener {
            startActivity(Intent(this, WarehousesMapActivity::class.java))
        }
        findViewById<View>(R.id.cardMenuHelp).setOnClickListener { startActivity(Intent(this, HelpActivity::class.java)) }
        findViewById<View>(R.id.btnMenuLogoutPrimary).setOnClickListener {
            FirebaseRefs.auth.signOut()
            Session.clear()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        findViewById<View>(R.id.chipMenuCreateOrder).setOnClickListener {
            startActivity(Intent(this, CreateOrderActivity::class.java))
        }
        findViewById<View>(R.id.chipMenuCreateProduct).setOnClickListener {
            startActivity(Intent(this, ProductFormActivity::class.java))
        }
        findViewById<View>(R.id.chipMenuCreateUser).setOnClickListener {
            startActivity(Intent(this, UserFormActivity::class.java))
        }
        findViewById<View>(R.id.chipMenuWarehouses).setOnClickListener {
            startActivity(Intent(this, WarehousesMapActivity::class.java))
        }
        findViewById<View>(R.id.chipMenuResetPassword).setOnClickListener {
            startActivity(Intent(this, ResetPasswordActivity::class.java))
        }

        setupSearch()
        setupEntries()
        refreshMenuState()
    }

    override fun onStart() {
        super.onStart()
        val current = FirebaseRefs.auth.currentUser
        if (current == null) {
            Session.clear()
            refreshMenuState()
            return
        }

        PiRepository.ensureEmployeeAccess(current.uid)
            .addOnSuccessListener { me ->
                Session.setEmployee(me)
                refreshMenuState()
            }
            .addOnFailureListener {
                refreshMenuState()
            }
    }

    private fun setupSearch() {
        val til = findViewById<TextInputLayout>(R.id.tilMenuSearch)
        til.editText?.doAfterTextChanged {
            currentQuery = it?.toString().orEmpty()
            applyFilter()
        }
    }

    private fun setupEntries() {
        fun k(vararg text: String): List<String> = text.map { it.lowercase() }

        entries = listOf(
            MenuEntry(
                view = findViewById(R.id.chipMenuCreateOrder),
                keywords = k(getString(R.string.menu_quick_order), getString(R.string.title_create_order)),
                category = Category.QUICK,
                requiresLogin = true
            ),
            MenuEntry(
                view = findViewById(R.id.chipMenuCreateProduct),
                keywords = k(getString(R.string.menu_quick_product), getString(R.string.title_product_form)),
                category = Category.QUICK,
                requiresLogin = true
            ),
            MenuEntry(
                view = findViewById(R.id.chipMenuCreateUser),
                keywords = k(getString(R.string.menu_quick_user), getString(R.string.title_user_form)),
                category = Category.QUICK,
                requiresLogin = true,
                requiresAdmin = true
            ),
            MenuEntry(
                view = findViewById(R.id.chipMenuWarehouses),
                keywords = k(getString(R.string.menu_quick_warehouses), getString(R.string.title_warehouses_map)),
                category = Category.QUICK,
                requiresLogin = true
            ),
            MenuEntry(
                view = findViewById(R.id.chipMenuResetPassword),
                keywords = k(getString(R.string.menu_quick_reset_password), getString(R.string.title_reset_password)),
                category = Category.QUICK
            ),
            MenuEntry(
                view = findViewById(R.id.cardMenuHome),
                keywords = k(
                    getString(R.string.menu_home),
                    getString(R.string.menu_subtitle_home),
                    getString(R.string.title_home),
                    getString(R.string.inventory_title)
                ),
                category = Category.SECTION
            ),
            MenuEntry(
                view = findViewById(R.id.cardMenuUsers),
                keywords = k(getString(R.string.menu_users), getString(R.string.menu_subtitle_users), getString(R.string.title_users)),
                category = Category.SECTION,
                requiresLogin = true,
                requiresAdmin = true
            ),
            MenuEntry(
                view = findViewById(R.id.cardMenuProducts),
                keywords = k(getString(R.string.menu_products), getString(R.string.menu_subtitle_products), getString(R.string.title_products)),
                category = Category.SECTION,
                requiresLogin = true
            ),
            MenuEntry(
                view = findViewById(R.id.cardMenuOrders),
                keywords = k(
                    getString(R.string.menu_orders),
                    getString(R.string.menu_subtitle_orders),
                    getString(R.string.title_orders)
                ),
                category = Category.SECTION,
                requiresLogin = true
            ),
            MenuEntry(
                view = findViewById(R.id.cardMenuPayment),
                keywords = k(getString(R.string.menu_payment), getString(R.string.menu_subtitle_payment), getString(R.string.title_payment)),
                category = Category.SECTION,
                requiresLogin = true
            ),
            MenuEntry(
                view = findViewById(R.id.cardMenuWarehouses),
                keywords = k(
                    getString(R.string.menu_warehouses_map),
                    getString(R.string.menu_subtitle_warehouses),
                    getString(R.string.title_warehouses_map)
                ),
                category = Category.SECTION,
                requiresLogin = true
            ),
            MenuEntry(
                view = findViewById(R.id.cardMenuHelp),
                keywords = k(getString(R.string.menu_help), getString(R.string.menu_subtitle_help), getString(R.string.title_help)),
                category = Category.SECTION
            ),
            MenuEntry(
                view = findViewById(R.id.btnMenuLogoutPrimary),
                keywords = k(getString(R.string.menu_logout), getString(R.string.menu_subtitle_logout)),
                category = Category.ACCOUNT,
                requiresLogin = true
            )
        )
    }

    private fun refreshMenuState() {
        isLoggedIn = FirebaseRefs.auth.currentUser != null
        isAdmin = Session.employee?.role == ROLE_ADMIN

        if (isLoggedIn) {
            val profile = Session.employee
            val name = profile?.name?.takeIf { it.isNotBlank() }
                ?: profile?.email?.takeIf { it.isNotBlank() }
                ?: FirebaseRefs.auth.currentUser?.email
                ?: getString(R.string.menu_guest)
            tvMenuUserName.text = name
            tvMenuUserRole.text = if (isAdmin) {
                getString(R.string.menu_role_admin)
            } else {
                getString(R.string.menu_role_worker)
            }
        } else {
            tvMenuUserName.setText(R.string.menu_guest)
            tvMenuUserRole.setText(R.string.menu_guest_subtitle)
        }

        applyFilter()
    }

    private fun applyFilter() {
        val q = currentQuery.trim().lowercase()
        var quick = 0
        var section = 0
        var account = 0

        entries.forEach { entry ->
            val allowed = when {
                entry.requiresAdmin && !isAdmin -> false
                entry.requiresLogin && !isLoggedIn -> false
                else -> true
            }
            val matches = q.isBlank() || entry.keywords.any { it.contains(q) }
            val show = allowed && matches
            entry.view.visibility = if (show) View.VISIBLE else View.GONE
            if (show) {
                when (entry.category) {
                    Category.QUICK -> quick++
                    Category.SECTION -> section++
                    Category.ACCOUNT -> account++
                }
            }
        }

        val hasQuick = quick > 0
        tvMenuQuickActions.visibility = if (hasQuick) View.VISIBLE else View.GONE
        chipGroupMenuQuick.visibility = if (hasQuick) View.VISIBLE else View.GONE
        tvMenuSections.visibility = if (section > 0) View.VISIBLE else View.GONE
        tvMenuAccount.visibility = if (account > 0) View.VISIBLE else View.GONE
        tvMenuEmpty.visibility = if (quick + section + account == 0) View.VISIBLE else View.GONE
    }
}
