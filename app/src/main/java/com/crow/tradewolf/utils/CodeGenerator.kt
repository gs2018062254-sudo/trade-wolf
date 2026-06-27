package com.crow.tradewolf.utils

import java.util.UUID

object CodeGenerator {
    fun generarCodigoUnico(): String {
        return "TW-" + UUID.randomUUID().toString().take(8).uppercase()
    }
}