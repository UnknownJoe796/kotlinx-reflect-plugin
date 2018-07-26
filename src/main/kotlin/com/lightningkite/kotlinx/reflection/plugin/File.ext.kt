package com.lightningkite.kotlinx.reflection.plugin

import java.io.File


fun File.recurse(onEach: (File) -> Unit) {
    if (this.isDirectory) {
        for (item in this.listFiles()) {
            item.recurse(onEach)
        }
    } else {
        onEach.invoke(this)
    }
}