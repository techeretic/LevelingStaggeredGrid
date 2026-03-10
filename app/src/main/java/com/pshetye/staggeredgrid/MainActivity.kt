package com.pshetye.staggeredgrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.pshetye.staggeredgrid.ui.screen.StaggeredGridScreen
import com.pshetye.staggeredgrid.ui.theme.StaggeredGridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StaggeredGridTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StaggeredGridScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
