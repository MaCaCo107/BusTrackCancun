package com.mayo.bustrackcancun

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mayo.bustrackcancun.API.ApiService
import com.mayo.bustrackcancun.API.Ruta
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RouteListFragment : Fragment() {

    private lateinit var rvRoutes: RecyclerView
    private lateinit var apiService: ApiService
    private var onRouteSelectedListener: ((Ruta) -> Unit)? = null

    fun setOnRouteSelectedListener(listener: (Ruta) -> Unit) {
        onRouteSelectedListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_route_list, container, false)
        rvRoutes = view.findViewById(R.id.rvRoutes)
        rvRoutes.layoutManager = LinearLayoutManager(context)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.82:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        fetchRoutes()

        return view
    }

    private fun fetchRoutes() {
        apiService.obtenerRutas().enqueue(object : Callback<List<Ruta>> {
            override fun onResponse(call: Call<List<Ruta>>, response: Response<List<Ruta>>) {
                if (response.isSuccessful && response.body() != null) {
                    val adapter = FullRouteAdapter(response.body()!!) { ruta ->
                        onRouteSelectedListener?.invoke(ruta)
                    }
                    rvRoutes.adapter = adapter
                }
            }

            override fun onFailure(call: Call<List<Ruta>>, t: Throwable) {
                Log.e("API_ERROR", "Error fetching routes: ${t.message}")
            }
        })
    }
}

class FullRouteAdapter(
    private val routes: List<Ruta>,
    private val onShowRouteClick: (Ruta) -> Unit
) : RecyclerView.Adapter<FullRouteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: android.widget.TextView = view.findViewById(R.id.tvRouteName)
        val tvPath: android.widget.TextView = view.findViewById(R.id.tvRoutePath)
        val btnShow: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btnShowRoute)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_route, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val route = routes[position]
        holder.tvName.text = route.nombre
        val origin = route.origenNombre ?: "Punto A"
        val destination = route.destinoNombre ?: "Punto B"
        holder.tvPath.text = "Origen: $origin -> Destino: $destination"
        holder.btnShow.setOnClickListener { onShowRouteClick(route) }
    }

    override fun getItemCount() = routes.size
}