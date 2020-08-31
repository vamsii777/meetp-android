package download.crossally.apps.hizkiyyah

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.AppUpdaterError
import com.github.javiersantos.appupdater.objects.Update
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import download.crossally.apps.hizkiyyah.LoginActivity
import download.crossally.apps.hizkiyyah.RegisterActivity
import download.crossally.apps.hizkiyyah.bean.UserBean
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager
import download.crossally.apps.hizkiyyah.utils.AppConstants
import download.crossally.apps.hizkiyyah.utils.SharedObjects
import io.github.inflationx.viewpump.ViewPumpContextWrapper

class LoginActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {
    @JvmField
    @BindView(R.id.btnLogin)
    var btnLogin: Button? = null

    @JvmField
    @BindView(R.id.llCreateAccount)
    var llCreateAccount: LinearLayout? = null

    @JvmField
    @BindView(R.id.inputLayoutEmail)
    var inputLayoutEmail: TextInputLayout? = null

    @JvmField
    @BindView(R.id.inputLayoutPassword)
    var inputLayoutPassword: TextInputLayout? = null

    @JvmField
    @BindView(R.id.edtEmail)
    var edtEmail: EditText? = null

    @JvmField
    @BindView(R.id.edtPassword)
    var edtPassword: EditText? = null

    @JvmField
    @BindView(R.id.txtForgotPassword)
    var txtForgotPassword: TextView? = null

    @JvmField
    @BindView(R.id.btnGoogleSignIn)
    var btnGoogleSignIn: SignInButton? = null
    var sharedObjects: SharedObjects? = null
    private var dfUser: DatabaseReference? = null
    var mDatabaseManager: DatabaseManager? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var progressDialog: ProgressDialog? = null
    private var googleSignInClient: GoogleSignInClient? = null
    var databaseManager: DatabaseManager? = null
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        ButterKnife.bind(this)

        //Get Firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance()
        databaseManager = DatabaseManager(this@LoginActivity)
        sharedObjects = SharedObjects(this@LoginActivity)
        setEdtListeners()
        mDatabaseManager = DatabaseManager(this@LoginActivity)
        dfUser = FirebaseDatabase.getInstance().getReference(AppConstants.Table.USERS)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    override fun onStart() {
        super.onStart()
        if (SharedObjects.isNetworkConnected(this@LoginActivity)) {
            val appUpdaterUtils = AppUpdaterUtils(this)
                    .withListener(object : AppUpdaterUtils.UpdateListener {
                        override fun onSuccess(update: Update, isUpdateAvailable: Boolean) {
                            if (isUpdateAvailable) {
                                launchUpdateDialog(update.latestVersion)
                            }
                        }

                        override fun onFailed(error: AppUpdaterError) {}
                    })
            appUpdaterUtils.start()
        }
    }

