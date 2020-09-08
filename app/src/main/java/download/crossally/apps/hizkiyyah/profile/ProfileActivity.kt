package download.crossally.apps.hizkiyyah.profile

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.Settings
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
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.github.ajalt.timberkt.Timber
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.mikhaellopez.circularimageview.CircularImageView
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImageView
import download.crossally.apps.hizkiyyah.R
import download.crossally.apps.hizkiyyah.bean.UserBean
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager.OnUserAddedListener
import download.crossally.apps.hizkiyyah.profile.ProfileActivity
import download.crossally.apps.hizkiyyah.utils.AppConstants
import download.crossally.apps.hizkiyyah.utils.SharedObjects
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity(), OnUserAddedListener {
    @JvmField
    @BindView(R.id.toolbar)
    var toolbar: Toolbar? = null

    @JvmField
    @BindView(R.id.imgUser)
    var imgUser: CircularImageView? = null

    @JvmField
    @BindView(R.id.rlPickImage)
    var rlPickImage: RelativeLayout? = null

    @JvmField
    @BindView(R.id.inputLayoutName)
    var inputLayoutName: TextInputLayout? = null

    @JvmField
    @BindView(R.id.inputLayoutEmail)
    var inputLayoutEmail: TextInputLayout? = null

    @JvmField
    @BindView(R.id.edtName)
    var edtName: TextInputEditText? = null

    @JvmField
    @BindView(R.id.edtEmail)
    var edtEmail: TextInputEditText? = null

    @JvmField
    @BindView(R.id.imgBack)
    var imgBack: ImageView? = null

    @JvmField
    @BindView(R.id.txtSave)
    var txtSave: TextView? = null

    @JvmField
    @BindView(R.id.txtEmail)
    var txtEmail: TextView? = null
    var sharedObjects: SharedObjects? = null
    private var dfUsers: DatabaseReference? = null
    var mDatabaseManager: DatabaseManager? = null
    var appPermissions = arrayOf(Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    var progressDialog: ProgressDialog? = null
    private var storageReference: StorageReference? = null
    private var firebaseAuth: FirebaseAuth? = null
    var userBean: UserBean? = null
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        ButterKnife.bind(this)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
            toolbar!!.setNavigationOnClickListener { onBackPressed() }
        }

        //Get Firebase auth instance
        firebaseAuth = FirebaseAuth.getInstance()
        sharedObjects = SharedObjects(this@ProfileActivity)
        userBean = sharedObjects!!.userInfo
        dfUsers = FirebaseDatabase.getInstance().getReference(AppConstants.Table.USERS)
        mDatabaseManager = DatabaseManager(this@ProfileActivity)
        mDatabaseManager!!.setOnUserAddedListener(this)
        storageReference = FirebaseStorage.getInstance().reference
        progressDialog = ProgressDialog(this@ProfileActivity)
        setEdtListeners()
        setData()
    }

    private fun setData() {
        if (userBean != null) {
            if (!TextUtils.isEmpty(userBean!!.profile_pic)) {
                Log.e("Pic", userBean!!.profile_pic)
                Picasso.get().load(userBean!!.profile_pic).error(R.drawable.avatar).into(imgUser)
            }
            if (!TextUtils.isEmpty(userBean!!.name)) {
                edtName!!.setText(userBean!!.name)
                edtName!!.setSelection(userBean!!.name.length)
            }
            inputLayoutEmail!!.isEnabled = false
            if (!TextUtils.isEmpty(userBean!!.email)) {
                txtEmail!!.text = userBean!!.email
            } else {
                txtEmail!!.text = ""
            }
        }
    }

    private fun updateUser() {
        userBean!!.name = edtName!!.text.toString()
        mDatabaseManager!!.updateUser(userBean!!)
    }

    private fun setEdtListeners() {
        edtName!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                inputLayoutName!!.isErrorEnabled = false
                inputLayoutName!!.error = ""
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        edtEmail!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                inputLayoutEmail!!.isErrorEnabled = false
                inputLayoutEmail!!.error = ""
            }

            override fun afterTextChanged(editable: Editable) {}
        })
    }

    @OnClick(R.id.rlPickImage, R.id.imgBack, R.id.txtSave)
    fun onClick(v: View) {
        when (v.id) {
            R.id.imgBack -> onBackPressed()
            R.id.rlPickImage -> if (SharedObjects.isNetworkConnected(this@ProfileActivity)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkAppPermissions(appPermissions)) {
                        showImagePickerDialog()
                    } else {
                        requestAppPermissions(appPermissions)
                    }
                } else {
                    showImagePickerDialog()
                }
            } else {
                AppConstants.showAlertDialog(getString(R.string.err_internet), this@ProfileActivity)
            }
            R.id.txtSave -> {
                SharedObjects.hideKeyboard(txtSave!!, this@ProfileActivity)
                if (TextUtils.isEmpty(edtName!!.text.toString().trim { it <= ' ' })) {
                    inputLayoutName!!.isErrorEnabled = true
                    inputLayoutName!!.error = getString(R.string.err_name)
                } else {
                    updateUser()
                }
            }
            else -> {
            }
        }
    }

    fun checkAppPermissions(appPermissions: Array<String>): Boolean {
        //check which permissions are granted
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        for (perm in appPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm)
            }
        }

        //Ask for non granted permissions
        return if (!listPermissionsNeeded.isEmpty()) {
//            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), PERMISSION_REQUEST_CODE);
            false
        } else true
        // App has all permissions
    }

    private fun requestAppPermissions(appPermissions: Array<String>) {
        ActivityCompat.requestPermissions(this, appPermissions, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                val permissionResults = HashMap<String, Int>()
                var deniedCount = 0
                var i = 0
                while (i < grantResults.size) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        permissionResults[permissions[i]] = grantResults[i]
                        deniedCount++
                    }
                    i++
                }
                if (deniedCount == 0) {
                    Log.e("Permissions", "All permissions are granted!")
                    showImagePickerDialog()
                } else {
                    //some permissions are denied
                    for ((permName, permResult) in permissionResults) {
                        //permission is denied and never asked is not checked
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permName)) {
                            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(this@ProfileActivity)
                            materialAlertDialogBuilder.setMessage(getString(R.string.permission_msg))
                            materialAlertDialogBuilder.setCancelable(false)
                                    .setNegativeButton(getString(R.string.no)) { dialog, which -> dialog.cancel() }
                                    .setPositiveButton(getString(R.string.yes_grant_permission)) { dialog, id ->
                                        dialog.cancel()
                                        if (!checkAppPermissions(appPermissions)) {
                                            requestAppPermissions(appPermissions)
                                        }
                                    }
                            materialAlertDialogBuilder.show()
                            break
                        } else { //permission is denied and never asked is checked
                            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(this@ProfileActivity)
                            materialAlertDialogBuilder.setMessage(getString(R.string.permission_msg_never_checked))
                            materialAlertDialogBuilder.setCancelable(false)
                                    .setNegativeButton(getString(R.string.no)) { dialog, which -> dialog.cancel() }
                                    .setPositiveButton(getString(R.string.go_to_settings)) { dialog, id ->
                                        dialog.cancel()
                                        openSettings()
                                    }
                            materialAlertDialogBuilder.show()
                            break
                        }
                    }
                }
            }
        }
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", this@ProfileActivity.packageName, null)
        intent.data = uri
        startActivityForResult(intent, SETTINGS_REQUEST_CODE)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SETTINGS_REQUEST_CODE -> {
                Log.e("Settings", "onActivityResult!")
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        if (checkAppPermissions(appPermissions)) {
                            showImagePickerDialog()
                        } else {
                            requestAppPermissions(appPermissions)
                        }
                    }
                }
            }
            REQUEST_CODE_TAKE_PICTURE -> when (resultCode) {
                Activity.RESULT_OK -> {
                    showCropImageDialog(imageUri)
                }
                Activity.RESULT_CANCELED -> {
                    Timber.tag("TAKE_PICTURE").e("RESULT_CANCELED")
                }
                else -> {
                }
            }
            SELECT_FILE_GALLERY -> when (resultCode) {
                Activity.RESULT_OK -> {
                    if (data != null) {
                        val uri = data.data
                        showCropImageDialog(uri)
                    }
                }
                Activity.RESULT_CANCELED -> {
                    Log.e("FILE_GALLERY", "RESULT_CANCELED")
                }
                else -> {
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun showCropImageDialog(uri: Uri?) {
        val dialogDate = Dialog(this@ProfileActivity)
        dialogDate.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogDate.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogDate.setContentView(R.layout.dialog_crop_image)
        dialogDate.setCancelable(true)
        val window = dialogDate.window
        val wlp = window!!.attributes
        wlp.gravity = Gravity.CENTER
        wlp.flags = wlp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
        wlp.dimAmount = 0.8f
        window.attributes = wlp
        dialogDate.window!!.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val btnCrop = dialogDate.findViewById<Button>(R.id.btnCrop)
        val cropImageView: CropImageView = dialogDate.findViewById(R.id.cropImageView)
        cropImageView.setImageUriAsync(uri)
        cropImageView.guidelines = CropImageView.Guidelines.ON
        cropImageView.setAspectRatio(1, 1)
        btnCrop.setOnClickListener {
            dialogDate.dismiss()
            var bitmapNew: Bitmap? = null
            val croppedBitmap = cropImageView.croppedImage
            val maxSize = 600
            val outWidth: Int
            val outHeight: Int
            val inWidth = croppedBitmap.width
            val inHeight = croppedBitmap.height
            if (inWidth > inHeight) {
                outWidth = maxSize
                outHeight = inHeight * maxSize / inWidth
            } else {
                outHeight = maxSize
                outWidth = inWidth * maxSize / inHeight
            }
            bitmapNew = Bitmap.createScaledBitmap(croppedBitmap, outWidth, outHeight, true)
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            val fileImageSend = getOutputMediaFile(FileColumns.MEDIA_TYPE_IMAGE)
            try {
                val fo = FileOutputStream(fileImageSend)
                bitmapNew.compress(Bitmap.CompressFormat.JPEG, 100, fo)
                fo.flush()
                fo.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val uri = Uri.fromFile(fileImageSend)
            UploadImageFileToFirebaseStorage(uri)
            Log.e("Uri", uri.path!!)
        }
        dialogDate.show()
    }

    fun UploadImageFileToFirebaseStorage(uri: Uri?) {
        try {
            if (uri != null) {
                progressDialog!!.setTitle(getString(R.string.uploading_image))
                progressDialog!!.show()

                // Creating second StorageReference.
                //        StorageReference storageReference2nd = storageReference.child(SharedObjects.Storage_Path + System.currentTimeMillis() + "." + GetFileExtension(imageUri));
                val storageReference2nd = storageReference!!.child(AppConstants.Storage_Path + System.currentTimeMillis() + ".jpg")
                val uploadTask = storageReference2nd.putFile(uri)
                val urlTask = uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        // Hiding the progressDialog.
                        progressDialog!!.dismiss()
                        // Showing exception error message.
                        AppConstants.showSnackBar(task.exception!!.message, edtName)
                        throw task.exception!!
                    }

                    // Continue with the task to get the download URL
                    storageReference2nd.downloadUrl
                }.addOnCompleteListener { task ->
                    progressDialog!!.dismiss()
                    if (task.isSuccessful) {
                        val downloadUri = task.result
                        Picasso.get().load(File(uri.path)).into(imgUser)
                        Log.e("taskSnapshot", downloadUri.toString())
                        //                                userBean.setProfile_pic(taskSnapshot.getDownloadUrl().toString());
                        userBean!!.profile_pic = downloadUri.toString()
                        sharedObjects!!.setPreference(AppConstants.USER_INFO, Gson().toJson(userBean))
                        mDatabaseManager!!.updateUser(userBean!!)
                    } else {
                        // Handle failures
                        // ...
                        // Hiding the progressDialog.
                        // Showing exception error message.
//                            AppConstants.showSnackBar(exception.getMessage(),edtName);
                    }
                }

                /*// Adding addOnSuccessListener to second StorageReference.
                storageReference2nd.putFile(uri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                Picasso.get().load(new File(uri.getPath())).into(imgUser);

                                // Hiding the progressDialog after done uploading.
                                progressDialog.dismiss();

                                */
                /*Task<Uri> downloadUri = taskSnapshot.getStorage().getDownloadUrl();

                                if(downloadUri.isSuccessful()) {
                                }else{
                                    Log.e("taskSnapshot", " not success");
                                }*/
                /*

                                String generatedFilePath = taskSnapshot.getMetadata().getPath();
                                Log.e("taskSnapshot", generatedFilePath);
//                                userBean.setProfile_pic(taskSnapshot.getDownloadUrl().toString());
                                mDatabaseManager.updateUser(userBean);

                            }
                        })
                        // If something goes wrong.
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Hiding the progressDialog.
                                progressDialog.dismiss();
                                // Showing exception error message.
                                AppConstants.showSnackBar(exception.getMessage(),edtName);
                            }
                        })
                        // On progress change upload time.
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                progressDialog.setTitle(getString(R.string.uploading_image));
                            }
                        });*/
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showImagePickerDialog() {
        val dialog = Dialog(this@ProfileActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(R.layout.dialog_image_picker)
        dialog.setCancelable(true)
        val window = dialog.window
        val wlp = window!!.attributes
        wlp.gravity = Gravity.CENTER
        wlp.flags = wlp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
        wlp.dimAmount = 0.8f
        window.attributes = wlp
        dialog.window!!.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val llGallery = dialog.findViewById<View>(R.id.llGallery) as LinearLayout
        val llCamera = dialog.findViewById<View>(R.id.llCamera) as LinearLayout
        val imgClose = dialog.findViewById<View>(R.id.imgClose) as ImageView
        val mediaStorageDir = File(Environment.getExternalStorageDirectory(), AppConstants.IMAGE_DIRECTORY_NAME)
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs()
            Log.e("Dir", "not exist")
        } else {
            Log.e("Dir", "exist")
        }
        llGallery.setOnClickListener {
            dialog.dismiss()
            galleryIntent()
        }
        llCamera.setOnClickListener {
            dialog.dismiss()
            takePicture()
        }
        imgClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    var imageUri: Uri? = null
    private fun galleryIntent() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(Intent.createChooser(intent, "Select Image"), SELECT_FILE_GALLERY)
    }

    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageUri = getOutputMediaFileUri(FileColumns.MEDIA_TYPE_IMAGE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE)
    }

    fun getOutputMediaFileUri(type: Int): Uri {
        return Uri.fromFile(getOutputMediaFile(type))
    }

    private fun getOutputMediaFile(type: Int): File? {
        val mediaStorageDir = File(Environment.getExternalStorageDirectory(), AppConstants.IMAGE_DIRECTORY_NAME)
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs()
            Log.e("Dir", "not exist")
        } else {
            Log.e("Dir", "exist")
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val mediaFile: File
        mediaFile = if (type == FileColumns.MEDIA_TYPE_IMAGE) {
            File(mediaStorageDir.path + File.separator + "IMG_" + timeStamp + ".jpg")
        } else {
            return null
        }
        return mediaFile
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onSuccess() {
        AppConstants.showSnackBar("Profile updated successfully", edtName)
    }

    override fun onFail() {
        AppConstants.showSnackBar("Couldn't updated profile", edtName)
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 10001
        private const val SETTINGS_REQUEST_CODE = 10002
        const val REQUEST_CODE_TAKE_PICTURE = 2222
        const val SELECT_FILE_GALLERY = 1111
    }
}