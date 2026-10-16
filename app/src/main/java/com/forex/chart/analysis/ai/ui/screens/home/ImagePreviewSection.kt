package com.forex.chart.analysis.ai.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.forex.chart.analysis.ai.ui.theme.ButtonBlue
import com.forex.chart.analysis.ai.ui.theme.TextWhite

@Composable
fun ImagePreviewSection(
    selectedImages: List<Uri>,
    onRemoveImage: (Uri) -> Unit,
    onAddFromGallery: () -> Unit,
    onAddFromCamera: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Selected Charts (${selectedImages.size}/5)",
                style = MaterialTheme.typography.titleMedium.copy(color = TextWhite),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {


            items(selectedImages) { uri ->
                ImagePreviewItem(uri = uri, onRemove = { onRemoveImage(uri) })
            }

            if (selectedImages.size < 5) {
                item {
                    AddActionsCard(
                        onGalleryClick = onAddFromGallery,
                        onCameraClick = onAddFromCamera
                    )
                }
            }
        }
    }
}

@Composable
fun ImagePreviewItem(uri: Uri, onRemove: () -> Unit) {
    Box(
        modifier = Modifier.size(width = 120.dp, height = 160.dp)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Chart Preview",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .background(Color.DarkGray),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


@Composable
fun AddActionsCard(
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(width = 120.dp, height = 160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, ButtonBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable { onGalleryClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Gallery",
                    tint = ButtonBlue,
                    modifier = Modifier.size(28.dp)
                )
                Text("Gallery", color = ButtonBlue, fontSize = 11.sp)
            }
        }


        HorizontalDivider(color = ButtonBlue.copy(alpha = 0.2f))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable { onCameraClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = ButtonBlue,
                    modifier = Modifier.size(28.dp)
                )
                Text("Camera", color = ButtonBlue, fontSize = 11.sp)
            }
        }
    }
}