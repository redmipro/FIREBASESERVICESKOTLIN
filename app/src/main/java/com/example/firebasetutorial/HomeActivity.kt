package com.example.firebasetutorial

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.android.synthetic.main.activity_home.*

enum class ProviderType{
    BASIC,
    GOOGLE,
    FACEBOOK
    //metodo de autenticacion basico
    //auth basica solo email y contraseña
}

class HomeActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //setup
    //recuperar parametros bundle, pueden ser nulos y las guardamos en conmstantes y enviarlas al setup y si no existen enviar string vacio
        val bundle = intent.extras
        val email = bundle?.getString("email")
        val provider = bundle?.getString("provider")
        setup(email ?: "", provider ?: "")

        //guarder email y clave de sesion
        //guardado de datos
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
        prefs.apply()//aunque la app se detenga se puede recuperar los datos de sesion

        //Remote Config
        errorButton.visibility = View.INVISIBLE
        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener{ task ->
            if (task.isSuccessful){
                val showErrorButton = Firebase.remoteConfig.getBoolean("show_error_button")
                val errorButtonText = Firebase.remoteConfig.getString("error_button_text")

                if (showErrorButton){
                    errorButton.visibility = View.VISIBLE
                }
                errorButton.text = errorButtonText
            }
        }

    }

    private fun setup(email: String, provider: String){

        title = "Inicio"
        emailTextView.text = email//propiedad valor text y la actualice valor email
        providerTextView.text = provider

        logOutButton.setOnClickListener {

            //borrar datos al cerrar sesion
            val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()

            if (provider == ProviderType.FACEBOOK.name){
                LoginManager.getInstance().logOut()
            }


            FirebaseAuth.getInstance().signOut()
            onBackPressed()
        }

        errorButton.setOnClickListener {
            //datos de usuario que presenta la falla
            FirebaseCrashlytics.getInstance().setUserId(email)
            FirebaseCrashlytics.getInstance().setCustomKey("provider", provider)//proveedor del fallo, google/faCEBOOK,
            //enviar log de contexto de error
            FirebaseCrashlytics.getInstance().log("se ha pulsado el botón FORZAR ERROR")
            //forzar error
            throw RuntimeException("Forzado de error")
        }

        saveButton.setOnClickListener {
            //guardar / Actualizar datos
            db.collection("users").document(email).set(
                hashMapOf("provider" to provider,
                    "address" to addressTextView.text.toString(),
                "phone" to phoneTextView.text.toString())
            )
        }

        getButton.setOnClickListener {

            db.collection("users").document(email).get().addOnSuccessListener {
                addressTextView.setText(it.get("address") as String? )
                phoneTextView.setText(it.get("phone") as String? )
            }
        }

        deleteButton.setOnClickListener {

            db.collection("users").document(email).delete()
        }


        }

}
