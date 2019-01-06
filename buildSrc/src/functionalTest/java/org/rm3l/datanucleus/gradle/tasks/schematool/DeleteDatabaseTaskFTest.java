package org.rm3l.datanucleus.gradle.tasks.schematool;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.rm3l.datanucleus.gradle.utils.DataNucleusPluginTestExtension;
import org.rm3l.datanucleus.gradle.utils.ExpectedSystemExit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.rm3l.datanucleus.gradle.utils.TestUtils.*;

@SuppressWarnings("Duplicates")
@ExpectedSystemExit
class DeleteDatabaseTaskFTest {

    @RegisterExtension
    final DataNucleusPluginTestExtension dataNucleusPluginTestExtension
            = new DataNucleusPluginTestExtension(
            (persistenceUnitMetaData, testPersistenceUnitMetaData) -> {
                persistenceUnitMetaData.addProperty("javax.persistence.jdbc.url",
                        "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_UPPER=FALSE");
                persistenceUnitMetaData.addProperty("javax.persistence.jdbc.driver", "org.h2.Driver");
                persistenceUnitMetaData.addProperty("javax.persistence.jdbc.user", "SA");
                persistenceUnitMetaData.addProperty("javax.persistence.jdbc.password", "");
                persistenceUnitMetaData.addProperty("datanucleus.schema.autoCreateAll", "true");
            });

