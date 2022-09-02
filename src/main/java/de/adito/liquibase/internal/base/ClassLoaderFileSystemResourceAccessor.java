package de.adito.liquibase.internal.base;

import liquibase.resource.*;

import java.io.*;

/**
 * FileSystemResourceAccessor, but loads embedded .xsd files
 * directly from the class loader for performance reasons
 *
 * @author p.neub, 01.09.2022
 */
public class ClassLoaderFileSystemResourceAccessor extends FileSystemResourceAccessor
{
  private ClassLoaderResourceAccessor classLoaderResourceAccessor = new ClassLoaderResourceAccessor();

  public ClassLoaderFileSystemResourceAccessor(File... baseDirsAndFiles)
  {
    super(baseDirsAndFiles);
  }

  @Override
  public InputStreamList openStreams(String relativeTo, String streamPath) throws IOException
  {
    if (streamPath != null && streamPath.toLowerCase().endsWith(".xsd"))
      return classLoaderResourceAccessor.openStreams(relativeTo, streamPath);
    return super.openStreams(relativeTo, streamPath);
  }
}
