package com.example.myapplication.navigation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.navigation.model.ContentDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.storage
import java.text.SimpleDateFormat
import java.util.Date

class AddPhotoActivity : ComponentActivity() {

    private val PICK_IMAGE_FROM_ALBUM = 0
    private val REQUEST_PERMISSION = 1001

    private var photoUri: Uri? = null
    private val storage = Firebase.storage

    private lateinit var addphotoBtnUpload: Button
    private lateinit var addphotoImage: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var  addphoto_edit_explain: EditText

    private fun debugStorageInfo() {
        val storage = Firebase.storage
        val rootRef = storage.reference

        Log.d("AddPhotoActivity", "🔥 rootRef = $rootRef")
        Log.d("AddPhotoActivity", "🔥 bucket = ${rootRef.bucket}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)
        debugStorageInfo()

        // View 초기화
        addphotoBtnUpload = findViewById(R.id.addphoto_btn_upload)
        addphotoImage = findViewById(R.id.addphoto_image)
        addphoto_edit_explain =  findViewById(R.id.addphoto_edit_explain)

        // 먼저 권한 체크 → 없으면 요청, 있으면 바로 앨범 오픈
        checkAndRequestPermission()

        // 업로드 버튼 클릭 시
        addphotoBtnUpload.setOnClickListener {
            contentUpload()
        }
    }

    private fun checkAndRequestPermission() {
        // 안드로이드 버전에 따라 권한 이름 다름
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val granted = ContextCompat.checkSelfPermission(this, permission)

        if (granted == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // 이미 권한 있음 → 바로 앨범 열기
            openAlbum()
        } else {
            // 권한 요청
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                // 권한 허용됨 → 앨범 열기
                openAlbum()
            } else {
                Toast.makeText(this, "사진 권한을 허용해야 업로드할 수 있습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun openAlbum() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"   // 소문자!
        }
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_FROM_ALBUM) {
            if (resultCode == Activity.RESULT_OK) {
                photoUri = data?.data
                addphotoImage.setImageURI(photoUri)
            } else {
                // 선택 안 하고 나온 경우
                finish()
            }
        }
    }

    private fun contentUpload() {
        if (photoUri == null) {
            Toast.makeText(this, "먼저 이미지를 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 파일 이름용(문자열)
        val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "IMAGE_${fileTimestamp}_.png"

        val storageRef = storage.reference
            .child("images")
            .child(imageFileName)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        storageRef.putFile(photoUri!!)
            .continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                return@continueWithTask storageRef.downloadUrl
            }.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    var contentDTO = ContentDTO()

                    //Insert downloadURL
                    contentDTO.imageUrl = uri.toString()

                    //Insert uid of user
                    contentDTO.uid = auth.currentUser?.uid

                    //Insert userId
                    contentDTO.userId = auth.currentUser?.email

                    //Insert explain of content
                    contentDTO.explain = addphoto_edit_explain.text.toString()

                    //Insert timestamp
                    contentDTO.timestamp = System.currentTimeMillis()

                    firestore.collection("images").document().set(contentDTO)

                    setResult(Activity.RESULT_OK)

                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "업로드 실패: ${it.message}", Toast.LENGTH_LONG).show()
            }
        /*
        //Callback method
        storageRef.putFile(photoUri!!)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    var contentDTO = ContentDTO()

                    //Insert downloadURL
                    contentDTO.imageUrl = uri.toString()

                    //Insert uid of user
                    contentDTO.uid = auth.currentUser?.uid

                    //Insert userId
                    contentDTO.userId = auth.currentUser?.email

                    //Insert explain of content
                    contentDTO.explain = addphoto_edit_explain.text.toString()

                    //Insert timestamp
                    contentDTO.timestamp = System.currentTimeMillis()

                    firestore.collection("images").document().set(contentDTO)

                    setResult(Activity.RESULT_OK)

                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "업로드 실패: ${it.message}", Toast.LENGTH_LONG).show()
            }

         */

    }
}
