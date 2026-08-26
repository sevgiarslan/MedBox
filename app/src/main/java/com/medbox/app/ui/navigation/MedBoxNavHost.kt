package com.medbox.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medbox.app.data.MedicineRepository
import com.medbox.app.ui.editor.MedicineEditorScreen
import com.medbox.app.ui.editor.MedicineEditorViewModel
import com.medbox.app.ui.list.MedicineListScreen
import com.medbox.app.ui.list.MedicineListViewModel
import com.medbox.app.ui.scanner.BarcodeScannerScreen

private object Routes {
    const val LIST = "list"
    const val SCANNER = "scanner"
    const val EDITOR = "editor?medicineId={medicineId}"
    fun editor(medicineId: Long? = null) = "editor?medicineId=${medicineId ?: -1}"
}

@Composable
fun MedBoxNavHost(repository: MedicineRepository) {
    val navController = rememberNavController()
    var pendingScannedBarcode by remember { mutableStateOf<String?>(null) }

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            val viewModel: MedicineListViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { MedicineListViewModel(repository) }
                }
            )
            MedicineListScreen(
                viewModel = viewModel,
                onAddMedicine = { navController.navigate(Routes.editor()) },
                onOpenMedicine = { id -> navController.navigate(Routes.editor(id)) }
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("medicineId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getLong("medicineId") ?: -1L
            val medicineId = rawId.takeIf { it != -1L }

            val viewModel: MedicineEditorViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { MedicineEditorViewModel(repository, medicineId) }
                }
            )

            MedicineEditorScreen(
                viewModel = viewModel,
                onScanBarcode = { navController.navigate(Routes.SCANNER) },
                onFinished = { navController.popBackStack(Routes.LIST, inclusive = false) },
                onOpenExisting = { existingId ->
                    navController.navigate(Routes.editor(existingId)) {
                        popUpTo(Routes.LIST)
                    }
                },
                pendingScannedBarcode = pendingScannedBarcode,
                onConsumeScannedBarcode = { pendingScannedBarcode = null }
            )
        }

        composable(Routes.SCANNER) {
            BarcodeScannerScreen(
                onBarcodeScanned = { value ->
                    pendingScannedBarcode = value
                    navController.popBackStack()
                },
                onClose = { navController.popBackStack() }
            )
        }
    }
}
