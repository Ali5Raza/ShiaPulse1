package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun QadhaTab(viewModel: PrayerViewModel, currentLang: String) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = if (currentLang == "ur") {
        listOf("قضا کاؤنٹر", "حساب کتاب", "شرعی احکام")
    } else if (currentLang == "ar") {
        listOf("عداد القضاء", "حساب الفوائت", "أحكام القضاء")
    } else {
        listOf("Qadha Tally", "Calculator", "Rules")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp, maxLines = 1) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            val qadaTallies by viewModel.qadaTallies.collectAsStateWithLifecycle()
            when (selectedTab) {
                0 -> QadhaTallyTab(viewModel, currentLang, qadaTallies)
                1 -> QadhaCalculatorTab(viewModel, currentLang)
                2 -> QadhaRulesTab(currentLang)
            }
        }
    }
}
