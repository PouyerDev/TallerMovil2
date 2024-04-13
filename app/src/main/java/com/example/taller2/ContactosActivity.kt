package com.example.taller2

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.danielm.ContactsAdapter
import com.example.taller2.Datos.Companion.MY_PERMISSION_REQUEST_READ_CONTACTS

class ContactosActivity : AppCompatActivity() {

    var mProjection: Array<String>? = null
    var mCursor: Cursor? = null
    var mContactsAdapter: ContactsAdapter? = null
    var mlista: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contactos)

        //variables
        mlista = findViewById(R.id.listView)

        //proyeccion
        mProjection =
            arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)
        //asignar adaptador
        mContactsAdapter = ContactsAdapter(this, mCursor, 0)
        mlista?.adapter = mContactsAdapter


        AskPermisoContactos()

        //cargar contactos
       // initView()

    }

    private fun AskPermisoContactos() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.

                //cargar contactos

                initView()
               Toast.makeText(this, "Gracias", Toast.LENGTH_LONG).show()

            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_CONTACTS
            ) -> {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    MY_PERMISSION_REQUEST_READ_CONTACTS
                )


            }

            else -> {
                // You can directly ask for the permission.
                requestPermissions(
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    MY_PERMISSION_REQUEST_READ_CONTACTS
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSION_REQUEST_READ_CONTACTS -> {


                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //cargar contactos
                    initView()
                    Toast.makeText(this, "Gracias", Toast.LENGTH_LONG).show()

                    // Permission is granted. Continue the action or workflow
                    // in your app.
                } else {


                    Toast.makeText(this, "funcionalidades reduciadas", Toast.LENGTH_LONG).show()
                    // Explain to the user that the feature is unavailable
                    finish();
                }
                return
            }

            else -> {
                // Ignore all other requests.
            }
        }
    }

    fun initView() {

        mCursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI, mProjection, null, null, null
        )
        mContactsAdapter?.changeCursor(mCursor)

    }






}
