package com.khassar

import java.io.File
import java.util.regex.Pattern

object MyUtils {
    fun getFileSerialNumberString(num:Int): String {
        if (num<10){
            return "0"+num+"."
        }else if (num>=10){
            return num.toString()+"."
        }else return ""

    }
    fun filenameFilter(str: String?): String? {
        return if (str == null) null else Pattern.compile("[\\\\/:*?\"<>|]").matcher(str).replaceAll("")
    }
}

