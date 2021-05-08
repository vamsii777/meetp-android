package download.crossally.apps.hizkiyyah

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.coordinatorlayout.widget.CoordinatorLayout
import butterknife.BindView
import butterknife.ButterKnife
import com.google.firebase.auth.FirebaseAuth

import download.crossally.apps.hizkiyyah.utils.SharedObjects
import io.github.inflationx.viewpump.ViewPumpContextWrapper

class SplashActivity : AppCompatActivity() {
    @JvmField
    @BindView(R.id.txtVersionName)
    var txtVersionName: TextView? = null
    var sharedObjects: SharedObjects? = null
    private var firebaseAuth: FirebaseAuth? = null
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        //window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        //Get Firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance()
        sharedObjects = SharedObjects(this@SplashActivity)
        ButterKnife.bind(this)
        //setResult(Activity.RESULT_OK)
        txtVersionName!!.text = getString(R.string.version, SharedObjects.getVersion(this@SplashActivity))
        val content: RelativeLayout = findViewById(R.id.contSplash)
        content.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        Handler().postDelayed({
            val intentLogin: Intent
            if (firebaseAuth!!.currentUser != null) {
                if (checkIfEmailVerified()) {
                    intentLogin = Intent(this@SplashActivity, MainActivity::class.java)
                    intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intentLogin)
                    finish()
                } else {
                    intentLogin = Intent(this@SplashActivity, IntroActivity::class.java)
                    intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intentLogin)
                    finish()
                }
            } else {
                intentLogin = Intent(this@SplashActivity, IntroActivity::class.java)
                intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intentLogin)
                finish()
            }
        }, SPLASH_TIME_OUT.toLong())
    }

    private fun checkIfEmailVerified(): Boolean {
        val user = firebaseAuth!!.currentUser
        return if (user!!.isEmailVerified) {
            // user is verified
            true
        } else {
            // email is not verified
            // NOTE: don't forget to log out the user.
            firebaseAuth!!.signOut()
            false
        }
    }

    companion object {
        private const val SPLASH_TIME_OUT = 3000
    }
}