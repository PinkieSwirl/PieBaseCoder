[![codecov](https://codecov.io/gh/PinkieSwirl/PieBaseCoder/graph/badge.svg?token=A94L3MPYC9)](https://codecov.io/gh/PinkieSwirl/PieBaseCoder)
![Code QL](https://github.com/PinkieSwirl/PieBaseCoder/actions/workflows/codeql.yml/badge.svg)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FPinkieSwirl%2FPieBaseCoder.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2FPinkieSwirl%2FPieBaseCoder?ref=badge_shield)

# PieBaseCoder

A lightweight Kotlin library for Base128 encoding and decoding operations. For fun and practice.

## Features

- **Base128 Encoding/Decoding**: Convert binary data to Base128 format and vice versa
- **Stream Support**: Includes `Base128InputStreamDecoder` for processing data streams
- **Pure Kotlin**: Written in Kotlin with Java interoperability
- **Well Tested**: Comprehensive test suite with property-based testing using jqwik

## Usage

The library provides simple encode/decode functionality through the `Base128` object:

```kotlin
// Encode data
val encoded = Base128.encode(byteArray)

// Decode data  
val decoded = Base128.decode(encodedData)
```

## Installation
This project uses Gradle with Kotlin. Clone the repository and build:
```bash
./gradlew build
```


## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FPinkieSwirl%2FPieBaseCoder.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2FPinkieSwirl%2FPieBaseCoder?ref=badge_large)