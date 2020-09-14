package download.crossally.apps.hizkiyyah.explore


import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import download.crossally.apps.hizkiyyah.R
import download.crossally.apps.hizkiyyah.bean.VideoList
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager
import download.crossally.apps.hizkiyyah.firebase.DatabaseManager.OnDatabaseDataChanged
import download.crossally.apps.hizkiyyah.utils.AppConstants
import download.crossally.apps.hizkiyyah.utils.AppConstants.Table.VIDEO_LIST
import download.crossally.apps.hizkiyyah.utils.SharedObjects
import download.crossally.apps.hizkiyyah.videoplay.VideoPlayFullscreenActivity
import java.util.*

open class ExploreFragment : Fragment(), OnDatabaseDataChanged {
    @JvmField
    @BindView(R.id.llError)
    var llError: LinearLayout? = null

    @JvmField
    @BindView(R.id.rvids)
    var rvids: RecyclerView? = null
    private var adView: AdView? = null

    @JvmField
    @BindView(R.id.txtError)
    var txtError: TextView? = null
    var databaseManager: DatabaseManager? = null
    private var arrExplore = ArrayList<VideoList>()
    var exploreAdapter: ExploreAdapter? = null
    var sharedObjects: SharedObjects? = null


    var recyclerView: RecyclerView? = null

