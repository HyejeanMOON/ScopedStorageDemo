package com.hyejeanmoon.scopedstoragedemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.hyejeanmoon.scopedstoragedemo.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + Job()

    private lateinit var binding: ActivityMainBinding

    private lateinit var viewModel: ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(ViewModel::class.java)

        binding.btnDownload.setOnClickListener {
            fileDownLoadWithCheckPermission()
        }
        viewModel.result.observe(this, Observer {
            Toast.makeText(this, "Download file is Completed!", Toast.LENGTH_LONG).show()
        })
    }

    private fun fileDownLoadWithCheckPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_WRITE_STORAGE
                )
                return
            }
        }
        launch {
            viewModel.fileDownload(URL)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_WRITE_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launch {
                        viewModel.fileDownload(URL)
                    }
                } else {
                    Toast.makeText(this, "Please agree permissions!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    companion object {
        const val URL =
            "https://unsplash.com/photos/ooEjKDNW47o/download?force=true&w=1920"

        internal const val REQUEST_CODE_WRITE_STORAGE = 0
    }
}
