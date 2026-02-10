package ci.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ci.build.BuildResult;
import ci.build.CiTrigger;
import ci.build.BuildResult.Status;

public class StorageTest {

    /**
     * Tests that a build can be saved and retrieved from DB.
     */
    @Test
    void saveBuildStoresData() {
        BuildStore store = new BuildStore();

        BuildResult original = new BuildResult("12345", LocalDate.now().toString(), Status.SUCCESS, "OK", new ArrayList<>());
        store.save(new CiTrigger("repo", "assessment", "abc123"), original);

        BuildResult loaded = store.load("12345");

        assertEquals(original.buildId, loaded.buildId);
        assertEquals(original.date, loaded.date);
        assertEquals(original.status, loaded.status);
        assertEquals(original.logs, loaded.logs);

        store.remove("12345");
    }

    /**
     * Tests that all builds can be retrieved from DB.
     */
    @Test
    void testFindAllReturnsAllBuilds() {
        BuildStore store = new BuildStore();

        List<BuildResult> builds = store.loadAll();
        int startSize = builds.size();

        BuildResult build1 = new BuildResult("1", LocalDate.now().toString(), Status.SUCCESS, "OK", new ArrayList<>());
        store.save(new CiTrigger("repo", "assessment", "abc123"), build1);

        BuildResult build2 = new BuildResult("2", LocalDate.now().toString(), Status.SUCCESS, "OK", new ArrayList<>());
        store.save(new CiTrigger("repo", "assessment", "abc123"), build2);

        builds = store.loadAll();
        assertEquals(2, builds.size() - startSize);

        store.remove("1");
        store.remove("2");
    }

    /**
     * Tests that duplicate buildIds aren't stored twice nor crash server.
     */
    @Test
    void testUniqueBuildIds() {
        BuildStore store = new BuildStore();

        List<BuildResult> builds = store.loadAll();
        int startSize = builds.size();

        CiTrigger trigger = new CiTrigger("repo", "assessment", "abc123");

        BuildResult build = new BuildResult("1", LocalDate.now().toString(), Status.SUCCESS, "OK", new ArrayList<>());
        store.save(trigger, build);
        store.save(trigger, build);

        builds = store.loadAll();
        assertEquals(1, builds.size() - startSize);

        store.remove("1");
    }
}
