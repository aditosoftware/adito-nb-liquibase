package de.adito.liquibase.internal.executors;

import liquibase.exception.LiquibaseException;
import lombok.NonNull;
import org.apache.commons.io.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link LiquibaseExecutorFacadeImpl}
 *
 * @author s.seemann, 08.07.2021
 */
class LiquibaseExecutorFacadeImplTest
{
  @SuppressWarnings("MethodOnlyUsedFromInnerClass")
  private static Stream<Arguments> provideFilePaths()
  {
    return Stream.of(
        Arguments.of("normalChangelogUnmodified.xml", "normalChangelogModified.xml"),
        Arguments.of("changelogWithoutCreateUnmodified.xml", "changelogWithoutCreateModified.xml")
    );
  }

  @Nested
  class ModifyChangelog
  {
    @ParameterizedTest
    @MethodSource("de.adito.liquibase.internal.executors.LiquibaseExecutorFacadeImplTest#provideFilePaths")
    void shouldReplaceEverything(@NonNull String pPathUnmodified, @NonNull String pPathModified) throws IOException, LiquibaseException
    {
      // Load unmodified File
      File file = Files.createTempFile("LiquibaseExecutorFacadeImplTest_changelog", ".xml").toFile();
      file.deleteOnExit();
      try (OutputStream outputStream = new FileOutputStream(file))
      {
        IOUtils.copy(LiquibaseExecutorFacadeImplTest.class.getResourceAsStream(pPathUnmodified), outputStream);
      }

      // modify
      LiquibaseExecutorFacadeImpl facade = new LiquibaseExecutorFacadeImpl();
      facade.modifyChangeset(file, "testAuthor", "987");

      // check if it is equal to expected one
      String expected = IOUtils.toString(LiquibaseExecutorFacadeImplTest.class.getResourceAsStream(pPathModified),
                                         StandardCharsets.UTF_8);
      String actually = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
      assertEquals(expected.replace("\r", ""), actually.replace("\r", ""));
    }
  }
}