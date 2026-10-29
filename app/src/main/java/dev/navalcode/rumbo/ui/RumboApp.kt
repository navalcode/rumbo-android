@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.navalcode.rumbo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.navalcode.rumbo.R
import dev.navalcode.rumbo.core.Category
import dev.navalcode.rumbo.core.SyncProblem
import dev.navalcode.rumbo.core.SyncStatus
import dev.navalcode.rumbo.core.Trip
import dev.navalcode.rumbo.core.filterTrips
import dev.navalcode.rumbo.data.DemoScenario
import java.text.DateFormat
import java.util.Date

@Composable
fun RumboApp(model: TripsViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val query by model.query.collectAsStateWithLifecycle()
    val category by model.category.collectAsStateWithLifecycle()
    val scenario by model.scenario.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: "explore"
    fun openTrip(id: String) { nav.navigate("trip/$id") { launchSingleTop = true } }
    fun selectTab(destination: String) {
        nav.navigate(destination) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = Paper,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (route != "trip/{tripId}") Surface(color = Paper, tonalElevation = 0.dp) {
                Column {
                    HorizontalDivider(color = Line)
                    NavigationBar(containerColor = Paper, tonalElevation = 0.dp) {
                        listOf(
                            Triple(R.string.explore, Icons.Outlined.Explore, "nav-explore"),
                            Triple(R.string.my_plan, Icons.Outlined.BookmarkBorder, "nav-plan"),
                            Triple(R.string.lab, Icons.Outlined.Science, "nav-lab"),
                        ).forEachIndexed { index, (label, icon, tag) ->
                            NavigationBarItem(
                                modifier = Modifier.testTag(tag),
                                selected = route == listOf("explore", "plan", "lab")[index],
                                onClick = { selectTab(listOf("explore", "plan", "lab")[index]) },
                                icon = { Icon(icon, null, modifier = Modifier.size(23.dp)) },
                                label = { Text(stringResource(label), fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Sage, selectedIconColor = Ink, selectedTextColor = Ink, unselectedIconColor = Muted, unselectedTextColor = Muted),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            NavHost(navController = nav, startDestination = "explore") {
                composable("explore") {
                    ExploreScreen(state, query, category, model::queryChanged, model::categoryChanged,
                        model::refresh, ::openTrip, model::favorite)
                }
                composable("plan") { PlanScreen(state, ::openTrip, model::favorite) }
                composable("lab") {
                    LabScreen(state, scenario, model::scenarioChanged, { selectTab("explore") })
                }
                composable("trip/{tripId}", arguments = listOf(navArgument("tripId") { type = NavType.StringType })) { entry ->
                    val trip = state.catalog.trips.find { it.id == entry.arguments?.getString("tripId") }
                    if (trip != null) {
                        TripDetail(trip, trip.id in state.favorites,
                            state.bookings.find { it.tripId == trip.id }?.reference,
                            { nav.popBackStack() }, { model.favorite(trip.id, it) }, { model.reserve(trip.id) })
                    } else if (state.sync == SyncStatus.Updating || state.sync == SyncStatus.Idle) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    } else {
                        EmptyPanel(R.string.empty_title, R.string.empty_body, R.string.close_detail, { nav.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.NearMe, null, Modifier.size(27.dp), tint = Ink)
        Text("rumbo", fontFamily = FontFamily.Serif, fontSize = 32.sp, letterSpacing = (-1.5).sp,
            modifier = Modifier.padding(start = 6.dp))
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.demo_label), style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.border(1.dp, Line, RoundedCornerShape(6.dp)).padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@Composable
private fun Hero(eyebrow: Int, title: Int, subtitle: Int? = null) {
    Text(stringResource(eyebrow), style = MaterialTheme.typography.labelSmall, color = Muted)
    Text(stringResource(title), style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.padding(top = 10.dp).semantics { heading() })
    if (subtitle != null) Text(stringResource(subtitle), style = MaterialTheme.typography.bodyLarge,
        color = Muted, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun ExploreScreen(
    state: TripsState,
    query: String,
    category: Category?,
    onQuery: (String) -> Unit,
    onCategory: (Category?) -> Unit,
    refresh: () -> Unit,
    open: (String) -> Unit,
    favorite: (String, Boolean) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val trips = filterTrips(state.catalog.trips, query, category)
    LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            BrandHeader()
            Hero(R.string.hero_eyebrow, R.string.hero_title, R.string.hero_subtitle)
            Spacer(Modifier.height(23.dp))
            OutlinedTextField(
                value = query, onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().testTag("search"),
                placeholder = { Text(stringResource(R.string.search_hint), fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Muted) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Outlined.Close, stringResource(R.string.clear_search)) } },
                shape = RoundedCornerShape(18.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Line, unfocusedContainerColor = Color.White.copy(alpha = .65f)),
            )
            Row(Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = category == null, onClick = { onCategory(null) }, label = { Text(stringResource(R.string.filter_all)) })
                Category.entries.forEach { value ->
                    FilterChip(selected = category == value, onClick = { onCategory(value) },
                        modifier = Modifier.testTag("filter-${value.name}"), label = { Text(categoryLabel(value)) })
                }
            }
        }
        if (state.sync is SyncStatus.Failed && state.catalog.trips.isNotEmpty()) {
            item { SyncBanner((state.sync as SyncStatus.Failed).problem, refresh) }
        }
        if (state.sync == SyncStatus.Updating && state.catalog.trips.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.updating), style = MaterialTheme.typography.bodyMedium, color = Muted)
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = Ink, trackColor = Sage)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.selection_title), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics { heading() })
                IconButton(onClick = refresh) { Icon(Icons.Outlined.Refresh, stringResource(R.string.refresh), tint = Muted) }
            }
            Text(pluralStringResource(R.plurals.result_count, trips.size, trips.size), style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        when {
            state.catalog.trips.isEmpty() && state.sync is SyncStatus.Failed -> item {
                val problem = (state.sync as SyncStatus.Failed).problem
                EmptyPanel(problem.title(), R.string.error_empty_body, R.string.retry, refresh)
            }
            state.catalog.trips.isEmpty() && (state.sync == SyncStatus.Idle || state.sync == SyncStatus.Updating) -> item {
                Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = Ink, strokeWidth = 2.dp)
                    Text(stringResource(R.string.loading_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.loading_body), style = MaterialTheme.typography.bodyMedium, color = Muted)
                }
            }
            trips.isEmpty() -> item {
                EmptyPanel(R.string.empty_title, R.string.empty_body, R.string.clear_filters, { onQuery(""); onCategory(null) })
            }
            else -> items(trips, key = { it.id }) { trip -> TripCard(trip, trip.id in state.favorites, { open(trip.id) }, { favorite(trip.id, it) }) }
        }
        item { Text(stringResource(R.string.demo_notice), color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp)) }
    }
}

