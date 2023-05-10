package com.example.mymemory

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
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
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "MainActivity"
    }

    // late initialization means that these variables will be set in oncreate() not at the time of MainActivity

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
        }
        return super.onOptionsItemSelected(item)
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
        memoryGame = MemoryGame(boardSize)
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