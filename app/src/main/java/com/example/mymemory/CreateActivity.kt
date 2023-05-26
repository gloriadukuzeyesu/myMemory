package com.example.mymemory

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.BoardSize
import com.example.mymemory.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CreateActivity"
        private const val PICK_PHOTO_CODE = 655 // this is just a number. Doesn't mean anything
        private const val READ_EXTERNAL_PHOTOS_CODE = 248 // this is just a number. Doesn't mean anything
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MAX_GAME_NAME_LENGTH = 14
        private const val MIN_GAME_NAME_LENGTH = 3
    }

    private lateinit var adapter: ImagePickerAdapter
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var pdUploading: ProgressBar


    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1
    private var chosenImageUris = mutableListOf<Uri>() // URi is uniform resources identifier / it is a like a string that identifies where a string lives

    //*****FIREBASE STUFFS****
    private var storage = Firebase.storage // Cloud storage
    private var db = Firebase.firestore // cloud firestore





    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pdUploading = findViewById(R.id.pdUploading)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // enable users to go back to main game if they want to
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE, BoardSize::class.java)!!
        numImagesRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose pics(0 / $numImagesRequired)" // ? means do something only if it is not null


        btnSave.setOnClickListener{
            // save all the images to the data base
            saveDataToFireBase()
        }


        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH)) // the game name should only be of 14 character long or 3 as defined in the shouldEnableSaveButton  method
        // handle when the name of the game is changed
        etGameName.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                btnSave.isEnabled = shouldEnableSaveButton() // in case user changes the name.
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        // a RecyclerView review has an adapter and a layout manager
        adapter = ImagePickerAdapter(this, chosenImageUris, boardSize, object: ImagePickerAdapter.ImageClickListener{
            override fun onPlaceHolderClicked() {
                if(isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)) {
                    Toast.makeText(this@CreateActivity, "permission granted", Toast.LENGTH_LONG).show()
                    launchIntentForPhotos()
                } else {
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
                    Log.i("GD", "TEST2")
                }
            }

        })

        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    // call back
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == READ_EXTERNAL_PHOTOS_CODE) {
            if (grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show()
                    launchIntentForPhotos()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "grantResults is empty, Permission denied", Toast.LENGTH_LONG).show()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    // take action when back button in clicked.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) { // home iD is created by the Android not us.
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode != PICK_PHOTO_CODE  || resultCode != Activity.RESULT_OK || data == null) {
            Log.i(TAG, "Did not get data back from the launched activity, user likely canceled the flow" )

            return
        }
        // at this point there is valid data (valid images)
        val selectedUri : Uri? =  data.data
        val clipData : ClipData? = data.clipData // means there is more than one images
        if(clipData != null) {
            // we have to verify that user didn't pick many images than what the recyler view can hold
            Log.i(TAG, "ClipData numImages ${clipData.itemCount}:$clipData")
            for(i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                if(chosenImageUris.size < numImagesRequired) {
                    chosenImageUris.add(clipItem.uri)
                }
            }


        } else {
            // meaning there is only one image selected
            Log.i(TAG, "data: $selectedUri")
            if (selectedUri != null) {
                chosenImageUris.add(selectedUri)
            }
        }

        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose Pics (${chosenImageUris.size} / $numImagesRequired)"
        btnSave.isEnabled = shouldEnableSaveButton()


    }

    private fun shouldEnableSaveButton(): Boolean {
         //check if we should enable the SAVE button or not
        // two condition (all required images should be picked up, 2nd the game name should be picked and has to be valid
        if(chosenImageUris.size != numImagesRequired) {
            Toast.makeText(this, "Not enough images", Toast.LENGTH_LONG).show()
            return false;
        }
        if(etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_NAME_LENGTH) {
            Toast.makeText(this, "Game name is empty or Invalid", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
       val intent = Intent(Intent.ACTION_PICK) // pick an item from data and return what was created
        intent.type = "image/*" // means we only want image types. No videos, pdf,etc
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // if we can selected multiple images at the same time
        startActivityForResult(Intent.createChooser(intent, "Choose Pics"), PICK_PHOTO_CODE)
//        startActivity(Intent.createChooser(intent, "Choose Pics"))
    }

    //*****************************************************************************************************************************************//
    //************************************************************FIRE BASE STUFFS ************************************************************//
    //*****************************************************************************************************************************************//
    private fun saveDataToFireBase() {
        Log.i(TAG, "Save Data to fireBase")
        btnSave.isEnabled = false
        val customGameName = etGameName.text.toString()
        // check that we are not over writing someone else's data
        db.collection("games").document(customGameName).get().addOnSuccessListener { document->
            if(document != null && document.data != null) {
                AlertDialog.Builder(this)
                    .setTitle("Name taken")
                    .setMessage("A game already exists with the name '$customGameName'. Please choose a different one")
                    .setPositiveButton("OK", null)
                    .show()
                btnSave.isEnabled = true // allow the user to save another game
            } else {
                handleImageUploading(customGameName)
            }
        }.addOnFailureListener{exception->
            Log.e(TAG, "Encountered error while saving memory game", exception)
            Toast.makeText(this, "Encountered error while saving memory game", Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = true
        }
    }

    private fun handleImageUploading(gameName: String) {
        pdUploading.visibility = View.VISIBLE // show the user that the uploading is happening. IN case images takes long to upload or when  user is uploading lots of images

        var didEncounterError = false
        val uploadedIMageUrl = mutableListOf<String>()
        for((index,photoUri) in chosenImageUris.withIndex()) {
            val imageByteArray: ByteArray = getImageByteArray(photoUri)
            // upload in firebase storage
            val filePath = "images/$gameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference = storage.reference.child(filePath) // location where to save this photo
            val success = photoReference.putBytes(imageByteArray) // put the image byte array data at that location.  // bcz this is asynchronous act. we need a way to find if all images have been uploaded in the storage. we can't control that. need a lambda function. Similar to parallel programming
                .continueWithTask { photoUploadTask ->
                    Log.i(TAG, "Uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                    // continued with taks
                    photoReference.downloadUrl
                }.addOnCompleteListener{downUrlTask ->
                    if(!downUrlTask.isSuccessful) {
                        Log.i(TAG,"Exception with Firebase storage", downUrlTask.exception)
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if(didEncounterError) {
                        // some other image has failed to upload
                        pdUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    }

                    val downLoadUrl = downUrlTask.result.toString()
                    uploadedIMageUrl.add(downLoadUrl)
                    pdUploading.progress = uploadedIMageUrl.size * 100 / chosenImageUris.size // progress of the bar
                    Log.i(TAG, "Finish uploading $photoUri, num uploaded $index")

                    if(uploadedIMageUrl.size == chosenImageUris.size) {
                        handleImagesUploaded(gameName, uploadedIMageUrl)
                    }
                }
        }

    }

    private fun handleImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        //TODO upload this info to Firestore
        db.collection("games").document(gameName)
            .set(mapOf("images" to imageUrls))
            .addOnCompleteListener { gameCreationTask ->
                pdUploading.visibility = View.GONE
                if(!gameCreationTask.isSuccessful) {
                    Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
                    Toast.makeText(this, "Failed Game Creation", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                pdUploading.visibility = View.GONE
                Log.i(TAG, "Successfully created game $gameName")
                AlertDialog.Builder(this)
                    .setTitle("Uploaded completed! Let's play your game $gameName")
                    .setPositiveButton("OK") {_, _, ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME, gameName)
                        setResult(Activity.RESULT_OK, resultData)
                        finish()

                    }.show()
            }
    }


    /**
     * will down grade the image quality and store it in data base. Firebase can only store 5GB. We are minimizing the images size to be store in firebase
     */
    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitMap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver,photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            //if the image comes from older version
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(TAG, "Original width ${originalBitMap.width} and height${originalBitMap.height}")
        val scaledBitMap = BitMapScaler.scaleToFitHeight(originalBitMap, 250)
        Log.i(TAG, "Scaled width ${scaledBitMap.width} and height${scaledBitMap.height}")

        val byteArrayOutPutStream = ByteArrayOutputStream()
        scaledBitMap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutPutStream)
        return byteArrayOutPutStream.toByteArray()
    }

}