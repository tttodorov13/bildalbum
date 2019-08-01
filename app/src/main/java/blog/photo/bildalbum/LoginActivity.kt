package blog.photo.bildalbum

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.facebook.*
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton

class LoginActivity : AppCompatActivity() {

    private var callbackManager: CallbackManager? = null
    private var accessTokenTracker: AccessTokenTracker? = null
    private var profileTracker: ProfileTracker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        callbackManager = CallbackManager.Factory.create()
        accessTokenTracker = object : AccessTokenTracker() {
            override fun onCurrentAccessTokenChanged(oldToken: AccessToken?, newToken: AccessToken?) {}
        }
        profileTracker = object : ProfileTracker() {
            override fun onCurrentProfileChanged(oldProfile: Profile?, newProfile: Profile?) {
                nextActivity(newProfile)
            }
        }
        accessTokenTracker?.startTracking()
        profileTracker?.startTracking()

        val loginButton = findViewById<View>(R.id.login_button) as LoginButton
        val callback = object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                val profile = Profile.getCurrentProfile()
                nextActivity(profile)
            }

            override fun onCancel() {}

            override fun onError(e: FacebookException) {}
        }
        loginButton.setPermissions("user_photos")
        loginButton.registerCallback(callbackManager, callback)
    }

    override fun onResume() {
        super.onResume()
        //Facebook login
        val profile = Profile.getCurrentProfile()
        nextActivity(profile)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        //Facebook login
        accessTokenTracker?.stopTracking()
        profileTracker?.stopTracking()
    }

    override fun onActivityResult(requestCode: Int, responseCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, responseCode, intent)
        //Facebook login
        callbackManager?.onActivityResult(requestCode, responseCode, intent)

    }

    private fun nextActivity(profile: Profile?) {
        if (profile != null) {
            val main = Intent(this@LoginActivity, MainActivity::class.java)
            main.putExtra("name", profile.firstName)
            main.putExtra("surname", profile.lastName)
            main.putExtra("imageUrl", profile.getProfilePictureUri(200, 200).toString())
            startActivity(main)
        }
    }
}
