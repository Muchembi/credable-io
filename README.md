## Setup & Running

### Prerequisites

* Java JDK 17 or later (only needed if building outside Docker)
* Docker & Docker Compose
* `curl` or Postman (for testing APIs)

### Configuration

1.  **Scoring Engine `client-token`:**
    * You **MUST** obtain a `client-token` from the Scoring Engine's `createClient` API.
    * Call `POST https://scoringtest.credable.io/api/v1/client/createClient` with a JSON body like:
        ```json
        {
          "url": "[YOUR_LMS_TRANSACTION_ENDPOINT_URL]",
          "name": "YourLmsServiceName",
          "username": "[USERNAME_LMS_EXPECTS]",
          "password": "[PASSWORD_LMS_EXPECTS]"
        }
        ```
    * Replace `[YOUR_LMS_TRANSACTION_ENDPOINT_URL]` with the **publicly accessible URL** where your LMS service will expose the transaction endpoint (e.g., `http://<your-public-ip-or-domain>:8080/api/v1/lms/transactions/{customerNumber}`). **This cannot be `localhost` or `127.0.0.1` unless the scoring engine is running on the exact same machine.** If testing locally, consider using a tool like `ngrok` to get a public URL (`ngrok http 8080`).
    * Replace `[USERNAME_LMS_EXPECTS]` and `[PASSWORD_LMS_EXPECTS]` with the credentials defined in `lms.security.username` / `lms.security.password` in `application.yaml` (or overridden in `docker-compose.yml`). Default: `scoring_user` / `scoring_pwd_123!`.
    * Take the `token` value from the response.
2.  **`docker-compose.yml`:**
    * Open the `docker-compose.yml` file.
    * Find the `environment` section under `lms-app`.
    * Replace the placeholder `YOUR_GENERATED_UNIQUE_UUID_FROM_CREATE_CLIENT_API` with the actual `client-token` you obtained in the previous step.
        ```yaml
        environment:
          - SCORING_API_CLIENT_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx # <-- PASTE YOUR TOKEN HERE
          # - Optional overrides for LMS_SECURITY_USERNAME / LMS_SECURITY_PASSWORD
        ```
    * Save the `docker-compose.yml` file.

### Running with Docker Compose

1.  **Navigate to the project root directory** (where `docker-compose.yml` is located).
2.  **Build and start the service:**
    ```bash
    docker-compose up --build -d
    ```
    * `--build`: Forces Docker to build the image using the `Dockerfile`.
    * `-d`: Runs the container in detached mode (in the background).
3.  **Check logs (optional):**
    ```bash
    docker-compose logs -f lms-app
    ```
4.  The service should now be running and accessible on `http://localhost:8080`.

### Stopping the service:

```bash
docker-compose down