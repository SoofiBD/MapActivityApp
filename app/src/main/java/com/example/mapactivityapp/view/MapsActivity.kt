package com.example.mapactivityapp.view

import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.mapactivityapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.mapactivityapp.databinding.ActivityMapsBinding
import com.example.mapactivityapp.model.Place
import com.example.mapactivityapp.roomdb.PlaceDao
import com.example.mapactivityapp.roomdb.PlaceDatabase
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers


class MapsActivity : AppCompatActivity(), OnMapReadyCallback , GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private var trackBoolean:Boolean? = null
    private var selectedLat:Double? = null
    private var selectedLong:Double? = null
    private lateinit var db: PlaceDatabase
    private lateinit var placeDao: PlaceDao
    val compositeDisposable = CompositeDisposable()
    var placeFromMain : Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestLauncher()
        sharedPreferences = this.getSharedPreferences("com.example.mapactivityapp", MODE_PRIVATE)
        trackBoolean = false
        selectedLat = 0.0
        selectedLong = 0.0

        db = Room.databaseBuilder(applicationContext, PlaceDatabase::class.java, "Places")
            //.allowMainThreadQueries()
            .build()
        placeDao = db.placeDao()
        binding.saveButton.isEnabled = false

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap
        mMap.setOnMapLongClickListener(this)

        val intent = intent
        val info = intent.getStringExtra("info")

        if (info == "new"){

            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.GONE

            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: android.location.Location) {
                    trackBoolean = sharedPreferences.getBoolean("trackBoolean", false)
                    if(trackBoolean == true) {
                        val userLocation = LatLng(location.latitude, location.longitude)
                        mMap.addMarker(MarkerOptions().position(userLocation).title("Your Location"))
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation , 15f))
                    }
                }
                // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, locationListener)
            }

            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Snackbar.make(binding.root, "Location services required for app to function", Snackbar.LENGTH_INDEFINITE).setAction("OK") {
                        permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        //ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
                    }.show()
                } else {
                    permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    //ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
                }
                //ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, locationListener)
                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastKnownLocation != null) {
                    val userLastLocation = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
                    mMap.addMarker(MarkerOptions().position(userLastLocation).title("Your Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLastLocation , 15f))
                } else {
                    val userLastLocation = LatLng(0.0, 0.0)
                    mMap.addMarker(MarkerOptions().position(userLastLocation).title("Your Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLastLocation , 15f))
                }

                mMap.isMyLocationEnabled = true
            }
        }else{
            mMap.clear()
            placeFromMain = intent.getSerializableExtra("selecetedPlace") as Place
            placeFromMain?.let {
                val latLng = LatLng(it.latitude!!, it.longitude!!)
                mMap.addMarker(MarkerOptions().position(latLng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                binding.placeText.setText(it.name)
                binding.saveButton.visibility = View.GONE
                binding.deleteButton.visibility = View.VISIBLE
            }

        }


    }

    private fun requestLauncher() {

    permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        if(result) {
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, locationListener)
                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastKnownLocation != null) {
                    val userLastLocation = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
                    mMap.addMarker(MarkerOptions().position(userLastLocation).title("Your Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLastLocation , 15f))
                } else {
                    val userLastLocation = LatLng(0.0, 0.0)
                    mMap.addMarker(MarkerOptions().position(userLastLocation).title("Your Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLastLocation , 15f))
                }

                mMap.isMyLocationEnabled = true
            }
        } else {
            Snackbar.make(binding.root, "Location services required for app to function", Snackbar.LENGTH_INDEFINITE).setAction("OK") {
                requestLauncher()
            }.show()
        }
    }
    //permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun onMapLongClick(p0: LatLng) {
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(p0))

        selectedLat = p0.latitude
        selectedLong = p0.longitude

        binding.saveButton.isEnabled = true

    }
    fun save(view : View) {
        val place = Place(binding.placeText.text.toString(), selectedLat!!, selectedLong!!)
        //placeDao.insertAll(place)
        compositeDisposable.add(
            placeDao.insertAll(place)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse)
                )
    }

    private fun handleResponse(){
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
    fun delete(view : View) {
        //placeDao.delete(placeFromMain!!)
        compositeDisposable.add(
            placeDao.delete(placeFromMain!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse)
        )

    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}