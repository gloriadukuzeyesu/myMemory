package com.example.mymemory

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.BoardSize
import com.example.mymemory.models.MemoryGame
import com.example.mymemory.models.UserImageList
import com.example.mymemory.utils.EXTRA_BOARD_SIZE
import com.example.mymemory.utils.EXTRA_GAME_NAME
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 248
    }

    // late initialization means that these variables will be set in oncreate() not at the time of MainActivity

    private val db = Firebase.firestore
    private var gameName : String? = null
    private var customGameImage: List<String>? = null

    private lateinit var clRoot: ConstraintLayout
    private lateinit var adapter: MemoryBoardAdapter
    private lateinit var memoryGame: MemoryGame
    private lateinit var  rvBoard : RecyclerView
    private lateinit var  tvNumMoves: TextView
    private lateinit var tvNumPais: TextView


    private var boardSize: BoardSize = BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPais = findViewById(R.id.tvNumPairs)

        // set up the game.
        setUpBoard()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * when the user clicks on the refresh, you have to run the game again.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.mi_refresh -> {
                // the first if is done when we are in the middle of the game.
                if(memoryGame.getNumMoves() > 0  && !memoryGame.haveWonGame()) {
                    showAlertDialog("Quit your current game", null, View.OnClickListener {
                        setUpBoard()
                    })
                } else {
                    // set up the game again when it is not in the middle of the game
                    setUpBoard()
                    return true
                }
            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
            
            R.id.mi_custom -> {
                showCreationDialog(){

                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCreationDialog(function: () -> Unit) {
        val boardSizeView =  LayoutInflater.from(this).inflate(R.layout.dialog_baord_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your own memory board", boardSizeView, View.OnClickListener {
            // set a new value for the board size
            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
           // setUpBoard() navigate the user to the new activity where they can start selecting pictures to create their game
            // Navigate new Activity

            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
//           startActivity(intent)
            
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if(customGameName == null) {
                Log.e(TAG, "Got a null game from CreateActivity")
                return 
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener { document->
            val userImageList = document.toObject(UserImageList::class.java) // grab the list of images from fireStore
            if(userImageList?.images == null) {
                Log.e(TAG, "Invalid custom game data from FireStore")
                Snackbar.make(clRoot, "Sorry, we couldn't find any such game, '$customGameName'", Snackbar.LENGTH_LONG).show()
                return@addOnSuccessListener
            }
            // if passed the above condition. I means the game has been found (all pics for that game were retried from FireStore
            val numCards = userImageList.images.size * 2 // * 2 bcz every image in the game has to be duplicate
            boardSize = BoardSize.getByValue(numCards)
            customGameImage = userImageList.images
            setUpBoard()
            gameName = customGameName

        }.addOnFailureListener{exception ->
            Log.e(TAG, "Exception when retrieving game", exception )
        }
    }

    /**
     * create a new view that allows the user to pick the game based on various sizes
     */
    private fun showNewSizeDialog() {
       val boardSizeView =  LayoutInflater.from(this).inflate(R.layout.dialog_baord_size, null)
       val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Choose new size", boardSizeView, View.OnClickListener {
            // set a new value for the board size
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImage = null
            setUpBoard()
        })

    }

    private fun showAlertDialog(title: String, view:View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok") { _, _ -> // the underscores means we don't care about those two params for setPositiveButton()
                positiveClickListener.onClick(null)
            }.show()
    }

    private fun setUpBoard() {
        supportActionBar?.title = gameName?: getString(R.string.app_name)
        when(boardSize) {
            BoardSize.EASY -> {
                tvNumMoves.text = "Easy: 4 x 2"
                tvNumPais.text = "Pairs: 0 / 4"
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "Medium:  6 x 3"
                tvNumPais.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD -> {
                tvNumMoves.text = "Hard: 6 x 6"
                tvNumPais.text = "Pairs: 0 / 12"
            }
        }
        tvNumPais.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
        memoryGame = MemoryGame(boardSize, customGameImage)
        adapter = MemoryBoardAdapter(this, boardSize, memoryGame.cards,object: MemoryBoardAdapter.CardClickListener{
            override fun onCarClicked(position: Int) {
                Log.i(TAG, "Card Clicked $position")
                updateGameWithFlip(position)
            }
        })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth());
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateGameWithFlip(position: Int) {
        // Error checking
        if(memoryGame.haveWonGame() ) {
            // alert the user of an invalid move
            Snackbar.make(clRoot, "You already Won", Snackbar.LENGTH_LONG).show()
            return
        }

        if(memoryGame.isCardFaceUp(position)) {
            // alert the user of an invalid move
            Snackbar.make(clRoot, "Invalid Move", Snackbar.LENGTH_SHORT).show()
            return
        }
        // actually flip over the card
       if (memoryGame.flipCard(position)) {
           Log.i(TAG,"Found a match! Num pairs found ${memoryGame.numPairsFound}" )
           // do interpolation based on the pair. (change the color according to how many pairs have been found, to alert the user of their progress)

           val color = ArgbEvaluator().evaluate(
               memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
               ContextCompat.getColor(this, R.color.color_progress_none),
               ContextCompat.getColor(this, R.color.color_progress_full)
               ) as Int

           tvNumPais.setTextColor(color)
           tvNumPais.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
           if(memoryGame.haveWonGame()) {
               Snackbar.make(clRoot, "You won! congratulations", Snackbar.LENGTH_LONG).show()
           }
       }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}