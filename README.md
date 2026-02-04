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


