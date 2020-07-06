package download.crossally.apps.hizkiyyah;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.javiersantos.appupdater.AppUpdaterUtils;
import com.github.javiersantos.appupdater.enums.AppUpdaterError;
import com.github.javiersantos.appupdater.objects.Update;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
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

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{

    @BindView(R.id.btnLogin)
    Button btnLogin;
    @BindView(R.id.llCreateAccount)
    LinearLayout llCreateAccount;
    @BindView(R.id.inputLayoutEmail)
    TextInputLayout inputLayoutEmail;
    @BindView(R.id.inputLayoutPassword)
    TextInputLayout inputLayoutPassword;
    @BindView(R.id.edtEmail)
    EditText edtEmail;
    @BindView(R.id.edtPassword)
    EditText edtPassword;
    @BindView(R.id.txtForgotPassword) TextView txtForgotPassword;
    @BindView(R.id.btnGoogleSignIn) SignInButton btnGoogleSignIn;
    SharedObjects sharedObjects;
    private DatabaseReference dfUser;
    DatabaseManager mDatabaseManager;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    private GoogleSignInClient googleSignInClient;
    private static final int SIGN_IN_REQUEST = 1;
    DatabaseManager databaseManager;
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        ButterKnife.bind(this);

        //Get Firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager(LoginActivity.this);

        sharedObjects = new SharedObjects(LoginActivity.this);

        setEdtListeners();

        mDatabaseManager = new DatabaseManager(LoginActivity.this);
        dfUser = FirebaseDatabase.getInstance().getReference(AppConstants.Table.USERS);

        GoogleSignInOptions gso =  new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (SharedObjects.isNetworkConnected(LoginActivity.this)) {
            AppUpdaterUtils appUpdaterUtils = new AppUpdaterUtils(this)
                    .withListener(new AppUpdaterUtils.UpdateListener() {
                        @Override
                        public void onSuccess(Update update, Boolean isUpdateAvailable) {
                            if (isUpdateAvailable) {
                                launchUpdateDialog(update.getLatestVersion());
                            }
                        }

                        @Override
                        public void onFailed(AppUpdaterError error) {

                        }
                    });
            appUpdaterUtils.start();
        }
    }

    private void launchUpdateDialog(String onlineVersion) {

        try {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(LoginActivity.this);
            materialAlertDialogBuilder.setMessage("Update " + onlineVersion + " is available to download. Downloading the latest update you will get the latest features," +
                    "improvements and bug fixes of " + getString(R.string.app_name));
            materialAlertDialogBuilder.setCancelable(false).setPositiveButton(getResources().getString(R.string.update_now), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                }
            });
            materialAlertDialogBuilder.show();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @OnClick({R.id.btnLogin, R.id.llCreateAccount, R.id.txtForgotPassword, R.id.btnGoogleSignIn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnGoogleSignIn:
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, SIGN_IN_REQUEST);
                break;
            case R.id.btnLogin:
                SharedObjects.hideKeyboard(btnLogin, LoginActivity.this);
                if (SharedObjects.isNetworkConnected(LoginActivity.this)) {

                    if (!validateEmail()) {
                        return;
                    }

                    if (!validatePassword()) {
                        return;
                    }

                    btnLogin.setEnabled(false);

                    checkUserLogin();
                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet), LoginActivity.this);
                }
                break;

            case R.id.llCreateAccount:

                Intent intentLogin;
                intentLogin = new Intent(LoginActivity.this, RegisterActivity.class);
                intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentLogin);
                finish();

                break;
            case R.id.txtForgotPassword:
                if (SharedObjects.isNetworkConnected(LoginActivity.this)) {
                    showForgotPasswordDialog();
                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet), LoginActivity.this);
                }
                break;
        }
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

    boolean isExist = false;
    UserBean userBeanData = null;

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
                            userBeanData = new UserBean();
                            userBeanData = postSnapshot.getValue(UserBean.class);
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

    private void firebaseAuthWithGoogle(AuthCredential credential, GoogleSignInAccount account){

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d("signInWith", "Credential:onComplete:" + task.isSuccessful());
                        if(task.isSuccessful()){
                            UserBean userBean = new UserBean();
                            if (userBeanData != null){
                                userBean = userBeanData;
                            }
                            userBean.setId(firebaseAuth.getCurrentUser().getUid());
                            userBean.setEmail(account.getEmail());
                            userBean.setProfile_pic(account.getPhotoUrl().toString());
                            if (!isExist){
                                userBean.setName(account.getDisplayName());
                                databaseManager.addUser(userBean);
                            }else{
                                databaseManager.updateUser(userBean);
                            }
                            googleSignInClient.signOut();

                            Intent intentLogin;
                            intentLogin = new Intent(LoginActivity.this, MainActivity.class);
                            intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intentLogin);
                            finish();

                        }else{
                            Log.w("signInWith", "Credential" + task.getException().getMessage());
                            task.getException().printStackTrace();
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });
    }

    private void checkUserLogin() {
        showProgressDialog();
        //authenticate user
        firebaseAuth.signInWithEmailAndPassword(edtEmail.getText().toString().trim(), edtPassword.getText().toString().trim())
                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        btnLogin.setEnabled(true);
                        dismissProgressDialog();

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.

                        if (!task.isSuccessful()) {
                            // there was an error
                            AppConstants.showAlertDialog("Authentication failed, check your email and password or sign up", LoginActivity.this);
                        } else {

                            UserBean userBean = new UserBean();
                            userBean.setId(firebaseAuth.getCurrentUser().getUid());
                            userBean.setName("");
                            userBean.setEmail(edtEmail.getText().toString());
                            userBean.setProfile_pic("");
                            if (!isExist){
                                databaseManager.addUser(userBean);
                            }

                            Intent intentLogin;
                            if (firebaseAuth.getCurrentUser() != null && firebaseAuth.getCurrentUser().isEmailVerified()) {
                                intentLogin = new Intent(LoginActivity.this, MainActivity.class);
                            } else {
                                intentLogin = new Intent(LoginActivity.this, EmailVerificationActivity.class);
                            }
                            intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intentLogin);
                            finish();
                        }
                    }
                });
    }

    TextInputLayout inputLayoutFPEmail;
    TextInputEditText edtFPEmail;

    public void showForgotPasswordDialog() {
        final Dialog dialogDate = new Dialog(LoginActivity.this);
        dialogDate.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogDate.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogDate.setContentView(R.layout.dialog_forgot_password);
        dialogDate.setCancelable(true);

        Window window = dialogDate.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.CENTER;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        wlp.dimAmount = 0.8f;
        window.setAttributes(wlp);
        dialogDate.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        inputLayoutFPEmail = dialogDate.findViewById(R.id.inputLayoutFPEmail);

        edtFPEmail = dialogDate.findViewById(R.id.edtFPEmail);

        edtFPEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                inputLayoutFPEmail.setErrorEnabled(false);
                inputLayoutFPEmail.setError("");
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        Button btnAdd = dialogDate.findViewById(R.id.btnAdd);
        Button btnCancel = dialogDate.findViewById(R.id.btnCancel);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(edtFPEmail.getText().toString().trim())) {
                    inputLayoutFPEmail.setErrorEnabled(true);
                    inputLayoutFPEmail.setError(getString(R.string.errEmailRequired));
                    return;
                }

                if (!AppConstants.isValidEmail(edtFPEmail.getText().toString().trim())) {
                    inputLayoutFPEmail.setErrorEnabled(true);
                    inputLayoutFPEmail.setError(getString(R.string.errValidEmailRequired));
                    return;
                }

                firebaseAuth.sendPasswordResetEmail(edtFPEmail.getText().toString().trim())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {

                                    MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(LoginActivity.this);
                                    materialAlertDialogBuilder.setMessage(getString(R.string.we_have_sent_instructions));
                                    materialAlertDialogBuilder.setCancelable(false).setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            dialogDate.dismiss();
                                        }
                                    });
                                    materialAlertDialogBuilder.show();
                                } else {
                                    AppConstants.showAlertDialog(task.getException().getMessage(), LoginActivity.this);
                                }

                            }
                        });
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogDate.dismiss();
            }
        });

        if (!dialogDate.isShowing()) {
            dialogDate.show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
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
                if (!TextUtils.isEmpty(edtEmail.getText().toString().trim())){
                    checkEmailExists(edtEmail.getText().toString().trim()) ;
                }else{
                    isExist = false;
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

    public void showProgressDialog() {
        try {
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setMax(100);
            progressDialog.setMessage(getString(R.string.authenticating));
            progressDialog.setCancelable(true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            if (!LoginActivity.this.isFinishing()) {
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

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