    @Test
    @DisplayName("should succeed deleting the database against an in-memory datastore")
    void test_DeleteDatabase_does_succeed(@DataNucleusPluginTestExtension.TempDir Path tempDir) throws IOException {
        final Path buildGradle = tempDir.resolve("build.gradle");
        Files.write(buildGradle,
                ("plugins { id 'org.rm3l.datanucleus-gradle-plugin' }\n\n" +
                        "repositories {\n" +
                        "  mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "  compile 'org.datanucleus:datanucleus-accessplatform-jpa-rdbms:" + DN_JPA_RDBMS_VERSION + "'\n" +
                        "  compile 'com.h2database:h2:" + H2_VERSION + "'\n" +
                        "  testCompile 'junit:junit:" + JUNIT_VERSION + "'\n" +
                        "}\n" +
                        "\n" +
                        "datanucleus {\n" +
                        "  schemaTool {\n" +
                        "    api 'JPA'\n" +
                        "    persistenceUnitName 'myPersistenceUnit'\n" +
                        "    schemaName 'mySchemaName'\n" +
                        "    catalogName 'myCatalogName'\n" +
                        "  }\n" +
                        "}\n")
                        .getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        //This does not make the build fail. Instead, a stacktrace is output by DataNucleus Enhancer
        BuildResult result = gradle(tempDir, "build", "deleteDatabase");
        assertNotNull(result);
        BuildTask deleteDatabaseTask = result.task(":deleteDatabase");
        assertNotNull(deleteDatabaseTask);
        assertSame(SUCCESS, deleteDatabaseTask.getOutcome());
        String output = result.getOutput();
        assertNotNull(output);
        assertTrue(output.contains("DataNucleus SchemaTool : Deletion of the database (cat=myCatalogName, sch=mySchemaName)"));
        assertTrue(output.contains("DataNucleus SchemaTool completed successfully"));
    }

    @Test
    @DisplayName("should not succeed deleting the database against an in-memory datastore, if schema or catalog are missing")
    void test_DeleteDatabase_does_not_succeed_if_no_sch_and_cat(
            @DataNucleusPluginTestExtension.TempDir Path tempDir) throws IOException {
        final Path buildGradle = tempDir.resolve("build.gradle");
        Files.write(buildGradle,
                ("plugins { id 'org.rm3l.datanucleus-gradle-plugin' }\n\n" +
                        "repositories {\n" +
                        "  mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "  compile 'org.datanucleus:datanucleus-accessplatform-jpa-rdbms:" + DN_JPA_RDBMS_VERSION + "'\n" +
                        "  compile 'com.h2database:h2:" + H2_VERSION + "'\n" +
                        "  testCompile 'junit:junit:" + JUNIT_VERSION + "'\n" +
                        "}\n" +
                        "\n" +
                        "datanucleus {\n" +
                        "  schemaTool {\n" +
                        "    api 'JPA'\n" +
                        "    persistenceUnitName 'myPersistenceUnit'\n" +
                        "  }\n" +
                        "}\n")
                        .getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        //This does not make the build fail. Instead, a stacktrace is output by DataNucleus Enhancer
        BuildResult result = gradle(tempDir, false, "build", "deleteDatabase");
        assertNotNull(result);
        BuildTask deleteDatabaseTask = result.task(":deleteDatabase");
        assertNotNull(deleteDatabaseTask);
        assertSame(FAILED, deleteDatabaseTask.getOutcome());
        String output = result.getOutput();
        assertNotNull(output);
        assertTrue(output.contains("Missing option: schemaName") || output.contains("Missing option: catalogName"));
        assertFalse(output.contains("DataNucleus SchemaTool : Deletion of the database (cat=myCatalogName, sch=mySchemaName)"));
        assertFalse(output.contains("DataNucleus SchemaTool completed successfully"));
    }

    @Test
    @DisplayName("should not succeed deleting the database against an in-memory datastore, if schema is missing")
    void test_DeleteDatabase_does_not_succeed_if_no_sch(
            @DataNucleusPluginTestExtension.TempDir Path tempDir) throws IOException {
        final Path buildGradle = tempDir.resolve("build.gradle");
        Files.write(buildGradle,
                ("plugins { id 'org.rm3l.datanucleus-gradle-plugin' }\n\n" +
                        "repositories {\n" +
                        "  mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "  compile 'org.datanucleus:datanucleus-accessplatform-jpa-rdbms:" + DN_JPA_RDBMS_VERSION + "'\n" +
                        "  compile 'com.h2database:h2:" + H2_VERSION + "'\n" +
                        "  testCompile 'junit:junit:" + JUNIT_VERSION + "'\n" +
                        "}\n" +
                        "\n" +
                        "datanucleus {\n" +
                        "  schemaTool {\n" +
                        "    api 'JPA'\n" +
                        "    persistenceUnitName 'myPersistenceUnit'\n" +
                        "    catalogName 'myCatalogName'\n" +
                        "  }\n" +
                        "}\n")
                        .getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        //This does not make the build fail. Instead, a stacktrace is output by DataNucleus Enhancer
        BuildResult result = gradle(tempDir, false, "build", "deleteDatabase");
        assertNotNull(result);
        BuildTask deleteDatabaseTask = result.task(":deleteDatabase");
        assertNotNull(deleteDatabaseTask);
        assertSame(FAILED, deleteDatabaseTask.getOutcome());
        String output = result.getOutput();
        assertNotNull(output);
        assertTrue(output.contains("Missing option: schemaName"));
        assertFalse(output.contains("DataNucleus SchemaTool : Deletion of the database (cat=myCatalogName, sch=mySchemaName)"));
        assertFalse(output.contains("DataNucleus SchemaTool completed successfully"));
    }

    @Test
    @DisplayName("should not succeed deleting the database against an in-memory datastore, if catalog is missing")
    void test_DeleteDatabase_does_not_succeed_if_no_cat(
            @DataNucleusPluginTestExtension.TempDir Path tempDir) throws IOException {
        final Path buildGradle = tempDir.resolve("build.gradle");
        Files.write(buildGradle,
                ("plugins { id 'org.rm3l.datanucleus-gradle-plugin' }\n\n" +
                        "repositories {\n" +
                        "  mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "  compile 'org.datanucleus:datanucleus-accessplatform-jpa-rdbms:" + DN_JPA_RDBMS_VERSION + "'\n" +
                        "  compile 'com.h2database:h2:" + H2_VERSION + "'\n" +
                        "  testCompile 'junit:junit:" + JUNIT_VERSION + "'\n" +
                        "}\n" +
                        "\n" +
                        "datanucleus {\n" +
                        "  schemaTool {\n" +
                        "    api 'JPA'\n" +
                        "    persistenceUnitName 'myPersistenceUnit'\n" +
                        "    schemaName 'mySchemaName'\n" +
                        "  }\n" +
                        "}\n")
                        .getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        //This does not make the build fail. Instead, a stacktrace is output by DataNucleus Enhancer
        BuildResult result = gradle(tempDir, false, "build", "deleteDatabase");
        assertNotNull(result);
        BuildTask deleteDatabaseTask = result.task(":deleteDatabase");
        assertNotNull(deleteDatabaseTask);
        assertSame(FAILED, deleteDatabaseTask.getOutcome());
        String output = result.getOutput();
        assertNotNull(output);
        assertTrue(output.contains("Missing option: catalogName"));
        assertFalse(output.contains("DataNucleus SchemaTool : Deletion of the database (cat=myCatalogName, sch=mySchemaName)"));
        assertFalse(output.contains("DataNucleus SchemaTool completed successfully"));
    }

    @Test
    @DisplayName("should succeed running the 'deleteDatabase' task from CLI")
    void test_run_deleteDatabase_task_cli_succeeds(@DataNucleusPluginTestExtension.TempDir Path tempDir) throws IOException {
        final File log4jConfFile = Files.createFile(tempDir.resolve("log4j.conf")).toFile();
        final File jdkLogConfFile = Files.createFile(tempDir.resolve("jdkLog.conf")).toFile();

        final Path buildGradle = tempDir.resolve("build.gradle");
        Files.write(buildGradle,
                ("plugins { id 'org.rm3l.datanucleus-gradle-plugin' }\n\n" +
                        "repositories {\n" +
                        "  mavenCentral()\n" +
                        "}\n" +
                        "\n" +
                        "dependencies {\n" +
                        "  compile 'org.datanucleus:datanucleus-accessplatform-jpa-rdbms:" + DN_JPA_RDBMS_VERSION + "'\n" +
                        "  compile 'com.h2database:h2:" + H2_VERSION + "'\n" +
                        "  testCompile 'junit:junit:" + JUNIT_VERSION + "'\n" +
                        "}\n" +
                        "\n")
                        .getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        BuildResult enhanceResult = gradle(tempDir,
                "enhance",
                "--api", "JPA",
                "--persistence-unit-name", "myPersistenceUnit");
        assertNotNull(enhanceResult);
        BuildTask enhanceTask = enhanceResult.task(":enhance");
        assertNotNull(enhanceTask);
        assertSame(SUCCESS, enhanceTask.getOutcome());
        String output = enhanceResult.getOutput();
        assertNotNull(output);
        assertTrue(output.contains("DataNucleus Enhancer completed with success for 1 classes."));

        BuildResult result = gradle(tempDir,
                "deleteDatabase",
                "--api", "JPA",
                "--catalog-name", "myCatalogName",
                "--schema-name", "mySchemaName",
                "--persistence-unit-name", "myPersistenceUnit",
                "--jdk-log-conf", jdkLogConfFile.getAbsolutePath(),
                "--log4j-conf", log4jConfFile.getAbsolutePath());
        assertNotNull(result);
        BuildTask deleteDatabaseTask = result.task(":deleteDatabase");
        assertNotNull(deleteDatabaseTask);
        assertSame(SUCCESS, deleteDatabaseTask.getOutcome());
    }
}
