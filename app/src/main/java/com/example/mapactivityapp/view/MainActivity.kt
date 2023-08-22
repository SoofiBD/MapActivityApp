package com.example.mapactivityapp.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.mapactivityapp.R
import com.example.mapactivityapp.adapter.placeAdapter
import com.example.mapactivityapp.databinding.ActivityMainBinding
import com.example.mapactivityapp.model.Place
import com.example.mapactivityapp.roomdb.PlaceDatabase
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        val view=binding.root
        setContentView(view)

        val db = Room.databaseBuilder(applicationContext, PlaceDatabase::class.java, "Places")
            //.allowMainThreadQueries()
            .build()
        val placeDao = db.placeDao()

        compositeDisposable.add(placeDao.getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe(this::handleResponse))

    }

    private fun handleResponse(placeList: List<Place>){
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = placeAdapter(placeList)
        binding.recyclerView.adapter = adapter
        //binding.recyclerView.adapter?.notifyDataSetChanged()
    }



    fun addButton(view : View) {
        val intent = Intent(applicationContext, MapsActivity::class.java)
        intent.putExtra("info", "new")
        startActivity(intent)
    }
}