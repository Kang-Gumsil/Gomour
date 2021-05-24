package com.santaistiger.gomourcustomerapp.ui.view

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.santaistiger.gomourcustomerapp.R
import com.santaistiger.gomourcustomerapp.data.model.Customer
import com.santaistiger.gomourcustomerapp.databinding.FragmentJoinBinding
import com.santaistiger.gomourcustomerapp.ui.base.BaseActivity
import com.santaistiger.gomourcustomerapp.ui.viewmodel.JoinViewModel
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.fragment_join.*
import java.util.regex.Pattern


class JoinFragment : Fragment() {
    private var auth: FirebaseAuth? = null
    private lateinit var binding: FragmentJoinBinding
    private lateinit var viewModel: JoinViewModel
    val db = Firebase.firestore


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        setToolbar()

        auth = Firebase.auth
        binding = DataBindingUtil.inflate<FragmentJoinBinding>(
            inflater,
            R.layout.fragment_join, container, false
        )
        viewModel = ViewModelProvider(this).get(JoinViewModel::class.java)

        binding.apply {

            // 비어 있지 않을 때 이메일 중복확인 버튼 나오게하기
            emailEditText.addTextChangedListener(emailChangeWatcher)

            // 비어 있지 않을 때 비밀번호 감지하여 판단
            passwordCheckEditText.addTextChangedListener(passwordCheckChangeWatcher)
            passwordEditText.addTextChangedListener(passwordChangeWatcher)

            // 가입버튼을 위해 비어있는지 확인
            emailEditText.addTextChangedListener(mTextWatcher)
            nameEditText.addTextChangedListener(mTextWatcher)
            passwordCheckEditText.addTextChangedListener(mTextWatcher)
            passwordEditText.addTextChangedListener(mTextWatcher)
            PhoneEditText.addTextChangedListener(mTextWatcher)
        }


        //이메일 중복 버튼
        binding.emailCheckButton.setOnClickListener {

            checkEmail(object : EmailCallback {
                override fun isEmailExist(exist: Boolean) {
                    if (exist) {
                        // 이미 사용중인 이메일
                        binding.emailValid.visibility = View.VISIBLE
                        binding.emailValid.setTextColor(Color.parseColor("#FFF44336")) //레드
                        binding.emailValid.setText(R.string.join_email_duplicate_info)


                    } else {
                        // 사용가능한 이메일
                        binding.emailValid.visibility = View.VISIBLE
                        binding.emailValid.setTextColor(Color.parseColor("#000000"))
                        binding.emailValid.setText(R.string.join_email_available_info)

                    }
                }
            })
        }

        //회원가입버튼
        binding.signUpButton.setOnClickListener {
            val mInputMethodManager: InputMethodManager =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            mInputMethodManager.hideSoftInputFromWindow(
                PhoneEditText.getWindowToken(),
                0
            )

            val email: String = binding.emailEditText.text.toString()
            val password: String = binding.passwordEditText.text.toString()
            val passwordCheck: String = binding.passwordCheckEditText.text.toString()
            val name: String = binding.nameEditText.text.toString()
            val phone = binding.PhoneEditText.text.toString()
            //uid는 회원가입완료 후 생성됨. 일단은 임시로 ..!
            val uid = ""
            var customer = Customer(email, password, name, phone, uid)

            //이메일 검사 -> 비밀번호 검사 -> 계정 생성
            if(emailValidCheck()==true){
                checkEmail(object : EmailCallback {
                    override fun isEmailExist(exist: Boolean) {
                        if (exist) {
                            // 이미 사용중인 이메일입니다.
                            binding.emailValid.visibility = View.VISIBLE
                            binding.emailValid.setTextColor(Color.parseColor("#FFF44336")) //레드
                            binding.emailValid.setText(R.string.join_email_duplicate_info)
                            Toast.makeText(context, R.string.confirm_fail, Toast.LENGTH_LONG).show()
                        } else {
                            // 사용가능 이메일
                            binding.emailValid.visibility = View.VISIBLE
                            binding.emailValid.setTextColor(Color.parseColor("#000000"))
                            binding.emailValid.setText(R.string.join_email_available_info)
                            if (passwordCheck(passwordCheck) && password(password)) {
                                createAccount(customer, passwordCheck)
                            }
                        }
                    }
                })
            }
            else{
                Toast.makeText(context,R.string.confirm_fail,Toast.LENGTH_SHORT).show()
            }



        }

