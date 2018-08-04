package com.supercilex.robotscouter.core.ui

import android.os.Bundle
import android.os.Parcelable
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.parcel.Parcelize

val inputMethodManager by lazy { checkNotNull(RobotScouter.getSystemService<InputMethodManager>()) }

fun Window.setKeyboardModeVisible() {
    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
}

fun View.showKeyboard() {
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

inline fun EditText.setImeOnDoneListener(
        crossinline listener: () -> Unit
) = setOnEditorActionListener { _, actionId, event: KeyEvent? ->
    if (event?.keyCode == KeyEvent.KEYCODE_ENTER) {
        if (event.action == KeyEvent.ACTION_UP) listener()

        // We need to return true even if we didn't handle the event to continue
        // receiving future callbacks.
        true
    } else if (actionId == EditorInfo.IME_ACTION_DONE) {
        listener()
        true
    } else {
        false
    }
}

interface KeyboardShortcutListener {
    fun registerShortcut(keyCode: Int, @StringRes description: Int, action: () -> Unit) =
            registerShortcut(keyCode, 0, description, action)

    fun registerShortcut(
            keyCode: Int,
            metaState: Int,
            @StringRes description: Int,
            action: () -> Unit
    )
}

internal class ShortcutManager(private val activity: FragmentActivity) : KeyboardShortcutListener {
    private val shortcuts = mutableListOf<Item>()

    override fun registerShortcut(
            keyCode: Int,
            metaState: Int,
            description: Int,
            action: () -> Unit
    ) {
        shortcuts += Item(Shortcut(keyCode, metaState, description), action)
    }

    /** @return true if the shortcut was handled, false otherwise */
    fun onEvent(event: KeyEvent): Boolean {
        event.metaState
        val processed = shortcuts
                .filter { (shortcut) ->
                    shortcut.keyCode == event.keyCode && shortcut.metaState == event.metaState
                }
                .onEach { it.action() }
                .isNotEmpty()

        if (!processed && event.keyCode == KeyEvent.KEYCODE_SLASH && event.isShiftPressed) {
            ShortcutDisplayer.show(activity.supportFragmentManager, shortcuts.map { it.shortcut })
            return true
        }

        return processed
    }

    data class Item(val shortcut: Shortcut, val action: () -> Unit)

    @Parcelize
    data class Shortcut(val keyCode: Int, val metaState: Int, val description: Int) : Parcelable
}

internal class ShortcutDisplayer : DialogFragmentBase() {
    override val containerView by unsafeLazy {
        null as View?
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = AlertDialog.Builder(requireContext())
            .setTitle("Shortcuts")
            .setView(containerView)
            .setPositiveButton(android.R.string.ok, null)
            .create { /*onViewCreated(containerView, savedInstanceState)*/ }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    }

    companion object {
        private const val TAG = "ShortcutDisplayer"
        private const val SHORTCUTS_KEY = "shortcuts_key"

        fun show(manager: FragmentManager, shortcuts: List<ShortcutManager.Shortcut>) =
                ShortcutDisplayer().show(manager, TAG) {
                    putParcelableArray(SHORTCUTS_KEY, shortcuts.toTypedArray())
                }
    }
}
