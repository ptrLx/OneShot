package de.ptrlx.oneshot.feature_diary.presentation.diary.components.diary

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DiaryTop(
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        Search()
        CalendarSelection()
        HappinessSelectorChips()
        GalleryList()
    }

}

//todo

@Composable
fun Search(
    modifier: Modifier = Modifier
) {
}

@Composable
fun CalendarSelection(
    modifier: Modifier = Modifier
) {
}

//todo

@Composable
fun HappinessSelectorChips(
    modifier: Modifier = Modifier
) {
}

@Composable
fun GalleryList(
    modifier: Modifier = Modifier
) {
}
