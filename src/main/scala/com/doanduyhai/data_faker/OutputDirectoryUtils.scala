package com.doanduyhai.data_faker

import java.io.File


object OutputDirectoryUtils {
  private val DirectoryPrefix = "generated"

  def createOutputDirectory(): String = {
    val dir = new File(DirectoryPrefix)
    if (!dir.exists) dir.mkdirs
    DirectoryPrefix
  }


}
