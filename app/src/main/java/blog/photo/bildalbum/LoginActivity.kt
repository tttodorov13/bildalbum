package blog.photo.bildalbum

import android.content.Intent
import android.os.Bundle
import kotlin.concurrent.thread

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        thread {
            Thread.sleep((3 * 1000).toLong())
            startActivity()
        }.priority = Thread.NORM_PRIORITY
    }

    private fun startActivity() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
