package com.example.interviewprep.screens.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.interviewprep.R
import com.example.interviewprep.util.SocialIconButton

@Composable
fun SignUpScreen(
    modifier: Modifier = Modifier,
    onSignUp: (fullName: String, email: String, password: String, confirm: String) -> Unit,
    onGoogleClick: () -> Unit,
    onLoginClick: () -> Unit,
    loading: Boolean = false,
    errorText: String? = null,
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var pwVisible by remember { mutableStateOf(false) }
    var cpwVisible by remember { mutableStateOf(false) }

    Box(
        modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "INTERVIEW PREPARATION",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )
            }
            Text(
                "Unlock Your Potential",
                color = Color(0xFF7A869A),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
            )


            Image(
                painter = painterResource(id = R.drawable.signup), // add asset
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .padding(bottom = 12.dp),
                contentScale = ContentScale.Fit
            )


            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                singleLine = true,
                placeholder = { Text("Full Name") },
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )
            Spacer(Modifier.height(12.dp))


            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                singleLine = true,
                placeholder = { Text("Email") },
                leadingIcon = {
                    FieldIcon {
                        Icon(
                            Icons.Outlined.Email,
                            null,
                            Modifier.size(18.dp)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )
            Spacer(Modifier.height(12.dp))


            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                singleLine = true,
                placeholder = { Text("Password") },
                visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = {
                    FieldIcon {
                        Icon(
                            Icons.Outlined.Lock,
                            null,
                            Modifier.size(18.dp)
                        )
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { pwVisible = !pwVisible }) {
                        Icon(
                            imageVector = if (pwVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )
            Spacer(Modifier.height(12.dp))


            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                singleLine = true,
                placeholder = { Text("Confirm Password") },
                visualTransformation = if (cpwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = {
                    FieldIcon {
                        Icon(
                            Icons.Outlined.Lock,
                            null,
                            Modifier.size(18.dp)
                        )
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { cpwVisible = !cpwVisible }) {
                        Icon(
                            imageVector = if (cpwVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )

            if (!errorText.isNullOrBlank()) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }


            Button(
                onClick = { onSignUp(fullName.trim(), email.trim(), password, confirm) },
                enabled = !loading,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B76F6))
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("SIGN UP")
            }


            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SocialIconButton(R.drawable.google, "Google", onGoogleClick)
            }


            Spacer(Modifier.height(10.dp))
            Row {
                Text("Already have account? ", color = Color(0xFF7A869A))
                Text(
                    "Login",
                    color = Color(0xFF3B76F6),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onLoginClick)
                )
            }
        }
    }
}

@Composable
private fun FieldIcon(content: @Composable () -> Unit) {
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = Color(0xFFF7F8FA),
    focusedContainerColor = Color.White,
    unfocusedBorderColor = Color(0xFFE1E5ED),
    focusedBorderColor = Color(0xFFB7C2D0),
    cursorColor = Color(0xFF3B76F6)
)