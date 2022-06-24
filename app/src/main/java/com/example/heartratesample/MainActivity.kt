package com.example.heartratesample

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import com.example.heartratesample.theme.WearAppTheme
import dagger.hilt.android.AndroidEntryPoint

lateinit var permissionLauncher: ActivityResultLauncher<String>
lateinit var viewModel: MainViewModel
var heartRateData: MutableState<String> = mutableStateOf("00.0")
var checkedState: MutableState<Boolean> = mutableStateOf(false)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainApp()
        }

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                when (result) {
                    true -> {
                        Log.i(TAG, "Body sensors permission granted")
                        viewModel.togglePassiveData(true)
                    }
                    false -> {
                        Log.i(TAG, "Body sensors permission not granted")
                        viewModel.togglePassiveData(false)
                    }
                }
            }

        lifecycleScope.launchWhenStarted {
            viewModel.latestHeartRate.collect {
                heartRateData.value = it.toString()
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.passiveDataEnabled.collect {
                checkedState.value = it
            }
        }
    }
}

@Composable
fun MainApp() {
    WearAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colors.background)
                .selectableGroup(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Heart Rate Sample")
            PermissionToggle()
            Card(onClick = {}) {
                Text(text = "Last measured")
                Row {
                    Image(
                        painter = painterResource(id = R.drawable.ic_heart),
                        contentDescription = ""
                    )
                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        text = heartRateData.value
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionToggle() {
    ToggleChip(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        checked = checkedState.value,
        toggleControl = {
            Icon(
                imageVector = ToggleChipDefaults.switchIcon(checked = checkedState.value),
                contentDescription = if (checkedState.value) "On" else "Off"
            )
        },
        onCheckedChange = {
            checkedState.value = it
            if (checkedState.value) {
                permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
            } else {
                viewModel.togglePassiveData(false)
            }
        },
        label = {
            Text(
                text = "Enable",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MainApp()
}
