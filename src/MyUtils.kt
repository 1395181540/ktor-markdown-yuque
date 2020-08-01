package com.khassar

import java.io.File

object MyUtils {
    fun getFileSerialNumberString(num:Int): String {
        if (num<10){
            return "0"+num+"."
        }else if (num>=10){
            return num.toString()+"."
        }else return ""

    }

}

