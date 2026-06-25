package uz.vazifa.app.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.SubcomposeAsyncImage
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme
import uz.vazifa.app.util.CameraImageUri

@Composable
fun OptionalTaskImagePicker(
    imageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onImageSelected(uri)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) onImageSelected(pendingCameraUri)
        pendingCameraUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val uri = pendingCameraUri
        if (granted && uri != null) {
            takePictureLauncher.launch(uri)
        } else {
            pendingCameraUri = null
        }
    }

    fun launchCamera() {
        val uri = CameraImageUri.create(context)
        pendingCameraUri = uri
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> takePictureLauncher.launch(uri)
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(LiquidGlass.RadiusInput),
            ) {
                Icon(Icons.Default.PhotoLibrary, null, tint = LiquidGlass.Blue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(localized("task_photo_gallery"), color = LiquidTheme.text, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { launchCamera() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(LiquidGlass.RadiusInput),
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = LiquidGlass.Blue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(localized("task_photo_camera"), color = LiquidTheme.text, fontSize = 13.sp)
            }
        }
        if (imageUri == null) {
            Text(label, color = LiquidTheme.textMuted, fontSize = 12.sp)
        }
        imageUri?.let { uri ->
            Box(Modifier.fillMaxWidth()) {
                SubcomposeAsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(LiquidGlass.RadiusInput)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = LiquidGlass.Blue)
                        }
                    },
                    error = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = LiquidTheme.textMuted)
                        }
                    },
                )
                IconButton(
                    onClick = { onImageSelected(null) },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(Icons.Default.Close, null, tint = LiquidTheme.text)
                }
            }
        }
    }
}
