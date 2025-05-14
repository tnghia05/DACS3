package com.eritlab.jexmon.presentation.screens.sign_up_screen.component

import android.app.DatePickerDialog
import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.eritlab.jexmon.R
import com.eritlab.jexmon.presentation.common.CustomDefaultBtn
import com.eritlab.jexmon.presentation.common.CustomTextField
import com.eritlab.jexmon.presentation.common.component.DefaultBackArrow
import com.eritlab.jexmon.presentation.common.component.ErrorSuggestion
import com.eritlab.jexmon.presentation.graphs.auth_graph.AuthScreen
import com.eritlab.jexmon.presentation.ui.theme.TextColor
import com.eritlab.jexmon.presentation.viewmodel.SignUpState
import com.eritlab.jexmon.presentation.viewmodel.SignUpViewModel
import java.util.Calendar

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
fun SignUpScreen(navController: NavController) {
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var confirmPass by remember { mutableStateOf(TextFieldValue("")) }
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var phoneNumber by remember { mutableStateOf(TextFieldValue("")) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }
    
    val emailErrorState = remember { mutableStateOf(false) }
    val passwordErrorState = remember { mutableStateOf(false) }
    val conPasswordErrorState = remember { mutableStateOf(false) }
    val nameErrorState = remember { mutableStateOf(false) }
    val phoneNumberErrorState = remember { mutableStateOf(false) }
    val birthdayErrorState = remember { mutableStateOf(false) }
    val genderErrorState = remember { mutableStateOf(false) }
    
    val animate = remember { mutableStateOf(true) }
    val context = LocalContext.current

    val viewModel: SignUpViewModel = viewModel()
    val signUpState by viewModel.signUpState.collectAsState()

    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
        },
        year, month, day
    )

    LaunchedEffect(signUpState) {
        when (signUpState) {
            is SignUpState.OTPSent -> {
                navController.navigate(AuthScreen.EmailVerificationScreen.route) {
                    popUpTo(AuthScreen.SignUpScreen.route) { inclusive = true }
                }
            }
            is SignUpState.Success -> {
                navController.navigate(AuthScreen.SignInSuccess.route) {
                    popUpTo(AuthScreen.SignUpScreen.route) { inclusive = true }
                }
            }
            is SignUpState.Error -> {
                Toast.makeText(
                    context,
                    (signUpState as SignUpState.Error).message,
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {}
        }
    }

    AnimatedContent(targetState = animate.value, transitionSpec = {
        slideInHorizontally(
            initialOffsetX = { value -> value }
        ) with slideOutHorizontally(
            targetOffsetX = { value -> -value }
        )
    }) {
        if (it) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(0.7f)) {
                        DefaultBackArrow {
                            navController.popBackStack()
                        }
                    }
                    Box(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = "Đăng Ký",
                            color = MaterialTheme.colors.TextColor,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(50.dp))
                Text(text = "Đăng Ký Tài Khoản", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Hoàn thành thông tin của bạn",
                    color = MaterialTheme.colors.TextColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(50.dp))

                // Email field
                CustomTextField(
                    placeholder = "example@email.com",
                    trailingIcon = R.drawable.mail,
                    label = "Email",
                    errorState = emailErrorState,
                    keyboardType = KeyboardType.Email,
                    visualTransformation = VisualTransformation.None,
                    onChanged = { newEmail -> email = newEmail }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Password field
                CustomTextField(
                    placeholder = "********",
                    trailingIcon = R.drawable.lock,
                    label = "Mật khẩu",
                    keyboardType = KeyboardType.Password,
                    errorState = passwordErrorState,
                    visualTransformation = PasswordVisualTransformation(),
                    onChanged = { newPass -> password = newPass }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Confirm Password field
                CustomTextField(
                    placeholder = "********",
                    trailingIcon = R.drawable.lock,
                    label = "Xác nhận mật khẩu",
                    keyboardType = KeyboardType.Password,
                    errorState = conPasswordErrorState,
                    visualTransformation = PasswordVisualTransformation(),
                    onChanged = { newPass -> confirmPass = newPass }
                )

                // Error messages
                Spacer(modifier = Modifier.height(10.dp))
                if (emailErrorState.value) {
                    ErrorSuggestion("Vui lòng nhập email hợp lệ")
                }
                if (passwordErrorState.value) {
                    ErrorSuggestion("Mật khẩu phải có ít nhất 8 ký tự")
                }
                if (conPasswordErrorState.value) {
                    ErrorSuggestion("Mật khẩu xác nhận không khớp")
                }

                CustomDefaultBtn(shapeSize = 50f, btnText = "Tiếp tục") {
                    val pattern = Patterns.EMAIL_ADDRESS
                    val isEmailValid = pattern.matcher(email.text).matches()
                    val isPassValid = password.text.length >= 8
                    val conPassMatch = password == confirmPass
                    emailErrorState.value = !isEmailValid
                    passwordErrorState.value = !isPassValid
                    conPasswordErrorState.value = !conPassMatch
                    if (isEmailValid && isPassValid && conPassMatch) {
                        animate.value = !animate.value
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(0.7f)) {
                        DefaultBackArrow {
                            animate.value = !animate.value
                        }
                    }
                    Box(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = "Thông tin cá nhân",
                            color = MaterialTheme.colors.TextColor,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(50.dp))
                Text(text = "Thông tin cá nhân", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Điền thông tin của bạn",
                    color = MaterialTheme.colors.TextColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(50.dp))

                // Name field
                CustomTextField(
                    placeholder = "Nhập họ tên của bạn",
                    trailingIcon = R.drawable.user,
                    label = "Họ và tên",
                    errorState = nameErrorState,
                    keyboardType = KeyboardType.Text,
                    visualTransformation = VisualTransformation.None,
                    onChanged = { newText -> name = newText }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Birthday field
                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (selectedDate.isEmpty()) "Chọn ngày sinh" else selectedDate,
                        color = if (selectedDate.isEmpty()) MaterialTheme.colors.TextColor.copy(alpha = 0.5f) 
                               else MaterialTheme.colors.TextColor
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Gender selection
                var expanded by remember { mutableStateOf(false) }
                val genders = listOf("Nam", "Nữ")

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedGender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Giới tính") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        genders.forEach { gender ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedGender = gender
                                    expanded = false
                                }
                            ) {
                                Text(text = gender)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Phone number field
                CustomTextField(
                    placeholder = "Nhập số điện thoại",
                    trailingIcon = R.drawable.phone,
                    label = "Số điện thoại",
                    keyboardType = KeyboardType.Phone,
                    errorState = phoneNumberErrorState,
                    visualTransformation = VisualTransformation.None,
                    onChanged = { newNumber -> phoneNumber = newNumber }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Error messages
                if (nameErrorState.value) {
                    ErrorSuggestion("Vui lòng nhập họ tên")
                }
                if (birthdayErrorState.value) {
                    ErrorSuggestion("Vui lòng chọn ngày sinh")
                }
                if (genderErrorState.value) {
                    ErrorSuggestion("Vui lòng chọn giới tính")
                }
                if (phoneNumberErrorState.value) {
                    ErrorSuggestion("Vui lòng nhập số điện thoại hợp lệ")
                }

                Spacer(modifier = Modifier.height(20.dp))

                CustomDefaultBtn(shapeSize = 50f, btnText = "Đăng ký") {
                    val isNameValid = name.text.isNotEmpty() && name.text.length >= 3
                    val isPhoneValid = phoneNumber.text.isNotEmpty() && phoneNumber.text.length >= 10
                    val isBirthdayValid = selectedDate.isNotEmpty()
                    val isGenderValid = selectedGender.isNotEmpty()

                    nameErrorState.value = !isNameValid
                    phoneNumberErrorState.value = !isPhoneValid
                    birthdayErrorState.value = !isBirthdayValid
                    genderErrorState.value = !isGenderValid

                    if (isNameValid && isPhoneValid && isBirthdayValid && isGenderValid) {
                        viewModel.signUp(
                            email = email.text,
                            password = password.text,
                            name = name.text,
                            phoneNumber = phoneNumber.text,
                            birthday = selectedDate,
                            gender = selectedGender
                        )
                    }
                }


                // Thêm nút kiểm tra xác minh email
                if (signUpState is SignUpState.OTPSent) {
                    Spacer(modifier = Modifier.height(20.dp))
                    CustomDefaultBtn(
                        shapeSize = 50f,
                        btnText = "Đã xác minh email"
                    ) {
                        viewModel.checkEmailVerification()
                    }
                }
            }
        }
    }
}


