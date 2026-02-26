package com.example.pi2dam
import android.content.Intent
import android.view.View

import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

object AppMenu {

    fun bind(activity: AppCompatActivity) {
        bindLogoHomeNavigation(activity)
        val iv = (activity.findViewById<ImageView?>(R.id.btnAppbarMenu)
            ?: activity.findViewById<ImageView?>(R.id.btnMenu)) ?: return

        iv.setImageResource(R.drawable.ic_hamburger_menu)

        iv.setOnClickListener {
            animateToClose(iv)
            AppMenuSheet.show(activity.supportFragmentManager) {
                animateToHamburger(iv)
            }
        }
    }

    private fun bindLogoHomeNavigation(activity: AppCompatActivity) {
        val root = activity.findViewById<View>(android.R.id.content) ?: return
        val logoCandidates = ArrayList<View>()
        root.findViewsWithText(
            logoCandidates,
            activity.getString(R.string.cd_logo),
            View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION
        )
        val logo = logoCandidates.firstOrNull() ?: return
        logo.isClickable = true
        logo.isFocusable = true
        logo.setOnClickListener {
            if (activity is HomeActivity) return@setOnClickListener
            activity.startActivity(
                Intent(activity, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
        }
    }

    private fun animateToClose(iv: ImageView) {
        iv.animate().cancel()
        iv.animate()
            .rotation(90f)
            .alpha(0.2f)
            .setDuration(120)
            .withEndAction {
                iv.rotation = 0f
                iv.alpha = 1f
                iv.setImageResource(R.drawable.ic_close)
            }
            .start()
    }

    private fun animateToHamburger(iv: ImageView) {
        iv.animate().cancel()
        iv.animate()
            .rotation(-90f)
            .alpha(0.2f)
            .setDuration(120)
            .withEndAction {
                iv.rotation = 0f
                iv.alpha = 1f
                iv.setImageResource(R.drawable.ic_hamburger_menu)
            }
            .start()
    }
}
