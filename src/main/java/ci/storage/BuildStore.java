package ci.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ci.build.BuildResult;
import ci.build.CiTrigger;
import ci.build.StepResult;

public class BuildStore implements storage {

    private final String url = "jdbc:sqlite:builds.db";

    /**
     * Constructor.
     * Create server persistence.
     */
    public BuildStore() {
        try {
            createTables();
        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Set up database to persist build history if it doeasn't already exist.
     * @throws SQLException
     */
    private void createTables() throws SQLException {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            conn.createStatement().execute("PRAGMA foreign_keys = ON");

            String createBuilds = """
                CREATE TABLE IF NOT EXISTS builds (
                    build_id TEXT PRIMARY KEY NOT NULL,
                    date TEXT NOT NULL,
                    commit_sha TEXT NOT NULL,
                    branch TEXT NOT NULL,
                    status TEXT NOT NULL,
                    logs TEXT
                )
                """;
            stmt.execute(createBuilds);

            String createSteps = """
                CREATE TABLE IF NOT EXISTS steps (
                    id INTEGER PRIMARY KEY NOT NULL,
                    build_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    success INTEGER NOT NULL,
                    logs TEXT,
                    FOREIGN KEY(build_id) REFERENCES builds(build_id) ON DELETE CASCADE
                )
                """;
            stmt.execute(createSteps);
        }
    }

    @Override
    public void save(CiTrigger trigger, BuildResult result){
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);

            String insertBuild = "INSERT INTO builds(build_id, date, commit_sha, branch, status, logs) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertBuild)) {
                stmt.setString(1, result.buildId);
                stmt.setString(2, result.date);
                stmt.setString(3, trigger.commitSha);
                stmt.setString(4, trigger.branch);
                stmt.setString(5, result.status.toString());
                stmt.setString(6, result.logs);
                stmt.executeUpdate();
            }

            String insertStep = "INSERT INTO steps(build_id, name, success, logs) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertStep)) {
                for (StepResult step : result.steps) {
                    stmt.setString(1, result.buildId);
                    stmt.setString(2, step.name);
                    stmt.setBoolean(3, step.success);
                    stmt.setString(4, step.logs);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            conn.commit();

        } catch (SQLException e) {
            System.err.println("Failed to store build " + result.buildId);
            e.printStackTrace();
        }
    }

    /**
     * Load a build from buildId.
     * @param buildId of build to load
     * @return searched build
     * @throws SQLException
     */
    public BuildResult load(String buildId) {
        BuildResult build;
        try (Connection conn = connect()) {

            String selectBuild = "SELECT * FROM builds WHERE build_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(selectBuild)) {
                stmt.setString(1, buildId);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) return null;

                String commitSha = rs.getString("commit_sha");
                String branch = rs.getString("branch");
                BuildResult.Status status = BuildResult.Status.valueOf(rs.getString("status"));
                String logs = rs.getString("logs");
                String date = rs.getString("date");
                
                List<StepResult> steps = new ArrayList<>();
                String selectSteps = "SELECT * FROM steps WHERE build_id = ?";
                try (PreparedStatement stepStmt = conn.prepareStatement(selectSteps)) {
                    stepStmt.setString(1, buildId);
                    ResultSet stepRs = stepStmt.executeQuery();

                    while (stepRs.next()) {
                        String name = stepRs.getString("name");
                        boolean success = stepRs.getInt("success") == 1;
                        String stepLogs = stepRs.getString("logs");
                        steps.add(new StepResult(name, success, stepLogs));
                    }
                }

                build = new BuildResult(buildId, date, status, logs, steps);
            }

            
        } catch (SQLException e) {
            System.err.println("Failed to load build " + buildId);
            return null;
        }
        return build;
    }

    /**
     * Load all builds from persistence.
     * @return full build history
     */
    public List<BuildResult> loadAll() {
        List<BuildResult> builds = new ArrayList<>();

        String sql = "SELECT build_id FROM builds ORDER BY date DESC";

        try (Connection conn = connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String buildId = rs.getString("build_id");
                builds.add(load(buildId)); 
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return builds;
    }

    /**
     * Remove a build from persistence.
     * @param buildId of build to remove
     */
    public void remove(String buildId) {
        try (Connection conn = connect()) {
            String deleteBuild = "DELETE FROM builds WHERE build_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteBuild)) {
                stmt.setString(1, buildId);

                int rowsDeleted = stmt.executeUpdate();

                if (rowsDeleted > 0) {
                    System.out.println("Build "+ buildId + " deleted!");
                } else {
                    System.out.println("No build found with ID" + buildId);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Failed to delete build " + buildId); 
            e.printStackTrace();
        }
    }

    /**
     * Remove steps belonging to a build.
     * This method should normally not be needed, remove() handles removing the whole build.
     * @param buildId of build whose steps to remove
     */
    public void removeSteps(String buildId) {
        try (Connection conn = connect()) {
            String deleteBuild = "DELETE FROM steps WHERE build_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteBuild)) {
                stmt.setString(1, buildId);

                int rowsDeleted = stmt.executeUpdate();

                if (rowsDeleted > 0) {
                    System.out.println("Steps for build "+ buildId + " deleted!");
                } else {
                    System.out.println("No steps found for build with ID" + buildId);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Failed to delete steps for build " + buildId); 
            e.printStackTrace();
        }
    }

    private Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(url);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
}
}
