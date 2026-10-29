# S3 / LocalStack development and integration tests

[Amazon Simple Storage Service (S3)](https://aws.amazon.com/s3/) is AWS's object storage service.

## Storage selection

The default is `cache.storage.type=memory`. Select `s3` to use `S3CacheStorage`; create the bucket before using the service. The application does not create buckets.

## S3 configuration

| Property | Environment variable | Default / requirement |
| --- | --- | --- |
| `cache.storage.type` | `CACHE_STORAGE_TYPE` | `memory`; set to `s3` for S3 storage |
| `cache.storage.s3.backend` | `CACHE_STORAGE_S3_BACKEND` | `aws`; alternative: `localstack` |
| `cache.storage.s3.bucket` | `CACHE_STORAGE_S3_BUCKET` | Required for S3 storage |
| `cache.storage.s3.region` | `CACHE_STORAGE_S3_REGION` | Required for S3 storage |
| `cache.storage.s3.localstack-endpoint` | `CACHE_STORAGE_S3_LOCALSTACK_ENDPOINT` | Required for the `localstack` backend |

With `backend=aws`, `S3Configuration` uses the AWS SDK default credential provider chain, such as a workload IAM role. With `backend=localstack`, it overrides the endpoint and supplies fixed `test`/`test` credentials.

### LocalStack application run

Start the development services:

```bash
docker compose up -d --build dev localstack
```

Once LocalStack is ready, create the bucket (once per LocalStack instance):

```bash
docker compose exec localstack awslocal s3 mb s3://maven-build-cache --region eu-central-1
```

Then run the application with configuration passed into the Maven process:

```bash
docker compose exec \
  -e CACHE_STORAGE_TYPE=s3 \
  -e CACHE_STORAGE_S3_BACKEND=localstack \
  -e CACHE_STORAGE_S3_BUCKET=maven-build-cache \
  -e CACHE_STORAGE_S3_REGION=eu-central-1 \
  -e CACHE_STORAGE_S3_LOCALSTACK_ENDPOINT=http://localstack:4566 \
  dev mvn spring-boot:run
```

Inside `dev`, use the Compose service address `http://localstack:4566`. For an application running on the host, use `http://localhost:4566`. Exporting variables in a host shell does not pass them to `docker compose exec`; use `-e` as above.

### Amazon S3

Set `CACHE_STORAGE_TYPE=s3`, `CACHE_STORAGE_S3_BACKEND=aws`, bucket and region in the application process environment. Provide AWS credentials through the SDK credential chain in that same runtime; host credentials are not mounted into `dev` by this Compose file.

For a general-purpose bucket, grant `s3:GetObject` and `s3:PutObject` on cache objects and `s3:ListBucket` on the bucket. `HeadBucket` health checks need the bucket permission; it also allows S3 to report missing objects as 404 rather than 403. The adapter maps S3 404 responses to missing entries and propagates other S3 errors.

See AWS references for [credentials](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html), [HeadBucket](https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadBucket.html) and [HeadObject](https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadObject.html).

## Automated LocalStack integration tests

Both S3 integration test classes use `localstack/localstack:4.14.0`. Each starts a separate container, supplies its dynamic endpoint to Spring, creates a test bucket and stops the container after the tests. No manually started LocalStack service or application environment variables are needed.

For commands, coverage and the Docker requirement, see [development and tests](readme-dev.md#3-run-tests).