@Composable
private fun TripCard(trip: Trip, saved: Boolean, open: () -> Unit, favorite: (Boolean) -> Unit) {
    val openLabel = stringResource(R.string.open_trip, trip.title)
    Card(
        onClick = open, modifier = Modifier.fillMaxWidth().testTag("trip-card-${trip.id}"),
        shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Box {
            Landscape(trip.artwork, Modifier.fillMaxWidth().height(170.dp).semantics { contentDescription = openLabel })
            Surface(Modifier.align(Alignment.TopStart).padding(14.dp), color = Paper.copy(alpha = .93f), shape = RoundedCornerShape(8.dp)) {
                Text(categoryLabel(trip.category), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.bodyMedium)
            }
            FavoriteButton(saved, trip.title, { favorite(!saved) }, Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        Column(Modifier.padding(18.dp)) {
            Text(trip.region.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted)
            Text(trip.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Schedule, null, Modifier.size(15.dp), tint = Muted)
                Text(pluralStringResource(R.plurals.trip_duration, trip.days, trip.days), style = MaterialTheme.typography.bodyMedium, color = Muted)
                Text("·", color = Muted)
                Text(stringResource(R.string.trip_distance, trip.distanceKm), style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Line)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.trip_price, trip.priceEuros), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.per_person), style = MaterialTheme.typography.bodyMedium, color = Muted)
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(21.dp), tint = Ink)
            }
        }
    }
}

