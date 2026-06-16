package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.db.VaultMessage
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    // Application Entry Point - VeilCrypt
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        var initialSharedText: String? = null
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            initialSharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VeilCryptApp(initialSharedText)
                }
            }
        }
    }
}

@Composable
fun VeilCryptApp(initialSharedText: String?) {
    var currentScreen by remember { mutableStateOf(if (initialSharedText != null) "Reveal" else "Compose") }
    val vaultViewModel: VaultViewModel = viewModel()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Compose") },
                    label = { Text("Compose") },
                    selected = currentScreen == "Compose",
                    onClick = { currentScreen = "Compose" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                    label = { Text("Chat") },
                    selected = currentScreen == "Chat",
                    onClick = { currentScreen = "Chat" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LockOpen, contentDescription = "Reveal") },
                    label = { Text("Reveal") },
                    selected = currentScreen == "Reveal",
                    onClick = { currentScreen = "Reveal" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentScreen == "Settings",
                    onClick = { currentScreen = "Settings" }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentScreen) {
                "Compose" -> ComposeScreen(vaultViewModel)
                "Chat" -> ChatScreen(vaultViewModel)
                "Reveal" -> RevealScreen(initialSharedText, vaultViewModel)
                "Settings" -> SettingsScreen(vaultViewModel)
            }
        }
    }
}

