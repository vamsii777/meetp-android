package download.crossally.apps.hizkiyyah.videoplay

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.adcolony.sdk.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import download.crossally.apps.hizkiyyah.R
import download.crossally.apps.hizkiyyah.utils.AppConstants.Table.VIDEO_LIST
import timber.log.Timber


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class VideoPlayFullscreenActivity : AppCompatActivity() {
    lateinit var fullscreenContent: YouTubePlayerView
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler()
    var finallink: String? = null
    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreenContentControls.visibility = View.VISIBLE
    }
    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val delayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    //private var mdatabasereference: DatabaseReference? = null

    private val APP_ID = "app077f4ff2c8f441c091"
    private val ZONE_ID = "vz284168965bc44f768d"
    private val TAG = "AdColony"

    private var adview: AdColonyInterstitial? = null
    private var listener: AdColonyInterstitialListener? = null
    private var adOptions: AdColonyAdOptions? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_video_play_fullscreen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isFullscreen = true

        // Set up the user interaction to manually show or hide the system UI.
        fullscreenContent = findViewById(R.id.andExoPlayerView)
        //fullscreenContent.setOnClickListener { toggle() }
        fullscreenContentControls = findViewById(R.id.fullscreen_content_controls)


        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById<Button>(R.id.dummy_button).setOnTouchListener(delayHideTouchListener)

        val playsessionId = intent.getStringExtra("EXTRA_DBKEY_ID")
        val fire = FirebaseDatabase.getInstance().reference.child(VIDEO_LIST).child(playsessionId!!)

        val appOptions: AdColonyAppOptions = AdColonyAppOptions()
                .setUserID(FirebaseAuth.getInstance().currentUser!!.uid.toString())
                .setKeepScreenOn(true)


        AdColony.configure(this, appOptions, APP_ID, ZONE_ID)

        fire.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                finallink = dataSnapshot.child("videourl").getValue(String::class.java)
                val finaltitle = dataSnapshot.child("title").getValue(String::class.java)
                Log.d("videoLink", " data : $finallink")
                //fullscreenContent.setSource(finallink)
                //fullscreenContent.setSource(Uri.parse(finallink))
                //fullscreenContent.setAutoPlay(true)
                lifecycle.addObserver(fullscreenContent)

                fullscreenContent.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(@NonNull youTubePlayer: YouTubePlayer) {
                        val videoId = finallink
                        youTubePlayer.loadVideo(videoId.toString(), 0f)
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        listener = object : AdColonyInterstitialListener() {
            override fun onRequestFilled(ad: AdColonyInterstitial) {
                // Ad passed back in request filled callback, ad can now be shown
                adview = ad
                Log.d(TAG, "onRequestFilled")
            }

            override fun onRequestNotFilled(zone: AdColonyZone) {
                // Ad request was not filled
                Log.d(TAG, "onRequestNotFilled")
            }

            override fun onOpened(ad: AdColonyInterstitial) {
                // Ad opened, reset UI to reflect state change
                Log.d(TAG, "onOpened")
            }

            override fun onExpiring(ad: AdColonyInterstitial) {
                // Request a new ad if ad is expiring
                AdColony.requestInterstitial(ZONE_ID, this, adOptions)
                Log.d(TAG, "onExpiring")
            }


            override fun onClosed(ad: AdColonyInterstitial?) {
                super.onClosed(ad)
            }
        }

        adview?.show()

        Timber.tag("VideoPlayID?:").d(playsessionId!!)


        Timber.tag("playbackId?:").d(finallink)
    }

    override fun onResume() {
        super.onResume()
        if (adview == null || adview!!.isExpired()) {
            AdColony.requestInterstitial(ZONE_ID, listener!!, adOptions);
        }
    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    override fun onPause() {
        super.onPause()
        // Make sure the player stops playing if the user presses the home button.
        //fullscreenContent.pause()
    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreenContentControls.visibility = View.GONE
        isFullscreen = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        isFullscreen = true

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }
}