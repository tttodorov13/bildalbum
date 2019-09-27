package blog.photo.buildalbum

import android.content.Intent
import android.os.Bundle
import kotlin.concurrent.thread

/**
 * Class to manage the login screen.
 */
class LoginActivity : AppBase() {

    /**
     * OnCreate LoginActivity
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        thread {
            Thread.sleep((3 * 1000).toLong())
            startActivity()
        }.priority = Thread.NORM_PRIORITY
    }

    /**
     * Method to redirect to the main screen
     */
    private fun startActivity() {
        startActivity(Intent(applicationContext, MainActivity::class.java))
        finish()
    }
}
