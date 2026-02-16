package com.example.pi2dam

import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

object AppMenu {

    fun bind(activity: AppCompatActivity) {
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