    private fun launchUpdateDialog(onlineVersion: String) {
        try {
            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(this@LoginActivity)
            materialAlertDialogBuilder.setMessage("Update " + onlineVersion + " is available to download. Downloading the latest update you will get the latest features," +
                    "improvements and bug fixes of " + getString(R.string.app_name))
            materialAlertDialogBuilder.setCancelable(false).setPositiveButton(resources.getString(R.string.update_now)) { dialog, which ->
                dialog.dismiss()
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            }
            materialAlertDialogBuilder.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    @OnClick(R.id.btnLogin, R.id.llCreateAccount, R.id.txtForgotPassword, R.id.btnGoogleSignIn)
    fun onClick(view: View) {
        when (view.id) {
            R.id.btnGoogleSignIn -> {
                val signInIntent = googleSignInClient!!.signInIntent
                startActivityForResult(signInIntent, SIGN_IN_REQUEST)
            }
            R.id.btnLogin -> {
                SharedObjects.hideKeyboard(btnLogin, this@LoginActivity)
                if (SharedObjects.isNetworkConnected(this@LoginActivity)) {
                    if (!validateEmail()) {
                        return
                    }
                    if (!validatePassword()) {
                        return
                    }
                    btnLogin!!.isEnabled = false
                    checkUserLogin()
                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet), this@LoginActivity)
                }
            }
            R.id.llCreateAccount -> {
                val intentLogin: Intent
                intentLogin = Intent(this@LoginActivity, RegisterActivity::class.java)
                intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intentLogin)
                finish()
            }
            R.id.txtForgotPassword -> if (SharedObjects.isNetworkConnected(this@LoginActivity)) {
                showForgotPasswordDialog()
            } else {
                AppConstants.showAlertDialog(getString(R.string.err_internet), this@LoginActivity)
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_REQUEST) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    checkEmailExists(account.email)
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    firebaseAuthWithGoogle(credential, account)
                }
            } catch (e: ApiException) {
                Log.w("signInResult", ":failed code=" + e.statusCode)
            }
        }
    }

    var isExist = false
    var userBeanData: UserBean? = null
    private fun checkEmailExists(email: String?): Boolean {
        isExist = false
        val query = dfUser!!.orderByChild("email").equalTo(email)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.e("User", "exists")
                    for (postSnapshot in dataSnapshot.children) {
                        if (postSnapshot.getValue(UserBean::class.java)!!.email == email) {
                            isExist = true
                            userBeanData = UserBean()
                            userBeanData = postSnapshot.getValue(UserBean::class.java)
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        return isExist
    }

    private fun firebaseAuthWithGoogle(credential: AuthCredential, account: GoogleSignInAccount) {
        firebaseAuth!!.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    Log.d("signInWith", "Credential:onComplete:" + task.isSuccessful)
                    if (task.isSuccessful) {
                        var userBean = UserBean()
                        if (userBeanData != null) {
                            userBean = userBeanData!!
                        }
                        userBean.id = firebaseAuth!!.currentUser!!.uid
                        userBean.email = account.email
                        userBean.profile_pic = account.photoUrl.toString()
                        if (!isExist) {
                            userBean.name = account.displayName
                            databaseManager!!.addUser(userBean)
                        } else {
                            databaseManager!!.updateUser(userBean)
                        }
                        googleSignInClient!!.signOut()
                        val intentLogin: Intent
                        intentLogin = Intent(this@LoginActivity, MainActivity::class.java)
                        intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intentLogin)
                        finish()
                    } else {
                        Log.w("signInWith", "Credential" + task.exception!!.message)
                        task.exception!!.printStackTrace()
                        Toast.makeText(this@LoginActivity, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun checkUserLogin() {
        showProgressDialog()
        //authenticate user
        firebaseAuth!!.signInWithEmailAndPassword(edtEmail!!.text.toString().trim { it <= ' ' }, edtPassword!!.text.toString().trim { it <= ' ' })
                .addOnCompleteListener(this@LoginActivity) { task ->
                    btnLogin!!.isEnabled = true
                    dismissProgressDialog()

                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (!task.isSuccessful) {
                        // there was an error
                        AppConstants.showAlertDialog("Authentication failed, check your email and password or sign up", this@LoginActivity)
                    } else {
                        val userBean = UserBean()
                        userBean.id = firebaseAuth!!.currentUser!!.uid
                        userBean.name = ""
                        userBean.email = edtEmail!!.text.toString()
                        userBean.profile_pic = ""
                        if (!isExist) {
                            databaseManager!!.addUser(userBean)
                        }
                        val intentLogin: Intent
                        intentLogin = if (firebaseAuth!!.currentUser != null && firebaseAuth!!.currentUser!!.isEmailVerified) {
                            Intent(this@LoginActivity, MainActivity::class.java)
                        } else {
                            Intent(this@LoginActivity, EmailVerificationActivity::class.java)
                        }
                        intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intentLogin)
                        finish()
                    }
                }
    }

    var inputLayoutFPEmail: TextInputLayout? = null
    var edtFPEmail: TextInputEditText? = null
    fun showForgotPasswordDialog() {
        val dialogDate = Dialog(this@LoginActivity)
        dialogDate.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogDate.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogDate.setContentView(R.layout.dialog_forgot_password)
        dialogDate.setCancelable(true)
        val window = dialogDate.window
        val wlp = window!!.attributes
        wlp.gravity = Gravity.CENTER
        wlp.flags = wlp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
        wlp.dimAmount = 0.8f
        window.attributes = wlp
        dialogDate.window!!.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        inputLayoutFPEmail = dialogDate.findViewById(R.id.inputLayoutFPEmail)
        edtFPEmail = dialogDate.findViewById(R.id.edtFPEmail)
        edtFPEmail!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                inputLayoutFPEmail!!.setErrorEnabled(false)
                inputLayoutFPEmail!!.setError("")
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        val btnAdd = dialogDate.findViewById<Button>(R.id.btnAdd)
        val btnCancel = dialogDate.findViewById<Button>(R.id.btnCancel)
        btnAdd.setOnClickListener(View.OnClickListener {
            if (TextUtils.isEmpty(edtFPEmail!!.getText().toString().trim { it <= ' ' })) {
                inputLayoutFPEmail!!.isErrorEnabled = true
                inputLayoutFPEmail!!.setError(getString(R.string.errEmailRequired))
                return@OnClickListener
            }
            if (!AppConstants.isValidEmail(edtFPEmail!!.getText().toString().trim { it <= ' ' })) {
                inputLayoutFPEmail!!.isErrorEnabled = true
                inputLayoutFPEmail!!.setError(getString(R.string.errValidEmailRequired))
                return@OnClickListener
            }
            firebaseAuth!!.sendPasswordResetEmail(edtFPEmail!!.getText().toString().trim { it <= ' ' })
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(this@LoginActivity)
                            materialAlertDialogBuilder.setMessage(getString(R.string.we_have_sent_instructions))
                            materialAlertDialogBuilder.setCancelable(false).setPositiveButton(resources.getString(R.string.ok)) { dialog, which ->
                                dialog.dismiss()
                                dialogDate.dismiss()
                            }
                            materialAlertDialogBuilder.show()
                        } else {
                            AppConstants.showAlertDialog(task.exception!!.message, this@LoginActivity)
                        }
                    }
        })
        btnCancel.setOnClickListener { dialogDate.dismiss() }
        if (!dialogDate.isShowing) {
            dialogDate.show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun setEdtListeners() {
        edtEmail!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                inputLayoutEmail!!.isErrorEnabled = false
                inputLayoutEmail!!.error = ""
                if (!TextUtils.isEmpty(edtEmail!!.text.toString().trim { it <= ' ' })) {
                    checkEmailExists(edtEmail!!.text.toString().trim { it <= ' ' })
                } else {
                    isExist = false
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        edtPassword!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                inputLayoutPassword!!.isErrorEnabled = false
                inputLayoutPassword!!.error = ""
            }

            override fun afterTextChanged(editable: Editable) {}
        })
    }

    fun validateEmail(): Boolean {
        if (TextUtils.isEmpty(edtEmail!!.text.toString().trim { it <= ' ' })) {
            inputLayoutEmail!!.isErrorEnabled = true
            inputLayoutEmail!!.error = getString(R.string.errEmailRequired)
            return false
        } else if (!AppConstants.isValidEmail(edtEmail!!.text.toString().trim { it <= ' ' })) {
            inputLayoutEmail!!.isErrorEnabled = true
            inputLayoutEmail!!.error = getString(R.string.errValidEmailRequired)
            return false
        }
        return true
    }

    fun validatePassword(): Boolean {
        if (TextUtils.isEmpty(edtPassword!!.text.toString().trim { it <= ' ' })) {
            inputLayoutPassword!!.isErrorEnabled = true
            inputLayoutPassword!!.error = getString(R.string.errPasswordRequired)
            return false
        } else if (edtPassword!!.text.toString().trim { it <= ' ' }.length < 6) {
            inputLayoutPassword!!.isErrorEnabled = true
            inputLayoutPassword!!.error = getString(R.string.errPasswordTooShort)
            return false
        }
        return true
    }

    fun showProgressDialog() {
        try {
            progressDialog = ProgressDialog(this@LoginActivity)
            progressDialog!!.max = 100
            progressDialog!!.setMessage(getString(R.string.authenticating))
            progressDialog!!.setCancelable(true)
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            if (!this@LoginActivity.isFinishing) {
                progressDialog!!.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissProgressDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) progressDialog!!.dismiss()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {}

    companion object {
        private const val SIGN_IN_REQUEST = 1
    }
}