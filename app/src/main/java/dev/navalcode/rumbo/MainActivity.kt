package dev.navalcode.rumbo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.navalcode.rumbo.ui.RumboApp
import dev.navalcode.rumbo.ui.RumboTheme
import dev.navalcode.rumbo.ui.TripsViewModel
import dev.navalcode.rumbo.data.DemoScenario

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = application as RumboApplication
        if (savedInstanceState == null) {
            DemoScenario.entries.firstOrNull { it.name == intent.getStringExtra("demo_scenario") }
                ?.let(graph.source::setScenario)
        }
        setContent {
            RumboTheme {
                val model: TripsViewModel = viewModel(factory = viewModelFactory {
                    initializer { TripsViewModel(graph.repository, graph.source) }
                })
                RumboApp(model)
            }
        }
    }
}
