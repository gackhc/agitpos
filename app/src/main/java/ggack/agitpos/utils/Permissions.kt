package ggack.agitpos.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object Permissions{
    @Synchronized
    fun requestPermission(context : Context, permission : String, requestCode : Int, tip : String?) : Boolean {
        return if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permission)) {
                tip?.let {
                    context.runOnUiThread {
                        val intent: Intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(
                                Uri.parse(
                                    "package:" + context.packageName
                                )
                            )
                        context.startActivity(intent)
                    }
                }
            } else {
                ActivityCompat.requestPermissions(context, arrayOf(permission), requestCode)
                return false
            }
            false
        } else {
            true
        }
    }
    @Synchronized
    fun requestPermission(context : Context, permission : Array<String>, requestCode : Int, tip : String?) : Boolean {
        var ret = true
        permission.forEach {
            if(ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED)
                ret = false
        }

        return if (ret)
            true
        else {
            var change = false
            permission.forEach {
                if(ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, it))
                    change = true
            }
            if (change) {
                tip?.run{
                    Toast.makeText(context, tip, Toast.LENGTH_SHORT).show()
                }
                val intent : Intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + context.packageName))
                context.startActivity(intent)
            } else {
                ActivityCompat.requestPermissions(context as Activity, permission, requestCode)
                return false
            }
            false
        }
    }
}