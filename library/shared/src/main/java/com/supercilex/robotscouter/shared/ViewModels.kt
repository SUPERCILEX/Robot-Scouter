package com.supercilex.robotscouter.shared

import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateVMFactory
import androidx.lifecycle.ViewModel

inline fun <reified VM : ViewModel> FragmentActivity.stateViewModels() =
        viewModels<VM> { SavedStateVMFactory(this) }

inline fun <reified VM : ViewModel> Fragment.stateViewModels() =
        viewModels<VM> { SavedStateVMFactory(this) }
