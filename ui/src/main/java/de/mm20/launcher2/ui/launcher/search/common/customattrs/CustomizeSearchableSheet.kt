package de.mm20.launcher2.ui.launcher.search.common.customattrs

import android.graphics.drawable.InsetDrawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.badges.Badge
import de.mm20.launcher2.icons.CustomIconWithPreview
import de.mm20.launcher2.search.data.Searchable
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.BottomSheetDialog
import de.mm20.launcher2.ui.component.ShapedLauncherIcon
import de.mm20.launcher2.ui.ktx.toPixels
import de.mm20.launcher2.ui.locals.LocalGridColumns
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeSearchableSheet(
    searchable: Searchable,
    onDismiss: () -> Unit,
) {
    val viewModel: CustomizeSearchableSheetVM =
        remember(searchable) { CustomizeSearchableSheetVM(searchable) }
    val context = LocalContext.current

    val pickIcon by viewModel.isIconPickerOpen.observeAsState(false)

    BottomSheetDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (pickIcon) R.string.icon_picker_title else R.string.menu_customize))
        },
        swipeToDismiss = { !pickIcon },
        dismissOnBackPress = { !pickIcon },
        confirmButton = {
            if (pickIcon) {
                OutlinedButton(onClick = { viewModel.closeIconPicker() }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            } else {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.close))
                }
            }
        }
    ) {
        if (!pickIcon) {
            Column(
                modifier = Modifier
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val iconSize = 64.dp
                val iconSizePx = iconSize.toPixels()
                val icon by remember { viewModel.getIcon(iconSizePx.toInt()) }.collectAsState(null)
                val primaryColor = MaterialTheme.colorScheme.onSecondary
                val badgeDrawable = remember {
                    InsetDrawable(
                        AppCompatResources.getDrawable(context, R.drawable.ic_edit),
                        8
                    ).also {
                        it.setTint(primaryColor.toArgb())
                    }
                }

                ShapedLauncherIcon(
                    size = iconSize,
                    icon = icon,
                    badge = Badge(
                        icon = badgeDrawable
                    ),
                    onClick = {
                        viewModel.openIconPicker()
                    }
                )
                OutlinedTextField(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .clickable {
                            Toast
                                .makeText(context, "Soon™", Toast.LENGTH_SHORT)
                                .show()
                        },
                    enabled = false,
                    value = searchable.label,
                    onValueChange = {},
                    placeholder = {
                        Text(searchable.label)
                    },
                )
            }
        } else {
            val iconSize = 48.dp
            val iconSizePx = iconSize.toPixels()

            val scope = rememberCoroutineScope()

            val suggestions by remember { viewModel.getIconSuggestions(iconSizePx.toInt()) }
                .observeAsState(emptyList())

            val defaultIcon by remember {
                viewModel.getDefaultIcon(iconSizePx.toInt())
            }.observeAsState()

            var query by remember { mutableStateOf("") }
            val isSearching by viewModel.isSearchingIcons.observeAsState(initial = false)
            val iconResults by viewModel.iconSearchResults.observeAsState(emptyList())

            val columns = LocalGridColumns.current

            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Fixed(columns)
            ) {

                item(span = { GridItemSpan(columns) }) {
                    OutlinedTextField(
                        modifier = Modifier.padding(bottom = 16.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = {
                                    query = ""
                                    scope.launch {
                                        viewModel.searchIcon("")
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = null
                                    )
                                }
                            }
                        },
                        value = query,
                        onValueChange = {
                            query = it
                            scope.launch {
                                viewModel.searchIcon(query)
                            }
                        },
                        label = {
                            Text(stringResource(R.string.icon_picker_search_icon))
                        },
                        singleLine = true,
                    )
                }

                if (query.isEmpty()) {
                    if (defaultIcon != null) {
                        item(span = { GridItemSpan(columns) }) {
                            Separator(stringResource(R.string.icon_picker_default_icon))
                        }
                        item {
                            IconPreview(item = defaultIcon, iconSize = iconSize, onClick = {
                                viewModel.pickIcon(null)
                            })
                        }
                    }
                    item(span = { GridItemSpan(columns) }) {
                        Separator(stringResource(R.string.icon_picker_suggestions))
                    }

                    items(suggestions) {
                        IconPreview(
                            it,
                            iconSize,
                            onClick = { viewModel.pickIcon(it.customIcon) }
                        )
                    }
                } else {

                    items(iconResults) {
                        IconPreview(
                            it,
                            iconSize,
                            onClick = { viewModel.pickIcon(it.customIcon) }
                        )
                    }

                    if (isSearching) {
                        item(span = { GridItemSpan(columns) }) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .size(24.dp)
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}

@Composable
fun IconPreview(
    item: CustomIconWithPreview?,
    iconSize: Dp,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        ShapedLauncherIcon(
            size = iconSize,
            icon = item?.preview,
            onClick = onClick
        )
    }
}

@Composable
fun Separator(label: String) {
    Text(
        label,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier
            .padding(top = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
    )
}