@Composable
private fun FavoriteButton(saved: Boolean, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalIconButton(onClick, modifier = modifier.size(48.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Paper.copy(alpha = .96f))) {
        Icon(if (saved) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
            stringResource(if (saved) R.string.favorite_remove else R.string.favorite_add, title), tint = Ink, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun SyncBanner(problem: SyncProblem, refresh: () -> Unit) {
    Surface(color = Sage, shape = RoundedCornerShape(14.dp), modifier = Modifier.testTag("sync-banner")) {
        Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WifiOff, null, Modifier.size(19.dp), tint = Ink)
            Text(stringResource(when (problem) {
                SyncProblem.OFFLINE -> R.string.offline_cached
                SyncProblem.UNAVAILABLE -> R.string.service_cached
                SyncProblem.TIMEOUT -> R.string.timeout_cached
            }), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
            IconButton(refresh) { Icon(Icons.Outlined.Refresh, stringResource(R.string.retry), Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun EmptyPanel(title: Int, body: Int, action: Int? = null, onClick: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().background(Sage.copy(alpha = .6f), RoundedCornerShape(18.dp)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = Muted)
        if (action != null) TextButton(onClick, contentPadding = PaddingValues(0.dp)) { Text(stringResource(action)) }
    }
}

@Composable
private fun PlanScreen(state: TripsState, open: (String) -> Unit, favorite: (String, Boolean) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { BrandHeader(); Hero(R.string.plan_eyebrow, R.string.plan_title) }
        item { SectionTitle(R.string.bookings_title) }
        if (state.bookings.isEmpty()) item { Text(stringResource(R.string.no_bookings), style = MaterialTheme.typography.bodyLarge, color = Muted) }
        items(state.bookings, key = { "booking-${it.tripId}" }) { booking ->
            state.catalog.trips.find { it.id == booking.tripId }?.let { trip ->
                Surface(onClick = { open(trip.id) }, shape = RoundedCornerShape(18.dp), color = Sage, modifier = Modifier.fillMaxWidth().testTag("booking-${trip.id}")) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Landscape(trip.artwork, Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)))
                        Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(trip.title, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.booking_reference, booking.reference), style = MaterialTheme.typography.bodyMedium, color = Muted)
                            Text(stringResource(R.string.booking_saved), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        item { SectionTitle(R.string.favorites_title) }
        val saved = state.catalog.trips.filter { it.id in state.favorites }
        if (saved.isEmpty()) item { Text(stringResource(R.string.no_favorites), style = MaterialTheme.typography.bodyLarge, color = Muted) }
        items(saved, key = { "favorite-${it.id}" }) { trip -> TripCard(trip, true, { open(trip.id) }, { favorite(trip.id, it) }) }
        item { Text(stringResource(R.string.demo_notice), color = Muted, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun LabScreen(state: TripsState, scenario: DemoScenario, select: (DemoScenario) -> Unit, explore: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { BrandHeader(); Hero(R.string.lab_eyebrow, R.string.lab_title, R.string.lab_body) }
        items(DemoScenario.entries, key = { it.name }) { value ->
            val selected = scenario == value
            val (title, body) = when (value) {
                DemoScenario.CONNECTED -> R.string.scenario_connected to R.string.scenario_connected_body
                DemoScenario.OFFLINE -> R.string.scenario_offline to R.string.scenario_offline_body
                DemoScenario.SERVICE_ERROR -> R.string.scenario_error to R.string.scenario_error_body
                DemoScenario.SLOW -> R.string.scenario_slow to R.string.scenario_slow_body
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (selected) Sage else Color.White)
                    .border(1.dp, if (selected) Ink.copy(alpha = .25f) else Line, RoundedCornerShape(16.dp))
                    .selectable(selected, role = Role.RadioButton, onClick = { select(value) })
                    .testTag("scenario-${value.name}").padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = Muted)
                }
                RadioButton(selected, null, Modifier.padding(start = 10.dp), colors = RadioButtonDefaults.colors(selectedColor = Ink))
            }
        }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = Ink) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.lab_cache_title), style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(stringResource(R.string.lab_cache_body), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD9E2D7))
                    Text(state.catalog.updatedAt?.let { stringResource(R.string.cache_updated, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))) }
                        ?: stringResource(R.string.cache_empty), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCDD6BC))
                }
            }
        }
        item { Button(explore, Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) { Text(stringResource(R.string.lab_cta)) } }
    }
}

@Composable
private fun SectionTitle(resource: Int) {
    Text(stringResource(resource), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
}

@Composable
private fun TripDetail(trip: Trip, saved: Boolean, reference: String?, close: () -> Unit, favorite: (Boolean) -> Unit, reserve: () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        Box {
            Landscape(trip.artwork, Modifier.fillMaxWidth().height(230.dp))
            FilledTonalIconButton(close, Modifier.align(Alignment.TopStart).padding(12.dp).size(48.dp).testTag("close-detail"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Paper)) {
                Icon(Icons.Outlined.Close, stringResource(R.string.close_detail))
            }
            FavoriteButton(saved, trip.title, { favorite(!saved) }, Modifier.align(Alignment.TopEnd).padding(12.dp))
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(trip.region.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted)
            Text(trip.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(categoryLabel(trip.category), style = MaterialTheme.typography.bodyMedium, color = Muted)
                Text(pluralStringResource(R.plurals.trip_duration, trip.days, trip.days), style = MaterialTheme.typography.bodyMedium, color = Muted)
                Text(stringResource(R.string.trip_distance, trip.distanceKm), style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
            HorizontalDivider(color = Line)
            Text(stringResource(R.string.about_trip), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(when (trip.category) {
                Category.MOUNTAIN -> R.string.description_mountain
                Category.COAST -> R.string.description_coast
                Category.FOREST -> R.string.description_forest
            }), style = MaterialTheme.typography.bodyLarge, color = Muted)
            listOf(R.string.highlight_flexible, R.string.highlight_local).forEach { label ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp), tint = Ink)
                    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider(color = Line)
            Text(stringResource(R.string.trip_price, trip.priceEuros), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.per_person), style = MaterialTheme.typography.bodyMedium, color = Muted)
            if (reference != null) {
                Surface(color = Sage, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().testTag("reservation-confirmation")) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.booking_saved), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.booking_reference, reference), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Button(reserve, enabled = reference == null, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("reserve-trip")) {
                Text(stringResource(if (reference == null) R.string.reserve_trip else R.string.reserved_trip))
            }
            Text(stringResource(R.string.demo_notice), style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
}

@Composable
private fun categoryLabel(value: Category) = stringResource(when (value) {
    Category.MOUNTAIN -> R.string.category_mountain
    Category.COAST -> R.string.category_coast
    Category.FOREST -> R.string.category_forest
})

private fun SyncProblem.title() = when (this) {
    SyncProblem.OFFLINE -> R.string.offline_title
    SyncProblem.UNAVAILABLE -> R.string.service_title
    SyncProblem.TIMEOUT -> R.string.timeout_title
}
