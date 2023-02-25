package de.ptrlx.oneshot.feature_diary.presentation.diary.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import de.ptrlx.oneshot.R
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel

@Composable
fun ImageCard(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel,
    entry: DiaryEntry,
    description: String,
    expandOnClick: Boolean = false
) {
    val contentDescription = "Diary entry: $description"
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = if (expandOnClick)
            modifier.clickable {
                expanded = !expanded
            }
        else modifier,
        shape = RoundedCornerShape(15.dp),
        elevation = 5.dp
    ) {
        Box(
            modifier = (if (!expanded)
                Modifier
                    .height(200.dp)
                    .width(200.dp)
            else Modifier)
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                modifier = Modifier.fillMaxWidth(),
                model = viewModel.fileManager?.resolveUri(entry.relativePath),
                contentDescription = contentDescription,
                error = painterResource(R.drawable.ic_baseline_error_24),
                contentScale = if (!expanded) ContentScale.Crop else ContentScale.FillWidth,
            )
//            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black
                            ),
                            startY = 300f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    description,
                    style = TextStyle(color = Color.White, fontSize = 16.sp),
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(entry.happiness.emoticon(), style = TextStyle(fontSize = 16.sp))
            }
        }
    }

}