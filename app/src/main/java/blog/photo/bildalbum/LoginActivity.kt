package blog.photo.bildalbum

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isInvisible
import blog.photo.bildalbum.receiver.ConnectivityReceiver
import com.facebook.*
import com.facebook.login.LoginResult
import kotlinx.android.synthetic.main.content_login.*

class LoginActivity : BaseActivity() {
    private var callbackManager: CallbackManager? = null
    private var accessTokenTracker: AccessTokenTracker? = null
    private var profileTracker: ProfileTracker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        for(i in getStoredImagesPaths())
            displayImage(i)

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

        val callback = object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                val profile = Profile.getCurrentProfile()
                nextActivity(profile)
            }

            override fun onCancel() {}

            override fun onError(e: FacebookException) {}
        }
        buttonFacebookLogin.setPermissions("user_photos")
        buttonFacebookLogin.registerCallback(callbackManager, callback)
    }

    override fun onResume() {
        super.onResume()
        //Facebook login
        val profile = Profile.getCurrentProfile()
        nextActivity(profile)
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        buttonFacebookLogin.isInvisible = !ConnectivityReceiver.isConnectedOrConnecting(this)
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
