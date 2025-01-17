package ca.ramzan.atmostate.ui.forecast

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import ca.ramzan.atmostate.ForecastViewModel
import ca.ramzan.atmostate.repository.RefreshState
import ca.ramzan.atmostate.repository.USER_LOCATION_CITY_ID
import ca.ramzan.atmostate.ui.MainDestinations
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.ramzan.atmostate.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val HIDE_LOCATION_RATIONALE = booleanPreferencesKey("hide_rationale")

@ExperimentalPermissionsApi
@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalPagerApi
@Composable
fun Forecast(
    vm: ForecastViewModel,
    navController: NavController,
    currentListState: LazyListState,
    hourlyListState: LazyListState,
    dailyListState: LazyListState,
    pagerState: PagerState,
    scaffoldState: ScaffoldState,
) {
    val refreshState = vm.state.collectAsState()
    val currentForecast = vm.currentForecast.collectAsState()
    val hourlyForecast = vm.hourlyForecast.collectAsState()
    val dailyForecast = vm.dailyForecast.collectAsState()
    val alerts = vm.alerts.collectAsState()
    val cities = vm.cities.collectAsState()
    val currentCityName = vm.currentCityName.collectAsState("")
    val context = LocalContext.current
    val hideRationale =
        context.dataStore.data.map { it[HIDE_LOCATION_RATIONALE] ?: false }.collectAsState(true)

    val scope = rememberCoroutineScope()

    val (errorShown, setErrorShown) = rememberSaveable { mutableStateOf(false) }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            ForecastAppBar(
                pagerState,
                currentCityName.value,
                {
                    scope.launch {
                        scaffoldState.drawerState.open()
                    }
                },
                {
                    scope.launch {
                        vm.removeCurrentCity()
                        scaffoldState.snackbarHostState.showSnackbar(context.getString(R.string.location_removed_message))
                    }
                },
                { showSourceCode(context) }
            )
        },
        drawerContent = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
            ) {
                item {
                    DrawerItem(
                        text = stringResource(R.string.your_location),
                        selected = currentCityName.value.isEmpty()
                    ) {
                        vm.setCurrentCity(USER_LOCATION_CITY_ID)
                        scope.launch { scaffoldState.drawerState.close() }
                    }
                }
                items(cities.value) { city ->
                    DrawerItem(text = city.name, selected = city.selected) {
                        vm.setCurrentCity(city.id)
                        scope.launch { scaffoldState.drawerState.close() }
                    }
                }
                item {
                    DrawerItem(text = stringResource(R.string.add_location), selected = false) {
                        navController.navigate(MainDestinations.CITY_SELECT_ROUTE)
                    }
                }
            }
        },
        content = {
            SwipeRefresh(
                state = rememberSwipeRefreshState(refreshState.value == RefreshState.Loading),
                onRefresh = {
                    setErrorShown(false)
                    vm.refresh()
                }
            ) {
                if (currentCityName.value.isEmpty() && refreshState.value == RefreshState.PermissionError) {
                    LocationRequestScreen(
                        { navController.navigate(MainDestinations.CITY_SELECT_ROUTE) },
                        { showAppSettingsPage(context) },
                        { setLocationRationaleHidden(scope, context) },
                        vm::onPermissionGranted,
                        hideRationale.value
                    )
                } else if (refreshState.value == RefreshState.Loading && currentForecast.value == null) {
                    EmptyLoadingPage()
                } else {
                    HorizontalPager(state = pagerState) { page ->
                        when (page) {
                            0 -> CurrentForecast(
                                currentListState,
                                currentForecast.value,
                                alerts.value
                            )
                            1 -> HourlyForecast(hourlyListState, hourlyForecast.value)
                            2 -> DailyForecast(dailyListState, dailyForecast.value)
                        }
                    }
                }
            }
        }
    )
    if (!errorShown) {
        setErrorShown(showErrorMessage(refreshState.value, scope, scaffoldState, context))
    }
}

fun showErrorMessage(
    refreshState: RefreshState,
    scope: CoroutineScope,
    scaffoldState: ScaffoldState,
    context: Context
): Boolean {
    var shown = false
    (refreshState as? RefreshState.Error)?.let { error ->
        scaffoldState.snackbarHostState.run {
            // Prevent duplicate snackbars when loading multiple cities
            val message = context.getString(error.message)
            if (currentSnackbarData?.message != message) {
                scope.launch { showSnackbar(message) }
                shown = true
            }
        }
    }
    return shown
}

