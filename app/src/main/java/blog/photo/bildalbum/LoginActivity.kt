package blog.photo.bildalbum

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

/**
 * Class that manages the login screen.
 */
class LoginActivity : AppCompatActivity() {

    /**
     * OnCreate Activity
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
     * Method that redirect to the main screen
     *
     * @param savedInstanceState
     */
    private fun startActivity() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
