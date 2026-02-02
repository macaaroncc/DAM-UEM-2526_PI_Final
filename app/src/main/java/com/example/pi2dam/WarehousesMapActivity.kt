package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions

class WarehousesMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var map: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_warehouses_map)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAppbarHelp).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
        findViewById<View>(R.id.btnAppbarMenu).setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Demo warehouses. Replace with your real data source (DB/API/Firebase/etc.).
        val warehouses = listOf(
            Warehouse("MAD", "Almacén Central (Madrid)", 40.4168, -3.7038),
            Warehouse("BCN", "Almacén Este (Barcelona)", 41.3874, 2.1686),
            Warehouse("VAL", "Almacén Levante (Valencia)", 39.4699, -0.3763)
        )

        if (warehouses.isEmpty()) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(40.4168, -3.7038), 5f))
            return
        }

        val boundsBuilder = LatLngBounds.Builder()
        warehouses.forEach { w ->
            val position = LatLng(w.lat, w.lng)
            googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(w.name)
            )
            boundsBuilder.include(position)
        }

        val bounds = boundsBuilder.build()
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
    }

    data class Warehouse(
        val id: String,
        val name: String,
        val lat: Double,
        val lng: Double
    )
}
