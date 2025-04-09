package com.example.ponger

import android.app.Dialog
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.database
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import android.util.DisplayMetrics
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.EditText
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.Math.max
import kotlin.math.floor
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


// ca-app-pub-4180004158069121~6431713497
// BANNER TYPE


class MainActivity : AppCompatActivity() {
    // Write a message to the database
    private lateinit var RLDB: DatabaseReference
    var P1_score = 0
    var P2_score = 0
    var P1: Boolean = true
    var end: Boolean = false

    lateinit var lobbyIDdisplay: TextView
    lateinit var P1_text: TextView
    lateinit var P2_text: TextView
    lateinit var delDown: FloatingActionButton
    lateinit var delUp: FloatingActionButton

    lateinit var RST_button: Button
    lateinit var Player_button: Button
    private val displayMetrics: DisplayMetrics by lazy { Resources.getSystem().displayMetrics }
    var height = displayMetrics.heightPixels
    var width = displayMetrics.widthPixels

    var normForg = Color.rgb(250,250,250)
    var normBack = Color.rgb(0,0,0)
    var serveForg = Color.rgb(250,250,250)
    var serveBack = Color.rgb(50,150,150)
    var winForg = Color.rgb(0,0,0)
    var winBack = Color.rgb(50,250,100)

    var lobbyID: Number = -1
    var Offline: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(this@MainActivity) {}
        }

        initComp()
        openDialog()