        return binding.root
    }


    private fun setToolbar() {
        requireActivity().apply {
            toolbar.visibility = View.GONE    // 툴바 숨기기
            drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED) // 스와이프 비활성화
        }
    }

    // 모든 영역이 채워져있는지 있는지 감지
    private val mTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
        override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
        override fun afterTextChanged(editable: Editable) {
            checkFieldsForEmptyValues()
        }
    }

    //이메일 변경될 때 마다 인식
    private val emailChangeWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable?) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            emailValidCheck()
        }
    }

    //비밀번호 변경될때마다 인식
    private val passwordChangeWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s != null) {
                password(s)
            }
        }
    }


    //비밀번호 체크 변경될때마다 인식
    private val passwordCheckChangeWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s != null) {
                passwordCheck(s)
            }
        }
    }

    // 패스워드 제한
    private fun password(password: CharSequence): Boolean {
        val passwordCheck = binding.passwordCheckEditText.text.toString()
        val pwPattern = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@\$%^&*-]).{8,16}\$"
        if (Pattern.matches(pwPattern, password)) {
            if (password == passwordCheck) {
                // 사용할 수 있는 비밀번호 입니다.
                binding.passwordValid.visibility = View.VISIBLE
                binding.passwordValid.setTextColor(Color.parseColor("#000000"))
                binding.passwordValid.setText(R.string.password_available_info)
            }

            else {
                // 비밀번호가 같지 않을 때
                binding.passwordValid.visibility = View.VISIBLE
                binding.passwordValid.setTextColor(Color.parseColor("#FFF44336"))
                binding.passwordValid.setText(R.string.password_different_info)
            }
            return true
        }
        else {
            // 비밀번호 형식 맞지 않을떄
            binding.passwordValid.setTextColor(Color.parseColor("#FFF44336"))
            binding.passwordValid.visibility = View.VISIBLE
            binding.passwordValid.setText(R.string.password_form_info)
            return false
        }
    }


    //패스워드 체크 제한 확인
    private fun passwordCheck(s: CharSequence): Boolean {
        val password = binding.passwordEditText.text.toString()
        val pwPattern = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@\$%^&*-]).{8,16}\$"
        if (password == s.toString()) {
            if (Pattern.matches(pwPattern, password)) {
                // 사용할 수 있는 비밀번호 입니다.
                binding.passwordValid.visibility = View.VISIBLE
                binding.passwordValid.setTextColor(Color.parseColor("#000000"))
                binding.passwordValid.setText(R.string.password_available_info)
                return true
            } else {
                // 비밀번호 형식 맞지 않을떄
                binding.passwordValid.setTextColor(Color.parseColor("#FFF44336"))
                binding.passwordValid.visibility = View.VISIBLE
                binding.passwordValid.setText(R.string.password_form_info)
                return false
            }
        } else {
            // 비밀번호가 같지 않을 때
            binding.passwordValid.visibility = View.VISIBLE
            binding.passwordValid.setTextColor(Color.parseColor("#FFF44336"))
            binding.passwordValid.setText(R.string.password_different_info)
            return false
        }
    }


    //모든 영역 널이 아닌지 확인
    fun checkFieldsForEmptyValues() {
        val signUpButton = binding.signUpButton
        val email = binding.emailEditText.text.toString()
        val password: String = binding.passwordEditText.text.toString()
        val passwordCheck: String = binding.passwordCheckEditText.text.toString()
        val phone: String = binding.PhoneEditText.text.toString()

        signUpButton.isEnabled =
            !(email == "" || password == "" || passwordCheck == "" || phone == "")
    }


    //이메일 형식 체크
    private fun emailValidCheck():Boolean{
        val email = binding.emailEditText.text.toString()

        //이메일 형식이 맞지 않을 경우
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailValid.visibility = View.VISIBLE
            binding.emailValid.setTextColor(Color.parseColor("#FFF44336"))
            binding.emailValid.setText(R.string.join_email_form_info)
            emailCheckButton.isEnabled = false
            return false
        }
        //이메일 형식이 맞는 경우
        else {
            binding.emailValid.visibility = View.GONE
            //단국이메일인경우
            if (email.contains(R.string.dankook_domain.toString())) {
                binding.emailValid.visibility = View.VISIBLE
                binding.emailValid.setTextColor(Color.parseColor("#FFF44336"))
                binding.emailValid.setText(R.string.dankook_domain_info)
                emailCheckButton.isEnabled = false
                return false
            } else {
                //사용가능
                binding.emailValid.visibility = View.GONE
                emailCheckButton.isEnabled = true
                return true

            }

        }
    }


    interface EmailCallback {
        fun isEmailExist(exist: Boolean)
    }


    fun checkEmail(emailCallback: EmailCallback) {
        val db = FirebaseFirestore.getInstance()
        val email = binding.emailEditText.text.toString()
        db.collection("customer").whereEqualTo("email", email).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result?.isEmpty == true) {
                        emailCallback.isEmailExist((false)) //사용 가능
                    } else {
                        emailCallback.isEmailExist(true) //사용 불가
                    }

                }
            }
    }


    //회원가입
    private fun createAccount(customer: Customer, passwordCheck: String) {
        auth?.createUserWithEmailAndPassword(customer.email, customer.password)
            ?.addOnCompleteListener() { task ->
                if (task.isSuccessful) {
                    Log.d("TEST", "createUserWithEmail: success")
                    val user = auth!!.currentUser
                    customer.uid = user.uid
                    // Add a new document with a generated ID
                    db.collection("customer")
                        .document(customer.uid!!).set(customer)
                        .addOnSuccessListener { documentReference ->

                            // 자동로그인에 저장
                            val auto = this.requireActivity()
                                .getSharedPreferences("auto", Context.MODE_PRIVATE)
                            val autoLogin = auto.edit()
                            autoLogin.putString("email", customer.email)
                            autoLogin.putString("password", customer.password)
                            autoLogin.commit()

                            // 회원가입 완료 후 주문하기 페이지로 이동
                            findNavController().navigate(R.id.action_joinFragment_to_doOrderFragment)
                            (activity as BaseActivity).setNavigationDrawerHeader()  // 네비게이션 드로어 헤더 설정
                        }
                        .addOnFailureListener { e ->
                            Log.w("TEST", "Error adding document", e)
                        }
                } else {
                    Log.w("TEST", "createUserWithEmail: failure", task.exception)
                }
            }

    }


}