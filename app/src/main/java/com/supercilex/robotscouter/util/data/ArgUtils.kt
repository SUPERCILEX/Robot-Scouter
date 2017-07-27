package com.supercilex.robotscouter.util.data

import android.os.Build
import android.os.Parcel
import android.os.PersistableBundle
import android.support.annotation.RequiresApi

fun Parcel.readBooleanCompat() = getBooleanForInt(readInt())

fun Parcel.writeBooleanCompat(value: Boolean) = writeInt(getIntForBoolean(value))

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun PersistableBundle.getBooleanCompat(key: String) = getBooleanForInt(getInt(key))

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun PersistableBundle.putBooleanCompat(key: String, value: Boolean) =
        putInt(key, getIntForBoolean(value))

private fun getBooleanForInt(value: Int) = value == 1

private fun getIntForBoolean(value: Boolean) = if (value) 1 else 0
