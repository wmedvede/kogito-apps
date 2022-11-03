# Introduction in NEW MACHINE

This document contains the Jobs Service public API specification

# REST API

## Job definition

A Job is represented by the following structure:

| Parameter  | Description | Type
| ------------- | ------------- | ------------- |
| id | The unique identifier of the job in the system. | string
| description  | A human readable string with the job description. | string
| state | The job state. | [Job State](#job-state)
| schedule| This value represents the job triggering periodicity. | [Job Schedule](#job-schedule)
| retry | Retries configuration in case the job execution fails. | [Retry](#retry)
| recipient | The recipient of the job execution. | The recipient is determined by using one of the following fields (exclusively): ["httpRecipient"](#http-recipient), ["sinkRecipient"](#sink-recipient), ["kafkaRecipient"](#kafka-recipient)

The following example shows the job definition that starts a serverless workflow *sales_report* every night at *03:00 AM* to produce the *nightly* sales report for the *Spanish* branch of a fictitious company:

```json
{
  "id": "398c3278-c930-497d-aaeb-0bdcf1a58adf",
  "description": "Execute Spain's sales nightly report",
  "schedule": {
    "cron": {
      "expression" : "0 3 * * *",
      "timeZone": "Europe/Madrid"
    }
  },
  "retry": {
    "maxRetries": "2",
    "delay": "5",
    "delayUnit": "SECONDS",
    "maxDuration": "15",
    "durationUnit": "SECONDS"
  },
  "httpRecipient": {
    "url": "http://acme.server.com/sales_report",
    "method": "POST",
    "headers": {
      "Content-Type": "application/json"
    },
    "body": "{\"reportType\" : \"nightly\", \"country\" : \"Spain\" }"
  }
}
```

### Job State

A job can be in any of the following states: TBD

### Job Schedule

A job schedule can be any of the following types: [Timer](#timer) or [Cron](#cron)

#### Timer

Timer schedules are executed at a given date time and can be repeated a configured number of times.

| Parameter  | Description | Type
| ------------- | ------------- | ------------- |
| startTime | Initial fire time.  | A string with a date-time in the ISO-8601 standard, or with the value "now" to indicate that the job must execute as soon as possible.
| repeatCount | Number of times the job must be repeated, default value is 0. | Integer
| delay | Time delay between the repeated executions, default value is 0. | Long
| delayUnit | The unit for the time delay between executions, default value is "MILLIS". | "MILLIS, "SECONDS", "MINUTES", "HOURS", "DAYS"

The following example shows a schedule configuration of type timer:

```json
{
  "schedule": {
    "timer": {
      "startTime": "2022-11-02T13:20:31.929821+01:00",
      "repeatCount": 3,
      "delay": 5,
      "delayUnit": "HOURS"
    }
  }
}
```

#### Cron

Crone schedules are executed periodically according to a cron expression.

| Parameter  | Description | Type
| ------------- | ------------- | ------------- |
| expression | A cron expression  | 0 0 0 1 * ?
| timeZone | A time zone for the cron programming | "Europe/Madrid"

The following example shows a schedule of type cron:

```json
{
  "schedule": {
    "cron": {
      "expression": "0 0 0 1 * ?",
      "timeZone": "Europe/Madrid"
    }
  }
}
```

### Retry

The retry configuration establishes the number of times a failing Job execution must be retried before it's considered
as FAILED. For multiple time jobs, such as timer jobs with a repeatCount > 0, or cron jobs, a Job execution N, marked as
FAILED, does not prevent the next programmed N+1 execution to run.

| Parameter  | Description | Type
| ------------- | ------------- | ------------- |
| maxRetries | An integer value that indicates the number of retries to execute in case of failures, the default value is 3. | Integer
| delay | Time delay between the retries, the default value is 0. | Integer
| delayUnit | The unit for the time delay between retries, default value is "MILLIS" | "MILLIS", "SECONDS"
| maxDuration | Maximum amount of time to continue retrying if no successful execution was produced, the default value is 180000. | Integer
| durationUnit | The unit for the max duration time, default value is MILLIS. |

The following example shows a retry configuration:

```json
{
  "retry": {
    "maxRetries": "4",
    "delay": "5",
    "delayUnit": "SECONDS",
    "maxDuration": "15",
    "durationUnit": "SECONDS"
  }
}
```

### Http Recipient

The http recipient configuration delivers the job's body by executing a http or https request on the configured url. The delivery is considered successful when the HTTP response is in the range [200 - 299].

| Parameter  | Description | Type
| ------------- | ------------- | ------------- |
| url | Url of the recipient that will receive the request. | An http or https url.
| timeoutMs | A timeout in milliseconds to wait for the response. | Long
| method | Http method to use for the request. | "GET", "HEAD", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
| headers | A json map of values representing the http headers to pass in the request. | { "header1" : "value1", "header2" , "value2" }
| queryParams | A map of values representing the http query parameters to pass in the request. | { "param1" : "value1", "param2" , "value2" }
| body | A String value to pass as the request body when the method is POST, PUT, DELETE or PATCH | Base64 encoded string

The following example shows a http recipient configuration:

```json
{
  "url": "http://myserver.com/add-user",
  "method": "POST",
  "headers": {
    "Content-Type": "application/xml"
  },
  "body": "<user><name>Bob</name><surname>Dylan</surname></user>"
}
```

### Sink Recipient

The sink recipient configuration delivers a cloud event to a knative sink by executing a http POST request on the
configured sinkUrl. The delivery is considered successful when the sink returns an HTTP response in the range [200 - 299].

Parameter  | Description | Type
| ------------- | ------------- | ------------- |
| sinkUrl | The knative sink url | string
| timeoutMs | A timeout in milliseconds to wait for the response. | Integer
| contentType | Configures the content type mode to use for the event delivery, the default value is "binary" | "binary", "structured"
| event | A cloud event compliant Json string. | string

The following example shows a sink recipient configuration:

```json
{
  "sinkUrl": "http://default-broker.default.svc.cluster.local",
  "httpTimeoutMs": 20,
  "contentMode": "binary",
  "event": {
    "id": "A234-1234-1234",
    "source": "my_source",
    "specversion": "1.0",
    "type": "customer.create",
    "subject": "my_subject",
    "kogitoprocid": "add-customer-sw",
    "kogitoprocist": "c54331b8-ab80-4931-ab4a-f789863b2d9c",
    "data": {
      "name": "Bob",
      "surname": "Dylan",
      "address": {
        "street": "Main Street, 56",
        "city": "New York"
      }
    }
  }
}
```

### Kafka Recipient

The kafka recipient configuration delivers a kafka message to a kafka broker.

Parameter  | Description | Type
| ------------- | ------------- | ------------- |
| bootstrap.servers | A comma-separated list of host:port to use for establishing the connection to the Kafka cluster. | string
| topicName | A topic name for the message delivery. | String
| payload | A string with the content of the kafka message to send. | Base64 encoded string
| headers | A json map of values representing the kafka message headers to send. | { "headerKey1" : "value1", "headerKey2" , "value2" }

The following example shows a kafka sink recipient configuration:

```json
{
  "bootstrap.servers": "server1:9092,server1:9092",
  "topicName": "hello-world",
  "payload": "Hello World!",
  "headers": {
    "headerKey1": "value1",
    "headerKey2": "value2"
  }
}
```







