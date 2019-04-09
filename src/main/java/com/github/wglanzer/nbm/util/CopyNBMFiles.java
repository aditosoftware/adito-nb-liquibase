package com.github.wglanzer.nbm.util;

/**
 * @author t.tasior, 15.03.2019
 */

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author a.arnold, 22.10.2018
 */
public class CopyNBMFiles
{
  public static void main(String[] args) throws IOException {
    File targetFolder = new File("C:\\entwicklungszweige\\0.0\\workingdir\\nbp_userdir\\modules");
    FileUtils.deleteDirectory(targetFolder);
//        targetFolder.mkdir();
    File sourcefolder = new File("C:\\entwicklungszweige\\nb-liquibase\\target\\nbm\\netbeans\\extra\\modules");
    FileUtils.copyDirectory(sourcefolder, targetFolder);
  }
}
