package com.jeeva.locationtracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.jeeva.locationtracker.databinding.ActivityViewerSetupBinding
import com.jeeva.locationtracker.util.FamilyCodeGenerator
import com.jeeva.locationtracker.util.Prefs

class ViewerSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewerSetupBinding
    private var createdCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createCodeButton.setOnClickListener { createNewFamilyCode() }
        binding.shareCodeButton.setOnClickListener { shareCode() }
        binding.continueButton.setOnClickListener { continueToMap() }
        binding.joinCodeButton.setOnClickListener { joinExistingCode() }
    }

    private fun createNewFamilyCode() {
        binding.createCodeButton.isEnabled = false
        ensureSignedIn { success ->
            if (!success) {
                binding.createCodeButton.isEnabled = true
                Toast.makeText(this, "Could not connect. Check your internet connection.", Toast.LENGTH_LONG).show()
                return@ensureSignedIn
            }

            val code = FamilyCodeGenerator.generate()
            FirebaseDatabase.getInstance().getReference("families/$code/createdAt")
                .setValue(ServerValue.TIMESTAMP)
                .addOnSuccessListener {
                    createdCode = code
                    Prefs.save(this, Prefs.Role.VIEWER, code)
                    binding.generatedCodeText.text = code
                    binding.generatedCodeText.visibility = android.view.View.VISIBLE
                    binding.shareCodeButton.visibility = android.view.View.VISIBLE
                    binding.continueButton.visibility = android.view.View.VISIBLE
                    binding.createCodeButton.isEnabled = true
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to create family code", e)
                    binding.createCodeButton.isEnabled = true
                    Toast.makeText(this, "Could not create a family code. Try again.", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun ensureSignedIn(onComplete: (Boolean) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            onComplete(true)
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e(TAG, "Anonymous sign-in failed", e)
                onComplete(false)
            }
    }

    private fun shareCode() {
        val code = createdCode ?: return
        val message = getString(R.string.viewer_setup_share_message, code)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.viewer_setup_share_code)))
    }

    private fun continueToMap() {
        startActivity(Intent(this, ViewerMapActivity::class.java))
        finish()
    }

    private fun joinExistingCode() {
        val code = FamilyCodeGenerator.normalize(binding.joinCodeInput.text?.toString().orEmpty())
        if (code.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_code, Toast.LENGTH_SHORT).show()
            return
        }
        Prefs.save(this, Prefs.Role.VIEWER, code)
        startActivity(Intent(this, ViewerMapActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "ViewerSetup"
    }
}
