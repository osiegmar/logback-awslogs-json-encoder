# Logback awslogs JSON encoder

> :point_up: Logback contains a [JsonEncoder](https://logback.qos.ch/manual/encoders.html#JsonEncoder) since 1.3.8.
> Therefore I do no longer consider this library as relevant.

[![build](https://github.com/osiegmar/logback-awslogs-json-encoder/actions/workflows/build.yml/badge.svg)](https://github.com/osiegmar/logback-awslogs-json-encoder/actions/workflows/build.yml)
[![javadoc](https://javadoc.io/badge2/de.siegmar/logback-awslogs-json-encoder/javadoc.svg)](https://javadoc.io/doc/de.siegmar/logback-awslogs-json-encoder)
[![Maven Central](https://img.shields.io/maven-central/v/de.siegmar/logback-awslogs-json-encoder.svg)](https://search.maven.org/artifact/de.siegmar/logback-awslogs-json-encoder)

Logback encoder for producing JSON output that is handled by
AWS [CloudWatch Logs Insights](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/AnalyzingLogData.html). This
library has no external dependencies and thus very light footprint.

## Features

- Forwarding of MDC (Mapped Diagnostic Context)
- Forwarding of caller data
- Forwarding of static fields
- Forwarding of exception root cause
- No runtime dependencies beside Logback

## Requirements

- Java 8
- Logback 1.2.10

## Prerequisites

Ensure that the task definition of your ECS task uses the `awslogs` log driver with 
the option `awslogs-datetime-format` set to `%Y-%m-%dT%H:%M:%S.%f%z`.

A full example (excerpt from a full task definition JSON) could look like this:

```json
{
  "logConfiguration": {
    "logDriver": "awslogs",
    "options": {
      "awslogs-group": "myloggroup",
      "awslogs-region": "eu-central-1",
      "awslogs-datetime-format": "%Y-%m-%dT%H:%M:%S.%f%z"
    }
  }
}
```

## Example

Simple configuration:

```xml
<configuration>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="de.siegmar.logbackawslogsjsonencoder.AwsJsonLogEncoder"/>
    </appender>

    <root level="DEBUG">
        <appender-ref ref="STDOUT" />
    </root>

</configuration>
```

Enhanced configuration:

```xml
<configuration>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="de.siegmar.logbackawslogsjsonencoder.AwsJsonLogEncoder">
            <includeRawMessage>false</includeRawMessage>
            <includeMarker>true</includeMarker>
            <includeMdcData>true</includeMdcData>
            <includeCallerData>false</includeCallerData>
            <includeRootCauseData>false</includeRootCauseData>
            <messageLayout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%m%nopex</pattern>
            </messageLayout>
            <fullMessageLayout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%m%n</pattern>
            </fullMessageLayout>
            <staticField>app_name:backend</staticField>
            <staticField>os_arch:${os.arch}</staticField>
            <staticField>os_name:${os.name}</staticField>
            <staticField>os_version:${os.version}</staticField>
        </encoder>
    </appender>

    <root level="DEBUG">
        <appender-ref ref="STDOUT" />
    </root>

</configuration>
```

## Configuration

`de.siegmar.logbackawslogsjsonencoder.AwsJsonLogEncoder`

* **includeRawMessage**: If true, the raw message (with argument placeholders) will be included, too.
  Default: false.
* **includeMarker**: If true, logback markers will be included, too. Default: true.
* **includeMdcData**: If true, MDC keys/values will be included, too. Default: true.
* **includeCallerData**: If true, caller data (source file-, method-, class name and line) will be
  included, too. Default: false.
* **includeRootCauseData**: If true, root cause exception of the exception passed with the log
   message will be exposed in the root_cause_class_name and root_cause_message fields.
   Default: false.
* **messageLayout**: Message format for messages without an exception. Default: `"%m%nopex"`.
* **fullMessageLayout**: Message format for messages with an exception. Default: `"%m%n"`.
* **staticFields**: Additional, static fields to include. Defaults: none.
