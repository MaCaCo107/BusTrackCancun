package com.mayo.bustrackcancun

import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.mayo.bustrackcancun.API.ReporteResponse
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

class CustomMarkerInfoWindow(
    mapView: MapView,
    private val reportId: String,
    private val reportAuthor: String,
    private val activeUser: String,
    private val onDeleteClicked: (String) -> Unit
) : InfoWindow(R.layout.custom_info_window, mapView) {

    override fun onOpen(item: Any?) {
        val marker = item as? Marker ?: return
        
        val tvTitle = mView.findViewById<TextView>(R.id.bubble_title)
        val tvSnippet = mView.findViewById<TextView>(R.id.bubble_snippet)
        val btnDelete = mView.findViewById<MaterialButton>(R.id.bubble_btn_delete)

        tvTitle.text = marker.title
        tvSnippet.text = marker.snippet

        // Lógica de seguridad: solo el autor ve el botón de eliminar
        if (reportAuthor.trim().equals(activeUser.trim(), ignoreCase = true)) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                onDeleteClicked(reportId)
                close()
            }
        } else {
            btnDelete.visibility = View.GONE
        }

        mView.setOnClickListener { close() }
    }

    override fun onClose() {}
}