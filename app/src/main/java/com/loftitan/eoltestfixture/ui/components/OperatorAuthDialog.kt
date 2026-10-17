package com.loftitan.eoltestfixture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuErrorRed
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary

@Composable
fun OperatorAuthDialog(
    isCurrentlyLoggedIn: Boolean,
    currentOperatorId: String,
    onDismiss: () -> Unit,
    onLogin: (operatorId: String, pin: String) -> Boolean,
    onLogout: () -> Unit
) {
    var operatorIdInput by remember { mutableStateOf(if (isCurrentlyLoggedIn) currentOperatorId else "") }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .background(BambuCardSurface, RoundedCornerShape(16.dp))
                .border(1.dp, BambuCardBorder, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isCurrentlyLoggedIn) "OPERATOR SESSION" else "OPERATOR AUTHENTICATION",
                color = BambuTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isCurrentlyLoggedIn) "Currently active testing technician" else "Scan barcode badge or enter credentials to unlock controls",
                color = BambuTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (!isCurrentlyLoggedIn) {
                OutlinedTextField(
                    value = operatorIdInput,
                    onValueChange = {
                        operatorIdInput = it
                        errorMessage = null
                    },
                    label = { Text("Operator ID / Badge Barcode") },
                    leadingIcon = {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = BambuAccentGreen)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BambuTextPrimary,
                        unfocusedTextColor = BambuTextPrimary,
                        focusedBorderColor = BambuAccentGreen,
                        unfocusedBorderColor = BambuCardBorder,
                        focusedLabelColor = BambuAccentGreen,
                        unfocusedLabelColor = BambuTextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        errorMessage = null
                    },
                    label = { Text("Security PIN") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = BambuAccentGreen)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (onLogin(operatorIdInput, pinInput)) {
                            onDismiss()
                        } else {
                            errorMessage = "Please enter valid Operator ID and PIN."
                        }
                    }),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BambuTextPrimary,
                        unfocusedTextColor = BambuTextPrimary,
                        focusedBorderColor = BambuAccentGreen,
                        unfocusedBorderColor = BambuCardBorder,
                        focusedLabelColor = BambuAccentGreen,
                        unfocusedLabelColor = BambuTextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = BambuErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BambuCardBorder)
                    ) {
                        Text("CANCEL", color = BambuTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (onLogin(operatorIdInput, pinInput)) {
                                onDismiss()
                            } else {
                                errorMessage = "Please enter valid Operator ID and PIN."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BambuAccentGreen,
                            contentColor = BambuSlateBackground
                        )
                    ) {
                        Text("AUTHENTICATE", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(
                    text = "Operator ID: $currentOperatorId",
                    color = BambuAccentGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            onLogout()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BambuErrorRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BambuErrorRed)
                    ) {
                        Text("LOGOUT")
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BambuAccentGreen,
                            contentColor = BambuSlateBackground
                        )
                    ) {
                        Text("CLOSE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
