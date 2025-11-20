package com.example.storageapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NotesScreen()
                }
            }
        }
    }
}

@Composable
fun NotesScreen(vm: NotesViewModel = viewModel()) {
    val ctx = LocalContext.current
    val resolver = ctx.contentResolver

    val fileName by vm.fileName.collectAsState()
    val content by vm.content.collectAsState()
    val target by vm.target.collectAsState()
    val lastUri by vm.lastExternalUri.collectAsState()

    // SAF launchers
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            // persist consent agar bisa dipakai ulang
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_ : Exception) {}
            vm.setExternalUri(it)
            vm.saveExternal(resolver)
            Toast.makeText(ctx, "Berkas dibuat & disimpan.", Toast.LENGTH_SHORT).show()
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_ : Exception) {}
            vm.setExternalUri(it)
            vm.openExternal(resolver)
            Toast.makeText(ctx, "Berkas dibuka.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier
            .padding(16.dp)
            .padding(top = 50.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Aplikasi Penyimpanan Internal & Eksternal",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = fileName,
            onValueChange = vm::setFileName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nama berkas") }
        )

        OutlinedTextField(
            value = content,
            onValueChange = vm::setContent,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            label = { Text("Isi catatan") }
        )

        // Target storage selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Simpan ke:")
            FilterChip(
                selected = target == NotesViewModel.Target.INTERNAL,
                onClick = { vm.setTarget(NotesViewModel.Target.INTERNAL) },
                label = { Text("Internal") }
            )
            FilterChip(
                selected = target == NotesViewModel.Target.EXTERNAL_SAF,
                onClick = { vm.setTarget(NotesViewModel.Target.EXTERNAL_SAF) },
                label = { Text("Eksternal (SAF)") }
            )
        }

        // Actions
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                when (target) {
                    NotesViewModel.Target.INTERNAL -> vm.saveInternal().also {
                        Toast.makeText(ctx, "Tersimpan di internal.", Toast.LENGTH_SHORT).show()
                    }
                    NotesViewModel.Target.EXTERNAL_SAF -> {
                        if (lastUri != null) {
                            vm.saveExternal(resolver)
                            Toast.makeText(ctx, "Tersimpan ke berkas SAF.", Toast.LENGTH_SHORT).show()
                        } else {
                            val suggested = if (fileName.isNotBlank()) fileName else "catatan.txt"
                            createDocLauncher.launch(suggested)
                        }
                    }
                }
            }) {
                Text("Simpan / Perbarui")
            }

            OutlinedButton(onClick = {
                when (target) {
                    NotesViewModel.Target.INTERNAL -> vm.openInternal().also {
                        Toast.makeText(ctx, "Dibuka dari internal (jika ada).", Toast.LENGTH_SHORT).show()
                    }
                    NotesViewModel.Target.EXTERNAL_SAF -> {
                        if (lastUri != null) {
                            vm.openExternal(resolver)
                        } else {
                            openDocLauncher.launch(arrayOf("text/plain"))
                        }
                    }
                }
            }) {
                Text("Buka")
            }

            OutlinedButton(onClick = {
                when (target) {
                    NotesViewModel.Target.INTERNAL -> vm.deleteInternal().also {
                        Toast.makeText(ctx, "Dihapus dari internal (jika ada).", Toast.LENGTH_SHORT).show()
                    }
                    NotesViewModel.Target.EXTERNAL_SAF -> {
                        if (lastUri != null) {
                            vm.deleteExternal(resolver)
                            Toast.makeText(ctx, "Berkas SAF dihapus.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(ctx, "Pilih berkas terlebih dahulu.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }) {
                Text("Hapus")
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn {
            item {
                Text("Uri eksternal terakhir: ${lastUri ?: "(belum dipilih)"}")
            }
        }
    }
}