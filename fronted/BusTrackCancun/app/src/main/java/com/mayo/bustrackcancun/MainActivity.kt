package com.mayo.bustrackcancun

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.mayo.bustrackcancun.API.ApiService
import com.mayo.bustrackcancun.API.Coordenada
import com.mayo.bustrackcancun.API.ReporteRequest
import com.mayo.bustrackcancun.API.ReporteResponse
import com.mayo.bustrackcancun.API.Ruta
import com.mayo.bustrackcancun.API.TrazadoResponse
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var apiService: ApiService
    private lateinit var bottomSheet: MaterialCardView
    private lateinit var tvRouteDetails: TextView
    private lateinit var cardSugerencias: MaterialCardView
    private lateinit var rvSugerencias: RecyclerView
    private lateinit var rutaAdapter: RutaAdapter
    private lateinit var etSearch: EditText
    private lateinit var fabAddComment: ExtendedFloatingActionButton
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var fragmentContainer: View
    private lateinit var layoutHeader: View

    private var isCommentsMode = false
    private var isCreationMode = false
    private var activeUserName: String = "Usuario"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, getSharedPreferences("osm_pref", MODE_PRIVATE))

        setContentView(R.layout.activity_main)

        // Inicializar Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // UI Elements
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        fragmentContainer = findViewById(R.id.fragment_container)
        layoutHeader = findViewById(R.id.layout_header)
        
        bottomSheet = findViewById(R.id.bottomSheet)
        tvRouteDetails = findViewById(R.id.tvRouteDetails)
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val profileCard = findViewById<MaterialCardView>(R.id.profileCard)
        etSearch = findViewById(R.id.etSearch)
        cardSugerencias = findViewById(R.id.cardSugerencias)
        rvSugerencias = findViewById(R.id.rvSugerencias)
        val ivMenu = findViewById<ImageView>(R.id.ivMenu)
        fabAddComment = findViewById(R.id.fabAddComment)

        // Recuperar nombre de usuario real
        val sharedPref = getSharedPreferences("BusTrackPrefs", MODE_PRIVATE)
        activeUserName = sharedPref.getString("userName", "Usuario") ?: "Usuario"
        tvUserName.text = activeUserName

        // Navigation Drawer Setup
        ivMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_map -> {
                    isCommentsMode = false
                    isCreationMode = false
                    fabAddComment.visibility = View.GONE
                    showMap()
                }
                R.id.nav_routes -> {
                    isCommentsMode = false
                    isCreationMode = false
                    fabAddComment.visibility = View.GONE
                    showRouteList()
                }
                R.id.nav_comments -> {
                    isCommentsMode = true
                    enableCommentsMode()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Setup RecyclerView para búsqueda predictiva
        rvSugerencias.layoutManager = LinearLayoutManager(this)
        rutaAdapter = RutaAdapter(emptyList()) { ruta ->
            etSearch.setText(ruta.nombre)
            cardSugerencias.visibility = View.GONE
            validarYSeleccionarRuta(ruta)
        }
        rvSugerencias.adapter = rutaAdapter

        // Update Nav Header User Info
        val headerView = navView.getHeaderView(0)
        val tvNavUserEmail = headerView.findViewById<TextView>(R.id.tv_nav_user_email)
        val userEmail = sharedPref.getString("userEmail", "transporte.cancun@bustrack.com")
        tvNavUserEmail.text = userEmail

        profileCard.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("Cerrar Sesión")
            popup.setOnMenuItemClickListener { item ->
                if (item.title == "Cerrar Sesión") {
                    with(sharedPref.edit()) {
                        clear()
                        apply()
                    }
                    val intent = Intent(this, WelcomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                true
            }
            popup.show()
        }

        // Inicializar Mapa
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        val mapController = map.controller
        mapController.setZoom(14.0)
        mapController.setCenter(GeoPoint(21.1619, -86.8515))

        // OnMapClickListener
        setupMapEvents()

        // Lógica de Búsqueda Predictiva
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.length > 2) {
                    buscarRutas(query)
                } else {
                    cardSugerencias.visibility = View.GONE
                    if (query.isEmpty()) {
                        if (::map.isInitialized) {
                            limpiarMapa()
                        }
                        bottomSheet.visibility = View.GONE
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Acción del botón cerrar ruta
        val btnCloseRoute = findViewById<ImageButton>(R.id.btnCloseRoute)
        btnCloseRoute.setOnClickListener {
            if (::map.isInitialized) {
                limpiarMapa()
                bottomSheet.visibility = View.GONE
                etSearch.setText("")
                map.invalidate()
            }
        }

        // FAB logic
        fabAddComment.setOnClickListener {
            isCreationMode = true
            Toast.makeText(this, "Toca el mapa para ubicar tu nota", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMapEvents() {
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                // Cerrar cualquier InfoWindow abierto al tocar el mapa
                InfoWindow.closeAllInfoWindowsOn(map)
                
                if (isCreationMode && p != null) {
                    isCreationMode = false
                    showNewCommentDialog(p)
                } else if (!isCommentsMode) {
                    limpiarMapa()
                    bottomSheet.visibility = View.GONE
                    cardSugerencias.visibility = View.GONE
                }
                return true
            }
            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (isCommentsMode && p != null) {
                    showNewCommentDialog(p)
                }
                return true
            }
        }
        map.overlays.add(MapEventsOverlay(mapEventsReceiver))
    }

    private fun enableCommentsMode() {
        showMap()
        fabAddComment.visibility = View.VISIBLE
        Toast.makeText(this, "Modo Comentarios: Mantén presionado o usa el botón '+'", Toast.LENGTH_LONG).show()
        cargarGlobosDeComentarios()
    }

    private fun cargarGlobosDeComentarios() {
        apiService.obtenerReportes().enqueue(object : Callback<List<ReporteResponse>> {
            override fun onResponse(call: Call<List<ReporteResponse>>, response: Response<List<ReporteResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    runOnUiThread {
                        limpiarMapa()
                        
                        response.body()?.forEach { reporte ->
                            val marker = Marker(map)
                            // Posición: [longitud, latitud] -> [coords[0], coords[1]]
                            marker.position = GeoPoint(reporte.ubicacion.coords[1], reporte.ubicacion.coords[0])
                            
                            marker.title = reporte.titulo
                            // Limpiar fecha ISO para el snippet
                            val cleanDate = reporte.fecha.split("T")[0]
                            marker.snippet = "${reporte.descripcion}\nFecha: $cleanDate\nAutor: ${reporte.usuario}"
                            
                            // Icono: Círculo amarillo con icono
                            marker.icon = resources.getDrawable(R.drawable.ic_comment_marker, null)
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            
                            // Usar el CustomMarkerInfoWindow con lógica de seguridad
                            val customInfoWindow = CustomMarkerInfoWindow(
                                map,
                                reporte.id,
                                reporte.usuario,
                                activeUserName
                            ) { id ->
                                eliminarReporte(id, marker)
                            }
                            
                            marker.infoWindow = customInfoWindow
                            map.overlays.add(marker)
                        }
                        map.invalidate()
                    }
                }
            }
            override fun onFailure(call: Call<List<ReporteResponse>>, t: Throwable) {
                Log.e("API_ERROR", "Error al cargar reportes: ${t.message}")
            }
        })
    }

    private fun eliminarReporte(id: String, marker: Marker) {
        lifecycleScope.launch {
            try {
                val response = apiService.eliminarReporte(id, activeUserName)
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Publicación eliminada", Toast.LENGTH_SHORT).show()
                    map.overlays.remove(marker)
                    map.invalidate()
                } else {
                    Toast.makeText(this@MainActivity, "Error al eliminar: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("DELETE_ERROR", "Falla en la petición", e)
                Toast.makeText(this@MainActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNewCommentDialog(point: GeoPoint) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_comment, null)
        
        val etTitle = view.findViewById<TextInputEditText>(R.id.etCommentTitle)
        val etDesc = view.findViewById<TextInputEditText>(R.id.etCommentDesc)
        val tvUser = view.findViewById<TextView>(R.id.tvCommentUser)
        val tvDate = view.findViewById<TextView>(R.id.tvCommentDate)
        val btnCancel = view.findViewById<ImageButton>(R.id.btnCancelComment)
        val btnPublish = view.findViewById<MaterialButton>(R.id.btnPublishComment)

        tvUser.text = "Publicado por: $activeUserName"
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        tvDate.text = "Fecha: $currentDate"

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val title = etTitle.text?.toString()?.trim() ?: ""
                val desc = etDesc.text?.toString()?.trim() ?: ""
                btnPublish.isEnabled = title.isNotEmpty() && desc.isNotEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etTitle.addTextChangedListener(textWatcher)
        etDesc.addTextChangedListener(textWatcher)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnPublish.setOnClickListener {
            // Recuperar nombre de usuario real de SharedPreferences justo antes del envío
            val sharedPref = getSharedPreferences("BusTrackPrefs", MODE_PRIVATE)
            val realUserName = sharedPref.getString("userName", activeUserName) ?: "Usuario"

            val reporte = ReporteRequest(
                titulo = etTitle.text.toString().trim(),
                descripcion = etDesc.text.toString().trim(),
                latitud = point.latitude,
                longitud = point.longitude,
                usuarioNombre = realUserName,
                fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )

            apiService.crearReporte(reporte).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Comentario publicado", Toast.LENGTH_SHORT).show()
                        cargarGlobosDeComentarios()
                        dialog.dismiss()
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error al publicar", Toast.LENGTH_SHORT).show()
                }
            })
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showMap() {
        fragmentContainer.visibility = View.GONE
        layoutHeader.visibility = View.VISIBLE
        map.visibility = View.VISIBLE
        limpiarMapa()
        bottomSheet.visibility = View.GONE
    }

    private fun showRouteList() {
        fragmentContainer.visibility = View.VISIBLE
        layoutHeader.visibility = View.VISIBLE
        map.visibility = View.GONE
        bottomSheet.visibility = View.GONE

        val fragment = RouteListFragment()
        fragment.setOnRouteSelectedListener { ruta ->
            if (ruta.id.isNullOrEmpty()) {
                Log.e("BusTrack_Error", "El ID de la ruta es nulo o vacío")
                return@setOnRouteSelectedListener
            }

            showMap()
            map.post {
                seleccionarRuta(ruta)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun buscarRutas(query: String) {
        apiService.buscarRutas(query).enqueue(object : Callback<List<Ruta>> {
            override fun onResponse(call: Call<List<Ruta>>, response: Response<List<Ruta>>) {
                if (response.isSuccessful && response.body() != null) {
                    val sugerencias = response.body()!!
                    if (sugerencias.isNotEmpty()) {
                        rutaAdapter.updateData(sugerencias)
                        cardSugerencias.visibility = View.VISIBLE
                    } else {
                        cardSugerencias.visibility = View.GONE
                    }
                }
            }
            override fun onFailure(call: Call<List<Ruta>>, t: Throwable) {
                Log.e("API_ERROR", "Error en búsqueda: ${t.message}")
            }
        })
    }

    private fun validarYSeleccionarRuta(ruta: Ruta) {
        if (ruta.id.isNullOrEmpty()) {
            Log.e("BusTrack_Error", "ID de ruta inválido para selección")
            return
        }
        seleccionarRuta(ruta)
    }

    private fun seleccionarRuta(ruta: Ruta) {
        apiService.obtenerTrazado(ruta.id).enqueue(object : Callback<TrazadoResponse> {
            override fun onResponse(call: Call<TrazadoResponse>, response: Response<TrazadoResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val trazado = response.body()!!
                    dibujarEnMapa(trazado.coordenadas)
                    
                    tvRouteDetails.text = trazado.nombre
                    bottomSheet.visibility = View.VISIBLE
                }
            }
            override fun onFailure(call: Call<TrazadoResponse>, t: Throwable) {
                Log.e("API_ERROR", "Error al cargar trazado: ${t.message}")
            }
        })
    }

    private fun dibujarEnMapa(trazado: List<Coordenada>) {
        runOnUiThread {
            Log.d("BusTrack_Debug", "Iniciando dibujo de ruta con ${trazado.size} puntos")
            limpiarMapa()

            val puntos = trazado.map { coord: Coordenada -> GeoPoint(coord.lat, coord.lng) }
            
            if (puntos.isEmpty()) {
                Log.e("BusTrack_Debug", "El trazado está vacío, no hay nada que dibujar")
                return@runOnUiThread
            }

            val polyline = Polyline()
            polyline.setPoints(puntos)
            polyline.outlinePaint.color = Color.RED
            polyline.outlinePaint.strokeWidth = 12f
            map.overlays.add(polyline)

            val startMarker = Marker(map)
            startMarker.position = puntos.first()
            startMarker.title = "Inicio de Ruta"
            map.overlays.add(startMarker)

            Log.d("BusTrack_Debug", "Polyline y Marker de inicio añadidos. Refrescando mapa.")
            map.controller.setZoom(15.0)
            map.controller.animateTo(puntos.first())
            map.invalidate()
        }
    }

    private fun limpiarMapa() {
        if (::map.isInitialized) {
            InfoWindow.closeAllInfoWindowsOn(map)
            map.overlays.clear()
            val mapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    InfoWindow.closeAllInfoWindowsOn(map)
                    if (isCreationMode && p != null) {
                        isCreationMode = false
                        showNewCommentDialog(p)
                    } else if (!isCommentsMode) {
                        limpiarMapa()
                        bottomSheet.visibility = View.GONE
                        cardSugerencias.visibility = View.GONE
                    }
                    return true
                }
                override fun longPressHelper(p: GeoPoint?): Boolean {
                    if (isCommentsMode && p != null) {
                        showNewCommentDialog(p)
                    }
                    return true
                }
            }
            map.overlays.add(MapEventsOverlay(mapEventsReceiver))
            map.invalidate()
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else if (fragmentContainer.visibility == View.VISIBLE) {
            showMap()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    // Clase interna para manejar la ventana de información personalizada
    inner class CommentInfoWindow(layoutResId: Int, mapView: MapView) : InfoWindow(layoutResId, mapView) {
        override fun onOpen(item: Any?) {
            val marker = item as Marker
            val reporte = marker.relatedObject as ReporteResponse
            
            val view = mView
            val tvTitle = view.findViewById<TextView>(R.id.bubble_title)
            val tvDesc = view.findViewById<TextView>(R.id.bubble_description)
            val tvUser = view.findViewById<TextView>(R.id.bubble_user)
            val tvDate = view.findViewById<TextView>(R.id.bubble_date)
            
            tvTitle.text = reporte.titulo
            tvDesc.text = reporte.descripcion
            tvUser.text = "Publicado por: ${reporte.usuario}"
            
            // Limpiar fecha ISO (YYYY-MM-DDTHH:mm:ss.sssZ -> YYYY-MM-DD)
            val cleanDate = reporte.fecha.split("T")[0]
            tvDate.text = "Fecha: $cleanDate"
            
            // Cerrar al hacer clic en la propia ventana
            view.setOnClickListener { close() }
        }

        override fun onClose() {}
    }
}