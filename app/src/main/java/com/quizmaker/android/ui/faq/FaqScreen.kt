package com.quizmaker.android.ui.faq

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.FaqContent
import com.quizmaker.android.data.model.FaqItem
import com.quizmaker.android.ui.common.elevatedSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("FAQ", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            FaqContent.sections.forEach { section ->
                item {
                    Text(
                        section.title,
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 10.dp, top = 6.dp)
                    )
                }
                items(section.items) { faq ->
                    FaqRow(faq)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun FaqRow(item: FaqItem) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
            .clickable { expanded = !expanded }
            .animateContentSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                item.question,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TextSecondary
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Text(item.answer, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}
