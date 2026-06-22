package uz.vazifa.app.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uz.vazifa.app.presentation.theme.LiquidGlass
import uz.vazifa.app.presentation.theme.LiquidTheme

@Composable
fun OptionalTaskImagePicker(
    imageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onImageSelected(uri)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LiquidGlass.RadiusInput),
        ) {
            Icon(Icons.Default.AddPhotoAlternate, null, tint = LiquidGlass.Blue)
            Spacer(Modifier.width(8.dp))
            Text(label, color = LiquidTheme.text)
        }
        imageUri?.let { uri ->
            Box(Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(LiquidGlass.RadiusInput)),
                    contentScale = ContentScale.Crop,
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

@Composable
fun TaskAttachmentGrid(
    urls: List<String>,
    modifier: Modifier = Modifier,
) {
    if (urls.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        urls.forEach { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(LiquidGlass.RadiusInput)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
