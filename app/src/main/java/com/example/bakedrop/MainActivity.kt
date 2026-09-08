package com.example.bakedrop

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bakedrop.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isSignInMode = true
    private var isPasswordVisible = false

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ইউজার লগইন থাকলে সরাসরি ড্যাশবোর্ডে রিডাইরেক্ট
        if (auth.currentUser != null) {
            navigateToDashboard()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playEntranceAnimation()

        setupTabSwitcher()
        setupPasswordToggle()
        setupButtons()
        setupKeyboardDismiss()
    }

    private fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun playEntranceAnimation() {
        binding.headerLayout.alpha = 0f
        binding.headerLayout.translationY = -60f
        binding.headerLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.formCardLayout.alpha = 0f
        binding.formCardLayout.translationY = 80f
        binding.formCardLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setStartDelay(150)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun setupTabSwitcher() {
        binding.tvSignIn.setOnClickListener {
            if (!isSignInMode) {
                isSignInMode = true
                updateTabUI()
            }
        }

        binding.tvSignUp.setOnClickListener {
            if (isSignInMode) {
                isSignInMode = false
                updateTabUI()
            }
        }
    }

    private fun updateTabUI() {
        val skyBlue = Color.parseColor("#38B6FF")
        val deepBlue = Color.parseColor("#073578")

        if (isSignInMode) {
            binding.headerLayout.setBackgroundResource(R.drawable.bg_header_signin)
            binding.tvSignIn.setBackgroundResource(R.drawable.bg_tab_signin)
            binding.tvSignIn.setTextColor(Color.WHITE)
            binding.tvSignUp.setBackground(null)
            binding.tvSignUp.setTextColor(skyBlue)

            binding.lblFullName.visibility = View.GONE
            binding.etFullName.visibility = View.GONE
            binding.tvForgotPassword.visibility = View.VISIBLE
            binding.tvTermsPolicy.visibility = View.GONE

            binding.lblEmail.setTextColor(skyBlue)
            binding.lblPassword.setTextColor(skyBlue)

            binding.btnAction.text = "Sign In"
            binding.btnAction.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_sky_blue)
        } else {
            binding.headerLayout.setBackgroundResource(R.drawable.bg_header_signup)
            binding.tvSignUp.setBackgroundResource(R.drawable.bg_tab_signup)
            binding.tvSignUp.setTextColor(Color.WHITE)
            binding.tvSignIn.setBackground(null)
            binding.tvSignIn.setTextColor(deepBlue)

            binding.lblFullName.visibility = View.VISIBLE
            binding.etFullName.visibility = View.VISIBLE
            binding.tvForgotPassword.visibility = View.GONE
            binding.tvTermsPolicy.visibility = View.VISIBLE

            binding.lblFullName.setTextColor(deepBlue)
            binding.lblEmail.setTextColor(deepBlue)
            binding.lblPassword.setTextColor(deepBlue)

            binding.btnAction.text = "Create Account"
            binding.btnAction.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_deep_blue)
        }
    }

    private fun setupPasswordToggle() {
        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }
    }

    private fun setupButtons() {
        binding.btnAction.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (!isSignInMode && binding.etFullName.text.toString().trim().isEmpty()) {
                showAlert("Error", "Please enter your full name")
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                showAlert("Error", "Please enter your email address")
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showAlert("Error", "Invalid email format")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                showAlert("Error", "Please enter your password")
                return@setOnClickListener
            }
            if (password.length < 6) {
                showAlert("Error", "Password must be at least 6 characters long")
                return@setOnClickListener
            }

            setLoading(true)

            if (isSignInMode) {
                performFirebaseLogin(email, password)
            } else {
                val fullName = binding.etFullName.text.toString().trim()
                performFirebaseRegistration(fullName, email, password)
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            showAlert("Notice", "Google Sign-In will be configured.")
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun performFirebaseLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    navigateToDashboard()
                } else {
                    showAlert("Login Failed", task.exception?.localizedMessage ?: "Invalid email or password.")
                }
            }
    }

    private fun performFirebaseRegistration(fullName: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val user = authTask.result?.user
                    if (user != null) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()

                        user.updateProfile(profileUpdates).addOnCompleteListener {
                            val merchantData = HashMap<String, Any>()
                            merchantData["uid"] = user.uid
                            merchantData["name"] = fullName
                            merchantData["fullName"] = fullName
                            merchantData["email"] = email
                            merchantData["role"] = "merchant"
                            merchantData["balance"] = 0
                            merchantData["isVerified"] = true
                            merchantData["createdAt"] = FieldValue.serverTimestamp()

                            db.collection("users").document(user.uid).set(merchantData)
                                .addOnCompleteListener { dbTask ->
                                    setLoading(false)
                                    if (dbTask.isSuccessful) {
                                        navigateToDashboard()
                                    } else {
                                        showAlert("Warning", "Account created, but database record failed.")
                                    }
                                }
                        }
                    } else {
                        setLoading(false)
                        showAlert("Error", "Failed to retrieve user profile.")
                    }
                } else {
                    setLoading(false)
                    showAlert("Registration Failed", authTask.exception?.localizedMessage ?: "Could not create account.")
                }
            }
    }

    private fun showForgotPasswordDialog() {
        val input = EditText(this)
        input.hint = "merchant@bakedrop.com"
        input.setPadding(48, 32, 48, 32)
        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your registered email address:")
            .setView(input)
            .setPositiveButton("Send Link") { _, _ ->
                val resetEmail = input.text.toString().trim()
                if (resetEmail.isNotEmpty()) {
                    auth.sendPasswordResetEmail(resetEmail)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                showAlert("Success", "Password reset link sent to your email!")
                            } else {
                                showAlert("Error", task.exception?.localizedMessage ?: "Email not found.")
                            }
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnAction.isEnabled = !isLoading
        binding.btnAction.text = if (isLoading) "Processing..." else (if (isSignInMode) "Sign In" else "Create Account")
    }

    private fun showAlert(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupKeyboardDismiss() {
        binding.rootScrollView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            }
            false
        }
    }
}