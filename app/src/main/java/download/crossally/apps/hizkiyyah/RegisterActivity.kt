package download.crossally.apps.hizkiyyah

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import download.crossally.apps.hizkiyyah.RegisterActivity
import download.crossally.apps.hizkiyyah.bean.UserBean
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager
import download.crossally.apps.hizkiyyah.utils.AppConstants
import download.crossally.apps.hizkiyyah.utils.SharedObjects
import io.github.inflationx.viewpump.ViewPumpContextWrapper

class RegisterActivity() : AppCompatActivity() {
    @JvmField
    @BindView(R.id.btnRegister)
    var btnRegister: Button? = null

    @JvmField
    @BindView(R.id.llLogin)
    var llLogin: LinearLayout? = null

    @JvmField
    @BindView(R.id.inputLayoutEmail)
    var inputLayoutEmail: TextInputLayout? = null

    @JvmField
    @BindView(R.id.inputLayoutName)
    var inputLayoutName: TextInputLayout? = null

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
    @BindView(R.id.edtName)
    var edtName: EditText? = null
    var sharedObjects: SharedObjects? = null
    private var firebaseAuth: FirebaseAuth? = null
    var databaseManager: DatabaseManager? = null
    private var dfUser: DatabaseReference? = null
    private val TAG = "Reg"
    private var progressDialog: ProgressDialog? = null

    @JvmField
    @BindView(R.id.btnGoogleSignIn)
    var btnGoogleSignIn: SignInButton? = null
    private var googleSignInClient: GoogleSignInClient? = null
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        ButterKnife.bind(this)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        //Get Firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance()
        databaseManager = DatabaseManager(this@RegisterActivity)
        dfUser = FirebaseDatabase.getInstance().getReference(AppConstants.Table.USERS)
        sharedObjects = SharedObjects(this@RegisterActivity)
        setEdtListeners()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    override fun onResume() {
        super.onResume()
    }

    @OnClick(R.id.btnRegister, R.id.llLogin, R.id.btnGoogleSignIn)
    fun onClick(view: View) {
        when (view.id) {
            R.id.btnGoogleSignIn -> {
                val signInIntent = googleSignInClient!!.signInIntent
                startActivityForResult(signInIntent, SIGN_IN_REQUEST)
            }
            R.id.btnRegister -> {
                SharedObjects.hideKeyboard(btnRegister!!, this@RegisterActivity)
                if (SharedObjects.isNetworkConnected(this@RegisterActivity)) {
                    if (!validateName()) {
                        return
                    }
                    if (!validateEmail()) {
                        return
                    }
                    if (isExist) {
                        inputLayoutEmail!!.isErrorEnabled = true
                        inputLayoutEmail!!.error = getString(R.string.email_exists)
                        return
                    }
                    if (!validatePassword()) {
                        return
                    }
                    btnRegister!!.isEnabled = false
                    checkUserLogin()
                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet), this@RegisterActivity)
                }
            }
            R.id.llLogin -> {
                val intentLogin: Intent
                intentLogin = Intent(this@RegisterActivity, LoginActivity::class.java)
                intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intentLogin)
                finish()
            }
        }
    }

    private fun checkUserLogin() {
        showProgressDialog()
        //create user
        firebaseAuth!!.createUserWithEmailAndPassword(edtEmail!!.text.toString().trim { it <= ' ' }, edtPassword!!.text.toString().trim { it <= ' ' })
                .addOnCompleteListener(this, OnCompleteListener { task ->
                    dismissProgressDialog()
                    btnRegister!!.isEnabled = true
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.e(TAG, "createUserWithEmail:success")
                        val user = firebaseAuth!!.currentUser
                        if (user != null) {
                            Log.e("UserBean", "Not null")
                            val userBean = UserBean()
                            userBean.id = user.uid
                            userBean.name = edtName!!.text.toString().trim { it <= ' ' }
                            userBean.email = edtEmail!!.text.toString().trim { it <= ' ' }
                            addUser(userBean)
                            val intentLogin: Intent
                            intentLogin = Intent(this@RegisterActivity, EmailVerificationActivity::class.java)
                            intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intentLogin)
                            finish()
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        AppConstants.showAlertDialog("Registration failed, " + task.exception!!.message, this@RegisterActivity)
                    }
                })
    }

    private fun addUser(userBean: UserBean) {
        databaseManager!!.addUser(userBean)
    }

    fun validateName(): Boolean {
        if (TextUtils.isEmpty(edtName!!.text.toString().trim { it <= ' ' })) {
            inputLayoutName!!.isErrorEnabled = true
            inputLayoutName!!.error = getString(R.string.err_name)
            return false
        }
        return true
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

    override fun onBackPressed() {
        super.onBackPressed()
        //        finish();
    }

    var isExist = false
    private fun checkEmailExists(email: String?): Boolean {
        isExist = false
        val query = dfUser!!.orderByChild("email").equalTo(email)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.e("User", "exists")
                    for (postSnapshot: DataSnapshot in dataSnapshot.children) {
                        if ((postSnapshot.getValue(UserBean::class.java)!!.email == email)) {
                            isExist = true
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
        return isExist
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

    private fun firebaseAuthWithGoogle(credential: AuthCredential, account: GoogleSignInAccount) {
        firebaseAuth!!.signInWithCredential(credential)
                .addOnCompleteListener(this, object : OnCompleteListener<AuthResult?> {
                    override fun onComplete(task: Task<AuthResult?>) {
                        Log.d("signInWith", "Credential:onComplete:" + task.isSuccessful)
                        if (task.isSuccessful) {
                            val userBean = UserBean()
                            userBean.id = firebaseAuth!!.currentUser!!.uid
                            userBean.name = account.displayName
                            userBean.email = account.email
                            userBean.profile_pic = account.photoUrl.toString()
                            if (!isExist) {
                                databaseManager!!.addUser(userBean)
                            } else {
                                databaseManager!!.updateUser(userBean)
                            }
                            googleSignInClient!!.signOut()
                            val intentLogin: Intent
                            intentLogin = Intent(this@RegisterActivity, MainActivity::class.java)
                            intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intentLogin)
                            finish()
                        } else {
                            AppConstants.showAlertDialog("Registration failed, " + task.exception!!.message, this@RegisterActivity)
                        }
                    }
                })
    }

    private fun setEdtListeners() {
        edtEmail!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                inputLayoutEmail!!.isErrorEnabled = false
                inputLayoutEmail!!.error = ""
                if (charSequence.toString().length > 0) {
                    checkEmailExists(charSequence.toString())
                } else if (charSequence.toString().length == 0) {
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
        edtName!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                inputLayoutName!!.isErrorEnabled = false
                inputLayoutName!!.error = ""
            }

            override fun afterTextChanged(editable: Editable) {}
        })
    }

    fun showProgressDialog() {
        try {
            progressDialog = ProgressDialog(this@RegisterActivity)
            progressDialog!!.max = 100
            progressDialog!!.setMessage(getString(R.string.please_wait))
            progressDialog!!.setCancelable(true)
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            if (!this@RegisterActivity.isFinishing) {
                progressDialog!!.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissProgressDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) progressDialog!!.dismiss()
    }

    companion object {
        private val SIGN_IN_REQUEST = 1
    }
}