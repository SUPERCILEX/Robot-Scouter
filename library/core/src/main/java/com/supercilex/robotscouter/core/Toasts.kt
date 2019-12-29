@file:Suppress("NOTHING_TO_INLINE", "unused")

package com.supercilex.robotscouter.core

import android.widget.Toast

/**
 * Display the simple Toast message with the [Toast.LENGTH_SHORT] duration.
 *
 * @param message the message text resource.
 */
inline fun toast(message: Int): Toast = Toast
        .makeText(RobotScouter, message, Toast.LENGTH_SHORT)
        .apply { show() }

/**
 * Display the simple Toast message with the [Toast.LENGTH_SHORT] duration.
 *
 * @param message the message text.
 */
inline fun toast(message: CharSequence): Toast = Toast
        .makeText(RobotScouter, message, Toast.LENGTH_SHORT)
        .apply { show() }

/**
 * Display the simple Toast message with the [Toast.LENGTH_LONG] duration.
 *
 * @param message the message text resource.
 */
inline fun longToast(message: Int): Toast = Toast
        .makeText(RobotScouter, message, Toast.LENGTH_LONG)
        .apply { show() }

/**
 * Display the simple Toast message with the [Toast.LENGTH_LONG] duration.
 *
 * @param message the message text.
 */
inline fun longToast(message: CharSequence): Toast = Toast
        .makeText(RobotScouter, message, Toast.LENGTH_LONG)
        .apply { show() }
