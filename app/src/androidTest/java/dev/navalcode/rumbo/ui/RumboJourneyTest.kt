package dev.navalcode.rumbo.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.test.core.app.ApplicationProvider
import dev.navalcode.rumbo.RumboApplication
import dev.navalcode.rumbo.data.DemoScenario
import dev.navalcode.rumbo.core.SyncStatus
import dev.navalcode.rumbo.MainActivity
import dev.navalcode.rumbo.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.ExternalResource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first

@RunWith(AndroidJUnit4::class)
class RumboJourneyTest {
    @get:Rule(order = 0) val sourceScenario = object : ExternalResource() {
        override fun before() {
            ApplicationProvider.getApplicationContext<RumboApplication>().source.setScenario(DemoScenario.CONNECTED)
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForCatalog() {
        val graph = ApplicationProvider.getApplicationContext<RumboApplication>()
        runBlocking { withTimeout(10_000) { graph.repository.catalog.first { catalog -> catalog.trips.any { it.id == "alba" } } } }
        compose.waitForIdle()
        // A populated catalog does not mean a lazy item is already composed on a small viewport.
        showFirstTrip()
    }

    private fun showFirstTrip() {
        compose.onNodeWithTag("explore-list").performScrollToNode(hasTestTag("trip-card-alba"))
        compose.onNodeWithTag("trip-card-alba").assertIsDisplayed()
    }

    private fun capture(name: String) {
        val label = InstrumentationRegistry.getArguments().getString("captureLabel") ?: return
        require(label.matches(Regex("[a-z0-9-]{1,32}")))
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "editorial-captures")
        check(folder.mkdirs() || folder.isDirectory)
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        try {
            File(folder, "$label-$name.png").outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally { bitmap.recycle() }
    }

    @Test fun openTripReserveAndFindItInPlan() {
        waitForCatalog()
        compose.onNodeWithTag("explore-list").performScrollToIndex(0)
        capture("explore")
        showFirstTrip()
        compose.onNodeWithTag("trip-card-alba").performClick()
        compose.onNodeWithTag("reserve-trip").performScrollTo()
        capture("detail")
        if (!compose.onNodeWithTag("reserve-trip").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)) {
            compose.onNodeWithTag("reserve-trip").performClick()
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("reservation-confirmation").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("close-detail").performScrollTo().performClick()
        compose.onNodeWithTag("nav-plan").performClick()
        compose.onNodeWithTag("booking-alba").assertExists()
        capture("plan")
    }

    @Test fun offlineScenarioKeepsCatalogAvailable() {
        waitForCatalog()
        compose.onNodeWithTag("nav-lab").performClick()
        compose.onNodeWithTag("scenario-OFFLINE").performClick()
        capture("lab")
        compose.onNodeWithTag("nav-explore").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("sync-banner").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(compose.activity.getString(R.string.offline_cached)).assertExists()
        compose.onNodeWithTag("explore-list").performScrollToIndex(0)
        capture("offline")
        showFirstTrip()
        compose.onNodeWithTag("nav-lab").performClick()
        compose.onNodeWithTag("scenario-CONNECTED").performClick()
        val graph = ApplicationProvider.getApplicationContext<RumboApplication>()
        runBlocking { withTimeout(10_000) { graph.repository.sync.first { it == SyncStatus.Ready } } }
        compose.onNodeWithTag("nav-explore").performClick()
        waitForCatalog()
        compose.onAllNodesWithTag("sync-banner").assertCountEquals(0)
        compose.onNodeWithTag("explore-list").performScrollToIndex(0)
        capture("recovered")
    }

    @Test fun systemBackReturnsFromDetailToTheCatalog() {
        waitForCatalog()
        compose.onNodeWithTag("trip-card-alba").performClick()
        compose.onNodeWithTag("close-detail").assertExists()
        compose.onNodeWithTag("nav-explore").assertDoesNotExist()
        androidx.test.espresso.Espresso.pressBack()
        showFirstTrip()
        compose.onNodeWithTag("nav-explore").assertIsSelected()
    }

    @Test fun favoriteActionStaysInCatalogAndSurvivesActivityRecreation() {
        waitForCatalog()
        val graph = ApplicationProvider.getApplicationContext<RumboApplication>()
        val wasSaved = runBlocking { "alba" in graph.repository.favorites.first() }
        val title = runBlocking { graph.repository.catalog.first().trips.first { it.id == "alba" }.title }
        val action = compose.activity.getString(if (wasSaved) R.string.favorite_remove else R.string.favorite_add, title)
        compose.onNodeWithContentDescription(action).performClick()
        runBlocking { withTimeout(5_000) { graph.repository.favorites.first { ("alba" in it) != wasSaved } } }
        compose.onNodeWithTag("close-detail").assertDoesNotExist()
        compose.onNodeWithTag("nav-explore").assertIsSelected()
        compose.activityRule.scenario.recreate()
        showFirstTrip()
        val reverseAction = compose.activity.getString(if (wasSaved) R.string.favorite_add else R.string.favorite_remove, title)
        compose.onNodeWithContentDescription(reverseAction).assertIsDisplayed()
        compose.onNodeWithTag("close-detail").assertDoesNotExist()
    }

    @Test fun activityRecreationRestoresTheDetailRoute() {
        waitForCatalog()
        compose.onNodeWithTag("trip-card-alba").performClick()
        compose.onNodeWithTag("close-detail").assertExists()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("close-detail").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("nav-explore").assertDoesNotExist()
        compose.onNodeWithTag("close-detail").performScrollTo().performClick()
        showFirstTrip()
    }
}
