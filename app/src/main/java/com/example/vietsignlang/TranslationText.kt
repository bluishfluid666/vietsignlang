package com.example.vietsignlang

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TranslationText(translation: String) {
    // Display the random string at the top of the screen
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = translation,
            modifier = Modifier
                .padding(16.dp)
                ,
            style = MaterialTheme.typography.displayMedium
        )
//        OutlinedTextField(value = translation, onValueChange = onValueChange, label = { Text("Name") })
    }
}