//        initRLDB()


    }

    private fun openDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.open_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(serveBack))

        val lobbyInput: EditText = dialog.findViewById(R.id.lobbyID)
        val offlineButton: Button = dialog.findViewById(R.id.offline)
        val enterLobby: Button = dialog.findViewById(R.id.joinLobby)
        val createLobby: Button = dialog.findViewById(R.id.createLobby)

        offlineButton.setOnClickListener{
            Offline = true

            getScore()
            displayScore(false)
            updateState()
            initADS()
            dialog.dismiss()
        }
        enterLobby.setOnClickListener{
            Offline = false
            lobbyID = lobbyInput.text.toString().toInt()
            Log.i("Botolog", "Got lobby ${lobbyID}")

            initRLDB()

            getScore()
            displayScore(false)
            updateState()

            dialog.dismiss()
        }
        createLobby.setOnClickListener{
            Offline = false
            lobbyID = lobbyInput.text.toString().toInt()
            Log.i("Botolog", "Got lobby ${lobbyID}")

            initRLDB()
            initLobby()

            getScore()
            displayScore()
            updateState()

            dialog.dismiss()
        }
        dialog.show()

    }

    private fun initLobby() {
        RLDB.child("${lobbyID}").child("P1").setValue(0)
        RLDB.child("${lobbyID}").child("P2").setValue(0)
        sendScore()
        displayScore()
    }

    fun initADS(){
        // Create a new ad view.
        val adView = findViewById<AdView>(R.id.adView)

        // Start loading the ad in the background.
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun getScore() {
        if (!Offline) {
            RLDB.child("${lobbyID}").child("P1").get().addOnSuccessListener {
                P1_score = it.value.toString().toInt()
            }
            RLDB.child("${lobbyID}").child("P2").get().addOnSuccessListener {
                P2_score = it.value.toString().toInt()
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        val x: Float = e.x
        val y: Float = e.y
        if (e.action == MotionEvent.ACTION_UP && !end){
            Log.i("Botolog", "$x, $y")
            Log.i("Botolog", "action: ${checkP12(y)}")
            if (!(checkP12(y) xor P1)){
                P1_score += 1
            }
            else {
                P2_score += 1
            }
            displayScore()
        }
        return true
    }
    private val postListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {

            P1_score = dataSnapshot.child("${lobbyID}").child("P1").value.toString().toInt()
            P2_score = dataSnapshot.child("${lobbyID}").child("P2").value.toString().toInt()
            displayScore(false)
        }

        override fun onCancelled(databaseError: DatabaseError) {
            // Getting Post failed, log a message
            Log.w("Botolog", "loadPost:onCancelled", databaseError.toException())
        }
    }

    private fun initRLDB(){
        Log.i("Botolog", "Runnin")
        RLDB = Firebase.database.reference
        RLDB.addValueEventListener(postListener)

    }

    fun updateServe(every: Int = 2){
        lateinit var serving: TextView
        lateinit var notServ: TextView
        if (floor(((P1_score+P2_score)/every).toDouble())%2 == 1.0)
        {
            if (!P1) {serving = P1_text; notServ = P2_text}
            else    {serving = P2_text; notServ = P1_text}
        }
        else {
            if (P1) {serving = P1_text; notServ = P2_text}
            else     {serving = P2_text; notServ = P1_text}
        }
        notServ.setTextColor(normForg)
        notServ.setBackgroundColor(normBack)
        serving.setTextColor(serveForg)
        serving.setBackgroundColor(serveBack)
    }

    fun updateWinner(P1w: Boolean){
        lateinit var winner: TextView
        lateinit var loser: TextView
        if (P1 xor P1w) {winner = P2_text; loser = P1_text}
        else            {winner = P1_text; loser = P2_text}
        loser.setTextColor(normForg)
        loser.setBackgroundColor(normBack)
        winner.setTextColor(winForg)
        winner.setBackgroundColor(winBack)
    }

    fun updateState(){
        val regWinP1 = P1_score >= 21 && P2_score < 20
        val regWinP2 = P2_score >= 21 && P1_score < 20
        val techWinP1 = P1_score <= 7 && P1_score-P2_score>=6
        val techWinP2 = P2_score <= 7 && P2_score-P1_score>=6
        val longGame = P1_score >= 20 && P2_score >= 20
        val longWinP1 = longGame && P1_score - P2_score >= 2
        val longWinP2 = longGame && P2_score - P1_score >= 2

        end = regWinP1 || regWinP2 || techWinP1 || techWinP2 || longWinP1 || longWinP2

        if (end) {
            updateWinner(regWinP1 || techWinP1 || longWinP1)
        }
        if (!end)
        {
            if (longGame){
                updateServe(1)
            }
            else {
                updateServe()
            }
        }
        if (lobbyID != -1){lobbyIDdisplay.text = "Lobby ID: ${lobbyID}"}
        else {lobbyIDdisplay.text = "Lobby ID: Offline"}
    }

    fun initComp(){
        P1_text = findViewById<TextView>(R.id.P1_score)
        P2_text = findViewById<TextView>(R.id.P2_score)
        delDown = findViewById(R.id.delDown)
        delUp = findViewById(R.id.delUp)
        RST_button = findViewById<Button>(R.id.Reset)
        delDown.setOnClickListener{
            if (P1) { P1_score = max(P1_score - 1, 0) }
            else { P2_score = max(P2_score - 1, 0) }
            displayScore()
        }
        delUp.setOnClickListener{
            if (!P1) { P1_score = max(P1_score - 1, 0) }
            else { P2_score = max(P2_score - 1, 0) }
            displayScore()
        }

        RST_button.setOnClickListener{
            P1_score = 0
            P2_score = 0
            displayScore()
        }
        Player_button = findViewById<Button>(R.id.Player)
        Player_button.setOnClickListener{
            P1 = !P1

            displayScore()
        }

        lobbyIDdisplay = findViewById(R.id.lobbyIDdisplay)

        val closeBtn = findViewById<FloatingActionButton>(R.id.close)
        closeBtn.setOnClickListener{
            System.exit(0)
        }
    }

    fun checkP12(y: Float): Boolean {
        return y>(height/2)
    }

    fun sendScore() {
        if (!Offline) {
            RLDB.child("${lobbyID}").child("P1").setValue(P1_score)
            RLDB.child("${lobbyID}").child("P2").setValue(P2_score)
        }
    }

    fun displayScore(update: Boolean = true){
//        Log.i("BotologE", "DAMN")
        updateState()
        if (P1){
            P1_text.text = P1_score.toString()
            P2_text.text = P2_score.toString()
            Player_button.text = "Player 1"
        }
        else {
            P1_text.text = P2_score.toString()
            P2_text.text = P1_score.toString()
            Player_button.text = "Player 2"
        }
        Log.i("Botolog", "P1:${P1_score}, P2:${P2_score}")
        if (update){
            sendScore()
        }
    }

}