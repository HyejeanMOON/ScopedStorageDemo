package com.hyejeanmoon.scopedstoragedemo

import android.os.StatFs

object FileUtil {
    fun getAvailableSize(path: String?): Long {
        var size: Long = -1

        if (path != null) {
            val fs = StatFs(path)
            val blockSize = fs.blockSizeLong
            val availableBlockSize = fs.availableBlocksLong
            size = blockSize * availableBlockSize
        }
        return size
    }
}