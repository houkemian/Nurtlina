package com.nurtlina.app.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.nurtlina.app.R
import com.nurtlina.app.ui.theme.NurtlinaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onSignInSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SignInViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity: Activity? = context.findActivity()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val firebaseWebClientId = stringResource(R.string.firebase_web_client_id)
    val googleNotConfiguredMessage = stringResource(R.string.signin_error_google_not_configured)

    // Navigate back after successful sign-in
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { onSignInSuccess() }
    }

    // Show snackbar (e.g. password reset sent)
    val snackbarMessage = uiState.snackbarMessage
    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            viewModel.clearSnackbar()
        }
    }

    // ── Google Sign-In handler ────────────────────────────────────────────────
    val googleSignInClient = remember(context, firebaseWebClientId) {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(firebaseWebClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, options)
    }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        handleGoogleSignInResult(
            context = context,
            data = result.data,
            onSuccess = viewModel::signInWithGoogle,
            onError = viewModel::onGoogleSignInError,
        )
    }
    val onGoogleSignIn: () -> Unit = {
        if (firebaseWebClientId == FIREBASE_WEB_CLIENT_ID_PLACEHOLDER) {
            viewModel.onGoogleSignInError(googleNotConfiguredMessage)
        } else {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    // ── Microsoft Sign-In handler ─────────────────────────────────────────────
    val onMicrosoftSignIn: () -> Unit = {
        if (activity == null) {
            viewModel.onMicrosoftSignInUnavailable()
        } else {
            val act: Activity = activity
            viewModel.signInWithMicrosoft(act)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.signin_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

            // ── Header ────────────────────────────────────────────────────────
            Icon(
                imageVector = Icons.Outlined.Backup,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.signin_header_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.signin_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            // ── Provider buttons ──────────────────────────────────────────────
            GoogleSignInButton(
                onClick = onGoogleSignIn,
                enabled = !uiState.isLoading,
            )

            Spacer(Modifier.height(12.dp))

            MicrosoftSignInButton(
                onClick = onMicrosoftSignIn,
                enabled = !uiState.isLoading,
            )

            Spacer(Modifier.height(24.dp))

            // ── Divider ───────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.signin_or_divider),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // ── Email / Password form ──────────────────────────────────────────
            EmailPasswordForm(
                isCreateMode = uiState.isCreateMode,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onSignIn = { email, password ->
                    focusManager.clearFocus()
                    viewModel.signInWithEmail(email.trim(), password)
                },
                onCreateAccount = { email, password ->
                    focusManager.clearFocus()
                    viewModel.createAccountWithEmail(email.trim(), password)
                },
                onForgotPassword = { email ->
                    focusManager.clearFocus()
                    viewModel.sendPasswordResetEmail(email.trim())
                },
                onToggleMode = { viewModel.toggleCreateMode() },
                onDismissError = { viewModel.clearError() },
            )

            Spacer(Modifier.height(24.dp))

            // ── Disclaimer ────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.signin_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Provider buttons ─────────────────────────────────────────────────────────

@Composable
private fun GoogleSignInButton(onClick: () -> Unit, enabled: Boolean) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Image(
            painter = painterResource(R.drawable.google_logo),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.signin_google),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun MicrosoftSignInButton(onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF0078D4),
            contentColor = Color.White,
        ),
    ) {
        Image(
            painter = painterResource(R.drawable.microsoft_logo),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.signin_microsoft),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

// ── Email / Password form ─────────────────────────────────────────────────────

@Composable
private fun EmailPasswordForm(
    isCreateMode: Boolean,
    isLoading: Boolean,
    error: String?,
    onSignIn: (email: String, password: String) -> Unit,
    onCreateAccount: (email: String, password: String) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onToggleMode: () -> Unit,
    onDismissError: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth()) {

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; onDismissError() },
            label = { Text(stringResource(R.string.signin_email_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
            leadingIcon = {
                Icon(Icons.Outlined.Email, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
        )

        Spacer(Modifier.height(12.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; onDismissError() },
            label = { Text(stringResource(R.string.signin_password_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (isCreateMode) onCreateAccount(email, password) else onSignIn(email, password)
                },
            ),
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            leadingIcon = {
                Icon(Icons.Outlined.Lock, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = if (passwordVisible) {
                            stringResource(R.string.signin_hide_password_cd)
                        } else {
                            stringResource(R.string.signin_show_password_cd)
                        },
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
        )

        // Error message
        AnimatedVisibility(visible = error != null) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Primary action button
        Button(
            onClick = {
                if (isCreateMode) onCreateAccount(email, password) else onSignIn(email, password)
            },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = if (isCreateMode) {
                        stringResource(R.string.signin_create_account)
                    } else {
                        stringResource(R.string.signin_submit)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Forgot password (only in sign-in mode)
        AnimatedVisibility(visible = !isCreateMode) {
            TextButton(
                onClick = { onForgotPassword(email) },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.signin_forgot_password),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // Toggle create / sign-in mode
        TextButton(
            onClick = onToggleMode,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (isCreateMode) {
                    stringResource(R.string.signin_toggle_signin)
                } else {
                    stringResource(R.string.signin_toggle_create)
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun handleGoogleSignInResult(
    context: Context,
    data: Intent?,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
) {
    try {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data)
            .getResult(ApiException::class.java)
        val idToken = account.idToken
        if (idToken != null) {
            onSuccess(idToken)
        } else {
            onError(context.getString(R.string.signin_error_google_no_token))
        }
    } catch (e: ApiException) {
        if (e.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
            onError(e.toGoogleSignInMessage(context))
        }
    }
}

private fun ApiException.toGoogleSignInMessage(context: Context): String = when (statusCode) {
    CommonStatusCodes.NETWORK_ERROR ->
        context.getString(R.string.signin_error_google_network)
    CommonStatusCodes.DEVELOPER_ERROR ->
        context.getString(R.string.signin_error_google_developer)
    GoogleSignInStatusCodes.SIGN_IN_FAILED ->
        context.getString(R.string.signin_error_google_failed)
    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS ->
        context.getString(R.string.signin_error_google_in_progress)
    else ->
        context.getString(R.string.signin_error_google_with_code, statusCode)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val FIREBASE_WEB_CLIENT_ID_PLACEHOLDER = "REPLACE_WITH_FIREBASE_WEB_CLIENT_ID"

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "Sign In – light", showBackground = true)
@Composable
private fun SignInScreenPreview() {
    NurtlinaTheme {
        // Preview without ViewModel wiring
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Sign In") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Icon(Icons.Outlined.Backup, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Sync and backup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Sign in to restore your data on any device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                GoogleSignInButton(onClick = {}, enabled = true)
                Spacer(Modifier.height(12.dp))
                MicrosoftSignInButton(onClick = {}, enabled = true)
            }
        }
    }
}

@Preview(name = "Sign In – dark", showBackground = true)
@Composable
private fun SignInScreenDarkPreview() {
    NurtlinaTheme(darkTheme = true) {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(title = { Text("Sign In") })
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                GoogleSignInButton(onClick = {}, enabled = true)
                Spacer(Modifier.height(12.dp))
                MicrosoftSignInButton(onClick = {}, enabled = true)
            }
        }
    }
}