    var query1: Query? = null
    private var mdatabasereference: DatabaseReference? = null
    private val progressDialog: ProgressDialog? = null
    var firebaseRecyclerAdapter: FirebaseRecyclerAdapter<VideoList, ExploreViewHolder>? = null
    var mLayoutManager: LinearLayoutManager? = null
var linearLayout: LinearLayoutManager? =null
    var appPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_explore, container, false)
        ButterKnife.bind(this, view)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        sharedObjects = SharedObjects(requireActivity())
        //databaseManager = DatabaseManager(requireActivity())
        //databaseManager!!.setDatabaseManagerListener(this)
        adView = view.findViewById(R.id.adView)
        bindAdvtView()
        recyclerView = rvids
        data
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkAppPermissions(appPermissions)) {
                requestAppPermissions(appPermissions)
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
    }

    private fun bindAdvtView() {
        if (SharedObjects.isNetworkConnected(Objects.requireNonNull(requireActivity()))) {
            val adRequest = AdRequest.Builder() //                      .addTestDevice("23F1C653C3AF44D748738885C1F91FDA")
                    .build()
            adView!!.adListener = object : AdListener() {
                override fun onAdLoaded() {}
                override fun onAdClosed() {
//                Toast.makeText(getApplicationContext(), "Ad is closed!", Toast.LENGTH_SHORT).show();
                }

                override fun onAdFailedToLoad(errorCode: Int) {
                    adView!!.visibility = View.GONE
                    //                Toast.makeText(getApplicationContext(), "Ad failed to load! error code: " + errorCode, Toast.LENGTH_SHORT).show();
                }

                override fun onAdLeftApplication() {
//                Toast.makeText(getApplicationContext(), "Ad left application!", Toast.LENGTH_SHORT).show();
                }

                override fun onAdOpened() {
                    super.onAdOpened()
                }
            }
            adView!!.loadAd(adRequest)
            adView!!.visibility = View.VISIBLE
        } else {
            adView!!.visibility = View.GONE
        }
    }

    val data: Unit
        get() {
            if (sharedObjects?.userInfo() != null) {
                //databaseManager!!.getVideoList()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setExploreAdapter() {
        /*rvids!!.adapter = exploreAdapter
        if (arrExplore.size > 0) {
            exploreAdapter = ExploreAdapter(arrExplore, requireActivity())
            Timber.d(arrExplore.toString())
            exploreAdapter!!.setOnItemClickListener(object : ExploreAdapter.OnItemClickListener {
                override fun onItemClickListener(position: Int, bean2: VideoList?) {

                }

                override fun onDeleteClickListener(position: Int, bean2: VideoList?) {
                    //databaseManager!!.deleteMeetingHistory(bean2)
                }

                override fun onJoinClickListener(position: Int, bean: VideoList?) {

                }
            })
            rvids!!.visibility = View.VISIBLE
            llError!!.visibility = View.GONE
        } else {
            rvids!!.visibility = View.GONE
            llError!!.visibility = View.VISIBLE
        }*/

        mdatabasereference = FirebaseDatabase.getInstance().getReference("videoList")

    }



    override fun onDataChanged(url: String?, dataSnapshot: DataSnapshot?) {
        if (url.equals(AppConstants.Table.VIDEO_LIST, ignoreCase = true)) {
            if (this@ExploreFragment.isVisible) {
                arrExplore = ArrayList()
                //arrExplore.addAll(databaseManager!!.userExplore)
                setExploreAdapter()
            }
        }
    }

    override fun onCancelled(error: DatabaseError?) {
        if (this@ExploreFragment.isVisible) {
            arrExplore = ArrayList()
            setExploreAdapter()
        }
    }

    fun checkAppPermissions(appPermissions: Array<String>): Boolean {
        //check which permissions are granted
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        for (perm in appPermissions) {
            if (ContextCompat.checkSelfPermission(requireActivity(), perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm)
            }
        }

        //Ask for non granted permissions
        return if (!listPermissionsNeeded.isEmpty()) {
            false
        } else true
        // App has all permissions
    }

    private fun requestAppPermissions(appPermissions: Array<String>) {
        ActivityCompat.requestPermissions(requireActivity(), appPermissions, PERMISSION_REQUEST_CODE)
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
                    //invoke ur method
                } else {
                    //some permissions are denied
                    for ((permName, permResult) in permissionResults) {
                        //permission is denied and never asked is not checked
                        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permName)) {
                            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(requireActivity())
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
                            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(requireActivity())
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
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivityForResult(intent, SETTINGS_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SETTINGS_REQUEST_CODE -> {
                Log.e("Settings", "onActivityResult!")
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        if (checkAppPermissions(appPermissions)) {
                        } else {
                            requestAppPermissions(appPermissions)
                        }
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 10001
        private const val SETTINGS_REQUEST_CODE = 10002
    }

    override fun onStop() {
        super.onStop()
        firebaseRecyclerAdapter?.stopListening()
    }

    override fun onStart() {
        super.onStart()
        query1 = FirebaseDatabase.getInstance().reference.child(VIDEO_LIST).orderByChild("timestamp")
        val options: FirebaseRecyclerOptions<VideoList> = FirebaseRecyclerOptions.Builder<VideoList>()
                .setQuery(query1!!, VideoList::class.java)
                .build()
        Log.d("Options", " data : $options")
        firebaseRecyclerAdapter = object : FirebaseRecyclerAdapter<VideoList, ExploreViewHolder>(options) {
            protected override fun onBindViewHolder(videoViewHolder: ExploreViewHolder, i: Int, set_v: VideoList) {
                videoViewHolder.setname(set_v.title)
                videoViewHolder.setdesc(set_v.description)
                videoViewHolder.setimage(set_v.imageurl)
                val link: String = set_v.videourl
                Log.d("LINKDATA", " data : $link")
                videoViewHolder.itemView.setOnClickListener {
                    val vidid = getRef(i).key
                    val intent = Intent(context, VideoPlayFullscreenActivity::class.java)
                    intent.putExtra("EXTRA_DBKEY_ID", vidid)
                    startActivity(intent)
                    Log.d("id", " data : $vidid")
                }
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_videolist, parent, false)
                //progressDialog!!.dismiss()
                return ExploreViewHolder(view)
            }
        }
        firebaseRecyclerAdapter!!.startListening()
        val mLayoutManager = LinearLayoutManager(context)
        recyclerView!!.setLayoutManager(mLayoutManager)
        //val gridLayoutManager = GridLayoutManager(getApplicationContext(), 2)
        //recyclerView!!.layoutManager = gridLayoutManager
        recyclerView!!.adapter = firebaseRecyclerAdapter
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);
    }

   class ExploreViewHolder(var mView: View) : RecyclerView.ViewHolder(mView) {

        @JvmField
        @BindView(R.id.txtName)
        public var txtName: TextView? = null

        @JvmField
        @BindView(R.id.txtDesc)
        public var txtDesc: TextView? = null

        @JvmField
        @BindView(R.id.itemVimg)
        public var itemVimgg: AppCompatImageView? = null

        @JvmField
        @BindView(R.id.cardv)
        public var cardV: TextView? = null

        fun setname(name: String?) {
            val ename = mView.findViewById<View>(R.id.txtName) as TextView
            ename.text = name
        }

       fun setdesc(name: String?) {
           val ename = mView.findViewById<View>(R.id.txtDesc) as TextView
           ename.text = name
       }

        fun setimage(url: String?): String? {
            val image = mView.findViewById<View>(R.id.itemVimg) as AppCompatImageView
            Picasso.get().load(url).into(image)
            return url
        }
    }
}