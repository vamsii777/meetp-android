package download.crossally.apps.hizkiyyah

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager
import download.crossally.apps.hizkiyyah.utils.AppConstants
import download.crossally.apps.hizkiyyah.utils.SharedObjects
import io.github.inflationx.viewpump.ViewPumpContextWrapper


class EmailVerificationActivity : AppCompatActivity() {
    @JvmField
    @BindView(R.id.btnVerify)
    var btnVerify: Button? = null

    @JvmField
    @BindView(R.id.txtEmail)
    var txtEmail: TextView? = null
    var sharedObjects: SharedObjects? = null
    private var firebaseAuth: FirebaseAuth? = null
    var databaseManager: DatabaseManager? = null
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)
        ButterKnife.bind(this)

        //Get Firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance()
        sharedObjects = SharedObjects(this@EmailVerificationActivity)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        databaseManager = DatabaseManager(this@EmailVerificationActivity)
        verifyUser()
    }

    fun verifyUser() {
        val user = firebaseAuth!!.currentUser
        txtEmail!!.text = user!!.email
        // Read from the database
        user.sendEmailVerification().addOnCompleteListener {task ->
            if (task.isSuccessful) {
                Toast.makeText(this@EmailVerificationActivity,
                        "Verification email sent to " + user.email,
                        Toast.LENGTH_SHORT).show()
            } else {
                Log.e("sendEmailVerification ", " " + task.exception)
                AppConstants.showAlertDialog(task.exception!!.message, this@EmailVerificationActivity)
            }
        }/*addOnCompleteListener(this, object : OnCompleteListener<Any> {
                    override fun onComplete(task: Task<*>) {
                        if (task.isSuccessful) {
                            Toast.makeText(this@EmailVerificationActivity,
                                    "Verification email sent to " + user.email,
                                    Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("sendEmailVerification ", " " + task.exception)
                            AppConstants.showAlertDialog(task.exception!!.message, this@EmailVerificationActivity)
                        }
                    }
                })*/
    }

    override fun onResume() {
        super.onResume()
    }

    @OnClick(R.id.btnVerify)
    fun onClick(view: View) {
        when (view.id) {
            R.id.btnVerify -> if (firebaseAuth != null && firebaseAuth!!.currentUser != null) {
                firebaseAuth!!.currentUser!!.reload()
                SharedObjects.hideKeyboard(btnVerify!!, this@EmailVerificationActivity)
                if (SharedObjects.isNetworkConnected(this@EmailVerificationActivity)) {
                    if (firebaseAuth!!.currentUser != null && firebaseAuth!!.currentUser!!.isEmailVerified) {
                        val intent = Intent(this@EmailVerificationActivity, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                        finish()
                    } else {
                        AppConstants.showAlertDialog("Please verify your email address and try again.", this@EmailVerificationActivity)
                    }
                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet), this@EmailVerificationActivity)
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}