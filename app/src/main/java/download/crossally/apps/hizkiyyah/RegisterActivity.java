package download.crossally.apps.hizkiyyah;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;


import download.crossally.apps.hizkiyyah.R;
import download.crossally.apps.hizkiyyah.bean.UserBean;
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager;
import download.crossally.apps.hizkiyyah.utils.AppConstants;
import download.crossally.apps.hizkiyyah.utils.SharedObjects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

public class RegisterActivity extends AppCompatActivity {

    @BindView(R.id.btnRegister)
    Button btnRegister;
    @BindView(R.id.llLogin)
    LinearLayout llLogin;
    @BindView(R.id.inputLayoutEmail)
    TextInputLayout inputLayoutEmail;
    @BindView(R.id.inputLayoutName)
    TextInputLayout inputLayoutName;
    @BindView(R.id.inputLayoutPassword)
    TextInputLayout inputLayoutPassword;
    @BindView(R.id.edtEmail)
    EditText edtEmail;
    @BindView(R.id.edtPassword)
    EditText edtPassword;
    @BindView(R.id.edtName)
    EditText edtName;
    SharedObjects sharedObjects;
    private FirebaseAuth firebaseAuth;
    DatabaseManager databaseManager;
    private DatabaseReference dfUser;
    private String TAG = "Reg";
    private ProgressDialog progressDialog;
    @BindView(R.id.btnGoogleSignIn)
    SignInButton btnGoogleSignIn;
    private GoogleSignInClient googleSignInClient;
    private static final int SIGN_IN_REQUEST = 1;
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        //Get Firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager(RegisterActivity.this);
        dfUser = FirebaseDatabase.getInstance().getReference(AppConstants.Table.USERS);
        sharedObjects = new SharedObjects(RegisterActivity.this);
        setEdtListeners();
        GoogleSignInOptions gso =  new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @OnClick({R.id.btnRegister, R.id.llLogin, R.id.btnGoogleSignIn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnGoogleSignIn:
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, SIGN_IN_REQUEST);
                break;
            case R.id.btnRegister:

                SharedObjects.hideKeyboard(btnRegister, RegisterActivity.this);
                if (SharedObjects.isNetworkConnected(RegisterActivity.this)) {

                    if (!validateName()) {
                        return;
                    }
                    if (!validateEmail()) {
                        return;
                    }
                    if (isExist) {
                        inputLayoutEmail.setErrorEnabled(true);
                        inputLayoutEmail.setError(getString(R.string.email_exists));
                        return;
                    }
                    if (!validatePassword()) {
                        return;
                    }

                    btnRegister.setEnabled(false);
                    checkUserLogin();

                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet), RegisterActivity.this);
                }
                break;

            case R.id.llLogin:
                Intent intentLogin;
                intentLogin = new Intent(RegisterActivity.this, LoginActivity.class);
                intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentLogin);
                finish();
                break;
        }
    }

    private void checkUserLogin() {
        showProgressDialog();
        //create user
        firebaseAuth.createUserWithEmailAndPassword(edtEmail.getText().toString().trim(), edtPassword.getText().toString().trim())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        dismissProgressDialog();
                        btnRegister.setEnabled(true);
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.e(TAG, "createUserWithEmail:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                Log.e("UserBean", "Not null");

                                UserBean userBean = new UserBean();
                                userBean.setId(user.getUid());
                                userBean.setName(edtName.getText().toString().trim());
                                userBean.setEmail(edtEmail.getText().toString().trim());

                                addUser(userBean);

                                Intent intentLogin;
                                intentLogin = new Intent(RegisterActivity.this, EmailVerificationActivity.class);
                                intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intentLogin);
                                finish();

                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            AppConstants.showAlertDialog("Registration failed, " + task.getException().getMessage(), RegisterActivity.this);
                        }
                    }
                });
    }

    private void addUser(UserBean userBean) {
        databaseManager.addUser(userBean);
    }

    public boolean validateName() {
        if (TextUtils.isEmpty(edtName.getText().toString().trim())) {
            inputLayoutName.setErrorEnabled(true);
            inputLayoutName.setError(getString(R.string.err_name));
            return false;
        }
        return true;
    }

    public boolean validateEmail() {
        if (TextUtils.isEmpty(edtEmail.getText().toString().trim())) {
            inputLayoutEmail.setErrorEnabled(true);
            inputLayoutEmail.setError(getString(R.string.errEmailRequired));
            return false;
        } else if (!AppConstants.isValidEmail(edtEmail.getText().toString().trim())) {
            inputLayoutEmail.setErrorEnabled(true);
            inputLayoutEmail.setError(getString(R.string.errValidEmailRequired));
            return false;
        }
        return true;
    }

    public boolean validatePassword() {
        if (TextUtils.isEmpty(edtPassword.getText().toString().trim())) {
            inputLayoutPassword.setErrorEnabled(true);
            inputLayoutPassword.setError(getString(R.string.errPasswordRequired));
            return false;
        } else if (edtPassword.getText().toString().trim().length() < 6) {
            inputLayoutPassword.setErrorEnabled(true);
            inputLayoutPassword.setError(getString(R.string.errPasswordTooShort));
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        finish();
    }

    boolean isExist = false;

    private boolean checkEmailExists(final String email) {
        isExist = false;
        Query query = dfUser.orderByChild("email").equalTo(email);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.e("User", "exists");
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        if (postSnapshot.getValue(UserBean.class).getEmail().equals(email)) {
                            isExist = true;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        return isExist;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGN_IN_REQUEST)
        {
            try {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null){
                    checkEmailExists(account.getEmail());

                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    firebaseAuthWithGoogle(credential, account);
                }
            } catch (ApiException e) {
                Log.w("signInResult", ":failed code=" + e.getStatusCode());
            }
        }
    }

    private void firebaseAuthWithGoogle(AuthCredential credential, GoogleSignInAccount account){

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d("signInWith", "Credential:onComplete:" + task.isSuccessful());
                        if(task.isSuccessful()){
                            UserBean userBean = new UserBean();
                            userBean.setId(firebaseAuth.getCurrentUser().getUid());
                            userBean.setName(account.getDisplayName());
                            userBean.setEmail(account.getEmail());
                            userBean.setProfile_pic(account.getPhotoUrl().toString());
                            if (!isExist){
                                databaseManager.addUser(userBean);
                            }else{
                                databaseManager.updateUser(userBean);
                            }
                            googleSignInClient.signOut();

                            Intent intentLogin;
                            intentLogin = new Intent(RegisterActivity.this, MainActivity.class);
                            intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intentLogin);
                            finish();

                        }else{
                            AppConstants.showAlertDialog("Registration failed, " + task.getException().getMessage(), RegisterActivity.this);
                        }

                    }
                });
    }

    private void setEdtListeners() {

        edtEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                inputLayoutEmail.setErrorEnabled(false);
                inputLayoutEmail.setError("");
                if (charSequence.toString().length() > 0) {
                    checkEmailExists(charSequence.toString());
                }else if (charSequence.toString().length() == 0){
                    isExist = false ;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        edtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                inputLayoutPassword.setErrorEnabled(false);
                inputLayoutPassword.setError("");
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        edtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                inputLayoutName.setErrorEnabled(false);
                inputLayoutName.setError("");
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    public void showProgressDialog() {
        try {
            progressDialog = new ProgressDialog(RegisterActivity.this);
            progressDialog.setMax(100);
            progressDialog.setMessage(getString(R.string.please_wait));
            progressDialog.setCancelable(true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            if (!RegisterActivity.this.isFinishing()) {
                progressDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }
}
