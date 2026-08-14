package com.app.forgefocus.features.mountains.presentation.viewmodel

sealed class MountainsEvent {
    data object BlockBroken : MountainsEvent()
    data object GoalCreated : MountainsEvent()
    data object GoalDeleted : MountainsEvent()
    data class Error(val message: String) : MountainsEvent()
}