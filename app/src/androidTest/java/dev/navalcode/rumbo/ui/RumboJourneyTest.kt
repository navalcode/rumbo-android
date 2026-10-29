package dev.navalcode.rumbo.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.navalcode.rumbo.MainActivity
import dev.navalcode.rumbo.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RumboJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForCatalog() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("trip-card-alba").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun openTripReserveAndFindItInPlan() {
        waitForCatalog()
        compose.onNodeWithTag("trip-card-alba").performClick()
        compose.onNodeWithTag("reserve-trip").performScrollTo()
        if (!compose.onNodeWithTag("reserve-trip").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)) {
            compose.onNodeWithTag("reserve-trip").performClick()
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("reservation-confirmation").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("close-detail").performScrollTo().performClick()
        compose.onNodeWithTag("nav-plan").performClick()
        compose.onNodeWithTag("booking-alba").assertExists()
    }

    @Test fun offlineScenarioKeepsCatalogAvailable() {
        waitForCatalog()
        compose.onNodeWithTag("nav-lab").performClick()
        compose.onNodeWithTag("scenario-OFFLINE").performClick()
        compose.onNodeWithTag("nav-explore").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("sync-banner").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("trip-card-alba").assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.offline_cached)).assertExists()
    }

    @Test fun systemBackReturnsFromDetailToTheCatalog() {
        waitForCatalog()
        compose.onNodeWithTag("trip-card-alba").performClick()
        compose.onNodeWithTag("close-detail").assertExists()
        compose.onNodeWithTag("nav-explore").assertDoesNotExist()
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithTag("trip-card-alba").assertExists()
        compose.onNodeWithTag("nav-explore").assertIsSelected()
    }

    @Test fun activityRecreationRestoresTheDetailRoute() {
        waitForCatalog()
        compose.onNodeWithTag("trip-card-alba").performClick()
        compose.onNodeWithTag("close-detail").assertExists()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("close-detail").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("nav-explore").assertDoesNotExist()
        compose.onNodeWithTag("close-detail").performScrollTo().performClick()
        compose.onNodeWithTag("trip-card-alba").assertExists()
    }
}
