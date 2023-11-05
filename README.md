# Logback awslogs JSON encoder

[![build](https://github.com/osiegmar/logback-awslogs-json-encoder/actions/workflows/build.yml/badge.svg)](https://github.com/osiegmar/logback-awslogs-json-encoder/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/de.siegmar/logback-awslogs-json-encoder.svg)](https://central.sonatype.com/artifact/de.siegmar/logback-awslogs-json-encoder)

Logback encoder for producing JSON output that is handled by
AWS [CloudWatch Logs Insights](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/AnalyzingLogData.html). This
library has no external dependencies and thus very light footprint.

Since version 1.3.8, Logback ships with a [JsonEncoder](https://logback.qos.ch/manual/encoders.html#JsonEncoder) itself.
Unfortunately that JsonEncoder produces log output that is poorly suited for use with CloudWatch Logs (Insights).

## Features

- Forwarding of MDC (Mapped Diagnostic Context)
- Forwarding of caller data
- Forwarding of static fields
- Forwarding of exception root cause
- No runtime dependencies beside Logback

## Requirements

- Java 11
- Logback 1.4.11

## Prerequisites

Ensure that the task definition of your ECS task uses the `awslogs` log driver.

A full example (excerpt from a full task definition JSON) could look like this:

```json
{
  "logConfiguration": {
    "logDriver": "awslogs",
    "options": {
      "awslogs-group": "myloggroup",
      "awslogs-region": "eu-central-1"
    }
  }
}
```

## Configuration

A basic configuration with defaults would look like this:

```xml
<configuration>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="de.siegmar.logbackawslogsjsonencoder.AwsJsonLogEncoder"/>
    </appender>

    <root level="DEBUG">
        <appender-ref ref="STDOUT"/>
    </root>

</configuration>
```

An extended version (with all default values set explicitly, custom static fields and mapper) would look like this:

```xml
<configuration>

  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="de.siegmar.logbackawslogsjsonencoder.AwsJsonLogEncoder">
      <includeTimestamp>true</includeTimestamp>
      <includeNanoseconds>false</includeNanoseconds>
      <includeSequenceNumber>false</includeSequenceNumber>
      <includeLevelName>true</includeLevelName>
      <includeThreadName>true</includeThreadName>
      <includeLoggerName>true</includeLoggerName>
      <includeFormattedMessage>true</includeFormattedMessage>
      <includeRawMessage>false</includeRawMessage>
      <includeStacktrace>true</includeStacktrace>
      <includeRootCause>false</includeRootCause>
      <includeMarker>true</includeMarker>
      <includeMdc>true</includeMdc>
      <includeKeyValues>true</includeKeyValues>
      <includeCaller>false</includeCaller>

      <staticField>app_name:backend</staticField>
      <staticField>os_arch:${os.arch}</staticField>

      <customMapper class="your.custom.Mapper"/>
    </encoder>
  </appender>

  <root level="DEBUG">
    <appender-ref ref="STDOUT"/>
  </root>

</configuration>
```

## Example output

Typical output:

```json
{
  "timestamp": 1698595093642,
  "level": "DEBUG",
  "thread": "Test worker",
  "logger": "my.app.MyClass",
  "message": "Message 1"
}
```

Maximal output:

```json
{
  "timestamp": 1698595093642,
  "nanoseconds": 642725000,
  "sequenceNumber": 0,
  "level": "DEBUG",
  "thread": "Test worker",
  "logger": "my.app.MyClass",
  "message": "Message 1",
  "rawMessage": "Message {}",
  "markers": {
    "foo": 1,
    "bar": 1
  },
  "mdc": {
    "foo": "bar",
    "baz": null
  },
  "keyValues": {
    "foo": "bar",
    "bar": null,
    "null": "bar"
  },
  "caller": {
    "file": "NativeMethodAccessorImpl.java",
    "line": -2,
    "class": "jdk.internal.reflect.NativeMethodAccessorImpl",
    "method": "invoke0"
  },
  "stacktrace": "java.lang.IllegalStateException: Error processing data\n\tat my.app.MyClass.foo(MyClass.java:123)",
  "rootCause": {
    "class": "java.lang.IllegalStateException",
    "message": "Error processing data"
  },
  "staticFields": {
    "app_name": "backend",
    "os_arch": "amd64"
  },
  "custom": "A field added by a custom mapper"
}
```
