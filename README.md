# Logback awslogs JSON encoder

[![build](https://github.com/osiegmar/logback-awslogs-json-encoder/workflows/build/badge.svg?branch=master)](https://github.com/osiegmar/logback-awslogs-json-encoder/actions?query=branch%3Amaster)
[![javadoc](https://javadoc.io/badge2/de.siegmar/logback-awslogs-json-encoder/javadoc.svg)](https://javadoc.io/doc/de.siegmar/logback-awslogs-json-encoder)
[![Maven Central](https://img.shields.io/maven-central/v/de.siegmar/logback-awslogs-json-encoder.svg)](https://search.maven.org/search?q=g:%22de.siegmar%22%20AND%20a:%22logback-awslogs-json-encoder%22)

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
- Logback 1.2.3


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
            <mdcPrefix></mdcPrefix>
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
* **mdcPrefix**: the key under which MDC values are stored. If empty or `null` MDC values will be written in the main object and can potentially overwrite other fields like level or message. Default: `"mdc"`.
* **includeCallerData**: If true, caller data (source file-, method-, class name and line) will be
  included, too. Default: false.
* **includeRootCauseData**: If true, root cause exception of the exception passed with the log
   message will be exposed in the root_cause_class_name and root_cause_message fields.
   Default: false.
* **messageLayout**: Message format for messages without an exception. Default: `"%m%nopex"`.
* **fullMessageLayout**: Message format for messages with an exception. Default: `"%m%n"`.
* **staticFields**: Additional, static fields to include. Defaults: none.


## Contribution

- Fork
- Code
- Add test(s)
- Commit
- Send me a pull request


## Copyright

Copyright (C) 2018 Oliver Siegmar

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