@Composable
fun ComposeScreen(vaultViewModel: VaultViewModel) {
    val context = LocalContext.current
    var visibleText by remember { mutableStateOf("") }
    var hiddenText by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var carrierType by remember { mutableStateOf("Text") }
    var showHiddenField by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var carrierUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var carrierFileName by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        carrierUri = uri
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) carrierFileName = c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
            }
            if (carrierFileName == null) carrierFileName = uri.lastPathSegment
            
            visibleText = "[Attached Media: $carrierFileName] - Stego payload ready"
            scope.launch { snackbarHostState.showSnackbar("Media attached.") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(Color.Green))
                        Text("Encryption Ready", fontSize = 11.sp, color = Color.LightGray)
                    }
                    Text("Robustness: 92%", fontSize = 11.sp, color = Color.LightGray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (password.isEmpty() || visibleText.isEmpty() || hiddenText.isEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar("All fields required") }
                                return@Button
                            }
                            if (carrierType != "Text" && carrierUri == null) {
                                scope.launch { snackbarHostState.showSnackbar("Please attach a file first") }
                                return@Button
                            }
                            try {
                                val encrypted = Crypto.encrypt(hiddenText, password)
                                val finalPayload = StegoText.embed(visibleText, encrypted)
                                vaultViewModel.saveMessage(visibleText, encrypted, isSent = true)
                                
                                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("VeilCrypt", finalPayload)
                                clipboardManager.setPrimaryClip(clip)
                                scope.launch { snackbarHostState.showSnackbar("Encrypted & Copied. Saved to Chat.") }
                            } catch (e: Exception) {
                                scope.launch { snackbarHostState.showSnackbar("Encryption failed: ${e.message}") }
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("COPY & SAVE", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    
                    Button(
                        onClick = {
                            if (password.isEmpty() || visibleText.isEmpty() || hiddenText.isEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar("All fields required") }
                                return@Button
                            }
                            if (carrierType != "Text" && carrierUri == null) {
                                scope.launch { snackbarHostState.showSnackbar("Please attach a file first") }
                                return@Button
                            }
                            try {
                                val encrypted = Crypto.encrypt(hiddenText, password)
                                vaultViewModel.saveMessage(visibleText, encrypted, isSent = true)
                                
                                if (carrierType != "Text" && carrierUri != null) {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val sharedUri = FileStego.embedInFile(context, carrierUri!!, encrypted)
                                            withContext(Dispatchers.Main) {
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_STREAM, sharedUri)
                                                    type = context.contentResolver.getType(carrierUri!!) ?: "*/*"
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, "Share Secure Package via")
                                                context.startActivity(shareIntent)
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                snackbarHostState.showSnackbar("File encryption failed: ${e.message}")
                                            }
                                        }
                                    }
                                } else {
                                    val finalPayload = StegoText.embed(visibleText, encrypted)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, finalPayload)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Secure Package via")
                                    context.startActivity(shareIntent)
                                }
                            } catch (e: Exception) {
                                scope.launch { snackbarHostState.showSnackbar("Encryption failed: ${e.message}") }
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("SHARE", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { pad ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.bg_brain),
                contentDescription = "Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))
            
            Column(
                modifier = Modifier
                    .padding(pad)
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Top) {
                            Text("VeilCrypt", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("v2.0", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                        Text("SECURE COMPOSITION", fontSize = 10.sp, color = Color.LightGray, letterSpacing = 2.sp)
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cover Layer
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("COVER LAYER (${if(carrierType == "Text") "VISIBLE TEXT" else "ATTACHED MEDIA"})", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Gray, letterSpacing = 1.sp, modifier = Modifier.padding(start = 8.dp))
                        if (carrierType == "Text") {
                            OutlinedTextField(
                                value = visibleText,
                                onValueChange = { visibleText = it },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                placeholder = { Text("Enter the innocent cover message...", color = Color.DarkGray, fontSize = 14.sp) },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.05f)
                                )
                            )
                        } else {
                            Button(
                                onClick = { 
                                    val mimeType = when(carrierType) {
                                        "Image" -> "image/*"
                                        "Audio" -> "audio/*"
                                        else -> "*/*"
                                    }
                                    filePicker.launch(mimeType)
                                },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                shape = RoundedCornerShape(24.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = Color.Gray)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Tap to pick $carrierType File", color = Color.LightGray)
                                    if (carrierUri != null) {
                                        Text(visibleText, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }

                    // Secret Layer
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SECRET LAYER (HIDDEN)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Text("Privacy Mode: ON", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        OutlinedTextField(
                            value = hiddenText,
                            onValueChange = { hiddenText = it },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text("Enter the encrypted payload...", color = Color.DarkGray, fontSize = 14.sp) },
                            shape = RoundedCornerShape(24.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                    }
                    
                    // Password
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Encryption Password", color = Color.DarkGray, fontSize = 14.sp) },
                            shape = RoundedCornerShape(24.dp),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, "Toggle Password", tint = Color.Gray)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.05f)
                            )
                        )
                    }

                    // Carrier Types
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Carrier & Steganography", fontSize = 12.sp, color = Color.Gray)
                            Text("AES-256 + Argon2id", fontSize = 10.sp, color = Color.Gray, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("Text" to "📝", "Image" to "🖼️", "Audio" to "🎵", "File" to "📄").forEach { (name, icon) ->
                                val isSelected = carrierType == name
                                Surface(
                                    modifier = Modifier.size(80.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f)),
                                    onClick = { carrierType = name }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(icon, fontSize = 24.sp, modifier = Modifier.padding(bottom = 4.dp).let { if(!isSelected) it.alpha(0.5f) else it })
                                        Text(name, fontSize = 10.sp, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text("Designed by Sulaiman Alarabi", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun RevealScreen(initialSharedText: String?, vaultViewModel: VaultViewModel) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf(initialSharedText ?: "") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var revealedVisible by remember { mutableStateOf("") }
    var revealedHidden by remember { mutableStateOf<String?>(null) }
    var revealError by remember { mutableStateOf<String?>(null) }
    var showShredDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var decryptUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var decryptFileName by remember { mutableStateOf<String?>(null) }
    
    val extractPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        decryptUri = uri
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) decryptFileName = c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
            }
            if (decryptFileName == null) decryptFileName = uri.lastPathSegment
            inputText = "[Selected File: $decryptFileName]"
            scope.launch { snackbarHostState.showSnackbar("File selected for decryption") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { pad ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.bg_brain),
                contentDescription = "Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))
            
            Column(
                modifier = Modifier
                    .padding(pad)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Top) {
                            Text("Reveal", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("PACKAGE", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                        Text("DECRYPT AND EXTRACT SECRETS", fontSize = 10.sp, color = Color.LightGray, letterSpacing = 2.sp)
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (clipboardManager.hasPrimaryClip()) {
                                val item = clipboardManager.primaryClip?.getItemAt(0)
                                inputText = item?.text?.toString() ?: ""
                                decryptUri = null
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color.LightGray)
                        Spacer(Modifier.width(4.dp))
                        Text("Paste Text", color = Color.White, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            extractPicker.launch("*/*")
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color.LightGray)
                        Spacer(Modifier.width(4.dp))
                        Text("Select File", color = Color.White, fontSize = 12.sp)
                    }
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ENCRYPTED SOURCE", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Gray, letterSpacing = 1.sp, modifier = Modifier.padding(start = 8.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { 
                            inputText = it 
                            decryptUri = null 
                        },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("Encrypted Package Source (Text or File)", color = Color.DarkGray, fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.05f)
                        )
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Decryption Password", color = Color.DarkGray, fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, "Toggle Password", tint = Color.Gray)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.05f)
                        )
                    )
                }
                
                Button(
                    onClick = {
                        revealedHidden = null
                        revealError = null
                        
                        try {
                            if (decryptUri != null) {
                                val encryptedBase64 = FileStego.extractFromFile(context, decryptUri!!)
                                if (encryptedBase64 != null) {
                                    revealedHidden = Crypto.decrypt(encryptedBase64, password)
                                    revealedVisible = "[Decrypted from File: $decryptFileName]"
                                    vaultViewModel.saveMessage(revealedVisible, encryptedBase64, isSent = false)
                                    scope.launch { snackbarHostState.showSnackbar("File decoded and saved to Chat Inbox.") }
                                } else {
                                    revealError = "No hidden payload found in this file."
                                }
                            } else {
                                revealedVisible = StegoText.extractVisible(inputText)
                                val encryptedBase64 = StegoText.extractHiddenBase64(inputText)
                                if (encryptedBase64 != null) {
                                    revealedHidden = Crypto.decrypt(encryptedBase64, password)
                                    vaultViewModel.saveMessage(revealedVisible, encryptedBase64, isSent = false)
                                    scope.launch { snackbarHostState.showSnackbar("Message decoded and saved to Chat Inbox.") }
                                } else {
                                    revealError = "No hidden payload found. Carrier might be empty or corrupted."
                                }
                            }
                        } catch (e: Exception) {
                            revealError = "Decryption failed. Incorrect password or corrupted payload."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("DECRYPT PACKAGE", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                
                if (revealedVisible.isNotEmpty() || revealedHidden != null || revealError != null) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.height(1.dp).weight(1f).background(Color.White.copy(alpha = 0.1f)))
                        Text("RESULTS", fontSize = 10.sp, color = Color.Gray, letterSpacing = 2.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        Box(modifier = Modifier.height(1.dp).weight(1f).background(Color.White.copy(alpha = 0.1f)))
                    }
                    Spacer(Modifier.height(8.dp))
                    
                    if (revealError != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(revealError!!, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
                        }
                    }
                    
                    if (revealedVisible.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("COVER TEXT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(revealedVisible, color = Color.LightGray, fontSize = 14.sp)
                            }
                        }
                    }
                    
                    if (revealedHidden != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("DECRYPTED SECRET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(revealedHidden!!, color = MaterialTheme.colorScheme.secondary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 14.sp)
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { showShredDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("SHRED SECRETS", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Designed by Sulaiman Alarabi", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showShredDialog) {
        AlertDialog(
            onDismissRequest = { showShredDialog = false },
            title = {
                Text("Secure Shredding", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This will permanently overwrite the hidden payload in memory. This action cannot be reversed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        revealedHidden = null
                        inputText = ""
                        password = ""
                        revealedVisible = ""
                        showShredDialog = false
                    }
                ) {
                    Text("Shred Now", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showShredDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }
}

@Composable
fun SettingsScreen(vaultViewModel: VaultViewModel) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_brain),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))
        
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Security Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Encryption Protocol", fontWeight = FontWeight.Bold)
                    Text("AES-256-GCM", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text("Key Derivation", fontWeight = FontWeight.Bold)
                    Text("PBKDF2WithHmacSHA256 (10,000 iter)", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
            
            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CLEAR LOCAL CHAT VAULT")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Deniability Mode (Fake Passwords)")
                Switch(checked = false, onCheckedChange = { })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Self-Destruct on Wrong Attempts")
                Switch(checked = true, onCheckedChange = { })
            }
            
            Text("Note: Plausible deniability and Self-Destruct are interface previews in this prototype.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Designed by Sulaiman Alarabi", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(top = 24.dp), textAlign = TextAlign.Center, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }
        
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Clear Vault") },
                text = { Text("Delete all saved messages?") },
                confirmButton = {
                    TextButton(onClick = { vaultViewModel.shredAll(); showDialog = false }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun ChatScreen(vaultViewModel: VaultViewModel) {
    val messages by vaultViewModel.messages.collectAsStateWithLifecycle()
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_brain),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))
        
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Secure P2P Vault", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("LOCAL INBOX", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                }
            }
            
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text("No messages saved yet. Compose a message and 'Save to Chat' or decode a package to see it here.", color = Color.Gray, modifier = Modifier.padding(32.dp), textAlign = TextAlign.Center)
                    }
                }
                
                items(messages.size) { index ->
                    val msg = messages[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isSent) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier.widthIn(max = 300.dp),
                            shape = RoundedCornerShape(
                                topStart = 16.dp, 
                                topEnd = 16.dp, 
                                bottomStart = if (msg.isSent) 16.dp else 4.dp, 
                                bottomEnd = if (msg.isSent) 4.dp else 16.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (msg.isSent) MaterialTheme.colorScheme.primary.copy(alpha=0.3f) 
                                                 else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(if (msg.isSent) "You (Cover)" else "Them (Cover)", fontSize = 10.sp, color = Color.Gray)
                                Text(msg.coverText, fontSize = 14.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("ENCRYPTED PAYLOAD:", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                Text("${msg.secretPayload.take(20)}...", fontSize = 12.sp, color = Color.Gray, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

object FileStego {
    private const val DELIMITER = ":::VEILCRYPT:::"
    
    fun embedInFile(context: Context, sourceUri: android.net.Uri, payload: String): android.net.Uri {
        val bytes = context.contentResolver.openInputStream(sourceUri)?.readBytes() ?: throw Exception("Cannot read file")
        
        val contentString = String(bytes, Charsets.ISO_8859_1)
        val strippedBytes = if (contentString.contains(DELIMITER)) {
            val originalLength = contentString.indexOf(DELIMITER)
            bytes.copyOfRange(0, originalLength)
        } else {
            bytes
        }
        
        val payloadBytes = "\n$DELIMITER$payload$DELIMITER".toByteArray(Charsets.UTF_8)
        val finalBytes = strippedBytes + payloadBytes
        
        val sharedDir = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
        val ext = context.contentResolver.getType(sourceUri)?.let { android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "bin"
        val tempFile = java.io.File(sharedDir, "secure_package.$ext")
        tempFile.writeBytes(finalBytes)
        
        return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
    }

    fun extractFromFile(context: Context, sourceUri: android.net.Uri): String? {
        val bytes = context.contentResolver.openInputStream(sourceUri)?.readBytes() ?: return null
        val contentString = String(bytes, Charsets.ISO_8859_1)
        val startIndex = contentString.indexOf(DELIMITER)
        if (startIndex != -1) {
            val stringEndIndex = contentString.indexOf(DELIMITER, startIndex + DELIMITER.length)
            if (stringEndIndex != -1) {
                val payloadISO = contentString.substring(startIndex + DELIMITER.length, stringEndIndex)
                return String(payloadISO.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
            }
        }
        return null
    }
}