@Composable
fun EmptyLoadingPage() {
    LazyColumn(content = {}, modifier = Modifier.fillMaxSize())
}

@ExperimentalPagerApi
@Composable
fun ForecastAppBar(
    pagerState: PagerState,
    currentCityName: String,
    openDrawer: () -> Unit,
    removeLocation: () -> Unit,
    showSource: () -> Unit
) {
    val (showMenu, setShowMenu) = rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val tabTitles = stringArrayResource(R.array.tab_titles)
    Column {
        TopAppBar(
            title = { Text(if (currentCityName.isEmpty()) stringResource(R.string.your_location) else currentCityName) },
            navigationIcon = {
                IconButton(onClick = { openDrawer() }) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = stringResource(R.string.locations_menu)
                    )
                }
            },
            actions = {
                IconButton(onClick = { setShowMenu(true) }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.more_options_menu)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { setShowMenu(false) }
                ) {
                    if (currentCityName.isNotEmpty()) {
                        DropdownMenuItem(onClick = {
                            removeLocation()
                            setShowMenu(false)
                        }) {
                            Text(stringResource(R.string.remove_location))
                        }
                    }
                    DropdownMenuItem(onClick = {
                        showSource()
                        setShowMenu(false)
                    }) {
                        Text(stringResource(R.string.source_code))
                    }
                }
            }
        )
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                )
            }
        ) {
            tabTitles.mapIndexed { idx, title ->
                Tab(
                    text = { Text(title) },
                    selected = pagerState.currentPage == idx,
                    onClick = { scope.launch { pagerState.scrollToPage(idx) } }
                )
            }
        }
    }
}

@ExperimentalPermissionsApi
@Composable
private fun LocationRequestScreen(
    navigateToSearchScreen: () -> Unit,
    navigateToSettingsScreen: () -> Unit,
    hideRationale: () -> Unit,
    onPermissionGranted: () -> Unit,
    doNotShowRationaleInit: Boolean
) {
    val (doNotShowRationale, setDoNotShowRationale) = rememberSaveable {
        mutableStateOf(
            doNotShowRationaleInit
        )
    }

    val permissionState =
        rememberPermissionState(android.Manifest.permission.ACCESS_COARSE_LOCATION)

    when {
        permissionState.hasPermission -> {
            onPermissionGranted()
            EmptyLoadingPage()
        }
        permissionState.shouldShowRationale || !permissionState.permissionRequested -> {
            if (doNotShowRationale) {
                hideRationale()
                PermissionDenied(navigateToSettingsScreen, navigateToSearchScreen)
            } else {
                AskPermission(
                    permissionState,
                    { setDoNotShowRationale(true) },
                    navigateToSearchScreen
                )
            }
        }
        else -> {
            hideRationale()
            PermissionDenied(navigateToSettingsScreen, navigateToSearchScreen)
        }
    }
}

@ExperimentalPermissionsApi
@Composable
fun AskPermission(
    permissionState: PermissionState,
    setDoNotShowRationale: () -> Unit,
    navigateToSearchScreen: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.location_permission_request),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(onClick = { permissionState.launchPermissionRequest() }) {
                Text(stringResource(R.string.yes))
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = setDoNotShowRationale) {
                Text(stringResource(R.string.no_thanks), textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.manual_location_prompt))
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = navigateToSearchScreen) {
            Text(stringResource(R.string.search))
        }
    }
}

@Composable
fun PermissionDenied(navigateToSettingsScreen: () -> Unit, navigateToSearchScreen: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.location_permission_denied),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = navigateToSettingsScreen) {
            Text(stringResource(R.string.open_settings_prompt))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.manual_location_prompt))
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = navigateToSearchScreen) {
            Text(stringResource(R.string.search))
        }
    }
}

fun showAppSettingsPage(context: Context) {
    startActivity(
        context,
        Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", context.packageName, null)
        },
        null
    )
}

fun setLocationRationaleHidden(scope: CoroutineScope, context: Context) {
    scope.launch {
        context.dataStore.edit {
            it[HIDE_LOCATION_RATIONALE] = true
        }
    }
}

fun showSourceCode(context: Context) {
    startActivity(
        context,
        Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.source_code_url))),
        null
    )
}

@Composable
fun DrawerItem(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .background(if (selected) MaterialTheme.colors.primaryVariant else MaterialTheme.colors.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}