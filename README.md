# CI-server
Continuous Integration server for automatically building and testing GitHub projects on push events.

## Overview

This CI server receives GitHub webhook notifications when code is pushed to a repository, then automatically compiles and tests the code. The server provides a REST API for receiving webhooks and exposes build results.

## Table of Contents
- [Dependencies](#dependencies)
- [Building the Project](#building-the-project)
- [Running the Server](#running-the-server)
- [API Endpoints](#api-endpoints)
- [GitHub Webhook Configuration](#github-webhook-configuration)
- [Local Development with ngrok](#local-development-with-ngrok)
- [Testing](#testing)
- [Implementation](#implementation)
- [Build list URL](#build-list-url)
- [Assessment test](#assessment-test)
- [Contributions](#contributions)
- [License](#license)

## Dependencies

- **Java**: JDK 17 or higher
- **Maven**: 3.6 or higher
- **Jetty**: 12.x (embedded HTTP server)
- **org.json**: JSON parsing library

All dependencies are managed through Maven and will be automatically downloaded.

## Building the Project

Compile the project using Maven:

```bash
mvn clean compile
```

Run all tests:

```bash
mvn test
```

## Running the Server

### Default Port (8080)

Start the server with default configuration:

```bash
mvn exec:java -Dexec.mainClass="ci.server.WebhookServer"
```

The server will start on port 8080 and display:

```
CI server started on port 8080
Health check: http://localhost:8080/
Webhook endpoint: http://localhost:8080/webhook
```

## API Endpoints

### Health Check

**Endpoint**: `GET /`

Returns server status. Used to verify the server is running.

**Response**:
```
200 OK
Content-Type: text/plain

CI server running
```

### Webhook Endpoint

**Endpoint**: `POST /webhook`

Receives GitHub push event webhooks. Validates the event type and payload, then triggers the CI pipeline.

**Required Headers**:
- `X-GitHub-Event: push` - Only push events are accepted
- `Content-Type: application/json`

**Request Body**: GitHub push event payload (JSON)

Required fields:
- `ref` - Branch reference (e.g., `refs/heads/main`)
- `after` - Commit SHA (40-character hexadecimal)
- `repository.name` - Repository name
- `repository.full_name` - Full repository name (owner/repo)
- `repository.clone_url` - HTTPS clone URL

**Success Response**:
```json
{
  "status": "received",
  "commit": "a1b2c3d4e5f6789012345678901234567890abcd"
}
```

## GitHub Webhook Configuration

Follow these steps to configure GitHub to send webhooks to your CI server:

### Step 1: Expose Your Server

When running locally, you need to make your server accessible from the internet. See [Local Development with ngrok](#local-development-with-ngrok) below.

### Step 2: Configure Webhook in GitHub

1. Navigate to your GitHub repository
2. Go to **Settings** → **Webhooks** → **Add webhook**
3. Configure the webhook:
   - **Payload URL**: `https://abc123.ngrok.dev/webhook`
   - **Content type**: `application/json`
   - **Secret**: (optional, not currently validated)
   - **Which events**: "Just the push event"
   - **Active**: ✓ Checked
4. Click **Add webhook**

### Step 3: Verify Configuration

1. Make a commit to the repository
2. Check the webhook delivery in GitHub:
   - Go to **Settings** → **Webhooks** → Click on your webhook
   - View **Recent Deliveries** to see request/response details
3. Check your server logs for the received webhook

## Local Development with ngrok

[ngrok](https://ngrok.com/) creates a secure tunnel from the internet to your local machine, allowing GitHub to send webhooks to your local server.

### Installation

Download and install ngrok from [https://ngrok.com/download](https://ngrok.com/download)

For macOS using Homebrew:
```bash
brew install ngrok
```

For Windows: <br>
Download and install ngrok from the Microsoft Store.

### Usage

1. Start your CI server:
```bash
mvn exec:java -Dexec.mainClass="ci.server.WebhookServer"
```

2. In a separate terminal, start ngrok tunnel:
```bash
ngrok http 8080
```

3. ngrok will display a forwarding URL:
```
Forwarding  https://abc123.ngrok.dev -> http://localhost:8080
```

4. Use the HTTPS URL (`https://abc123.ngrok.io/webhook`) as your GitHub webhook payload URL

5. Monitor webhook requests in the ngrok web interface: `http://127.0.0.1:4040`

## Testing

### Run All Tests

```bash
mvn test
```

### Test Files

Test webhook payloads are located in `src/test/resources/webhook-payloads/`:
- `valid-push.json` - Valid GitHub push event
- `malformed.json` - Malformed JSON for error testing
- `missing-ref.json` - Missing required field for validation testing


## Implementation

### Test compilation and execution 

## CI Pipeline Architecture

The CI pipeline executes the following steps when triggered by a webhook:

### 1. Repository Checkout
- Creates a temporary workspace directory
- Clones the repository using the provided HTTPS URL
- Checks out the specific branch and commit SHA from the webhook payload

### 2. Compilation
- Executes `mvn -B compile` in the workspace
- Captures stdout/stderr logs
- Pipeline stops if compilation fails

### 3. Testing
- Executes `mvn -B test` in the workspace
- Captures test output and results
- Pipeline stops if tests fail

### 4. Result Handling
- Build results are stored with status (SUCCESS, FAILURE, or ERROR)
- Notifications are sent (e.g., GitHub commit status updates)
- All logs and step results are persisted

**Status Values**:
- `SUCCESS`: All steps completed successfully
- `FAILURE`: Compilation or tests failed
- `ERROR`: Pipeline encountered an unexpected error (e.g., git clone failed)

Each build is assigned a unique ID and timestamp for storing builds.

The test execution is also unit tested by making sure both the compile method and test method return the correct result and logs depending on the process exit code.

### Notification

We notify CI results by posting **GitHub commit statuses** via the REST API. `NotifierFactory` uses `GitHubStatusNotifier` when `GITHUB_TOKEN` is set, otherwise it falls back to `NoOpNotifier`. Status mapping: `SUCCESS→success`, `FAILURE→failure`, `ERROR→error`. Notification errors are logged and do **not** crash the pipeline.

Unit tests check: factory selection (token vs no token), correct status mapping + JSON payload, correct REST endpoint and headers, optional `target_url` handling, description truncation, and that network failures don’t throw.

## Build list URL
The build list of our server is available here:
https://subintegumentary-tobie-nonefficaciously.ngrok-free.dev/builds

To see logs for a specific build, add the build id from the list into the url: /builds/{build-id}

## Assessment test

Please edit the Assessment.md file in the assessment branch, and push the changes directly to the branch. This will trigger the CI pipeline and you will see the results of the build & tests directly on Github.

## Contributions
 - **Ammar Alzeno:** HTTP server for CI, webhook endpoints, unit tests for webhook handling, README, documentation
 - **Jens Cancio:** CI result notification logic, corresponding unit tests, MIT License
 - **Amanda Henrion Eskeus:** build result persistence logic, corresponding tests, README
 - **Anna Remmare:** CI pipeline flow, corresponding unit tests, Essence
 - **Nora Wennerström:** build execution logic, corresponding unit tests

## License

This project is licensed under the MIT License.