package com.example.geologgersample

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import me.mmd44.wsmobile.GeoLogger

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private val geoLogger = GeoLogger()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.log_button).setOnClickListener {
            geoLogger.log(requireActivity().application, null, null)
        }
        view.findViewById<Button>(R.id.coarse_button).setOnClickListener {
            requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        view.findViewById<Button>(R.id.fine_button).setOnClickListener {
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        view.findViewById<Button>(R.id.background_button).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    private fun requestPermission(permissionString: String) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                permissionString
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(permissionString),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(requireActivity(), "Permission Granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireActivity(), "Permission Denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG = "FirstFragment"
        internal const val PERMISSION_REQUEST_CODE = 10
    }
}