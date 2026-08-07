#!/bin/bash
sed -i 's/class WorkspaceViewModelFactory(/class WorkspaceViewModelFactory(private val context: android.content.Context, /' app/src/main/java/com/example/ui/WorkspaceViewModel.kt
sed -i 's/return WorkspaceViewModel(workspaceDao, settingsRepository)/return WorkspaceViewModel(context, workspaceDao, settingsRepository)/' app/src/main/java/com/example/ui/WorkspaceViewModel.kt
sed -i 's/class WorkspaceViewModel(/class WorkspaceViewModel(private val context: android.content.Context, /' app/src/main/java/com/example/ui/WorkspaceViewModel.kt
sed -i 's/WorkspaceViewModelFactory(database.workspaceDao(), settingsRepository)/WorkspaceViewModelFactory(applicationContext, database.workspaceDao(), settingsRepository)/' app/src/main/java/com/example/MainActivity.kt
