# protobuf-jackson

protobuf-jackson is a library for efficient marshalling of Protocol Buffer messages to and from
JSON. It is based on the streaming API of the Jackson JSON handling library and uses Byte Buddy to
generate efficient bytecode at runtime for marshalling specific message types.

## Why another Protocol Buffer <-> JSON marshaller

The official Protocol Buffer Java library comes with a util library for marshalling to JSON, using
a class called JsonFormat. Unfortunately, JsonFormat is very slow, and its core design of using
manual JSON printers / GSON parsers and introspection during serialization time limit how fast it
can get.

JSON marshalling is important - it can allow existing REST services to be migrated to Protocol
Buffers and makes it easier to evangelize their use in organizations currently tied heavily to JSON.
However, slow marshalling prevents these use cases from being practical, so this library aims to
allow JSON marshalling to be fast enough for production. It will never be as good as binary
serialization, but should be about as fast as standard REST API libraries.

If the protoc compiler supports generating source code for JSON marshalling, this library will
likely be deprecated.

## Usage

Include protobuf-jackson as a dependency. Gradle users can add

```groovy
'org.curioswitch.curiostack:protobuf-jackson:0.1.0'
```

to their dependencies.

The API is exposed in ```MessageMarshaller```. Use its builder to configure options and register
specific Protocol Buffer message types you will marshall. Any other types will not be marshallable
by the built ```MessageMarshaller```.

```java
class MessageApi {

  private static final MessageMarshaller marshaller = MessageMarshaller.builder()
      .register(ApiRequest.getDefaultInstance())
      .register(ApiResponse.getDefaultInstance())
      .build();

  String coolApi(String jsonRequest) {
    ApiRequest.Builder request = ApiRequest.newBuilder();
    marshaller.mergeValue(jsonRequest, request);

    ApiResponse response = processRequest(request);

    return marshaller.writeValueAsString(response);
  }
}
```

## Design

protobuf-jackson introspects registered message types during bytecode generation time and generates
code specific to the type. Unlike JsonFormat, this means that during actually marshalling, there is
no introspection of the message type at all - it's already been done during registration. In
addition, messages are accessed through their standard Java generated API, no reflection is needed.
No reflection allows the JIT to optimize generated machine code as much as possible. By generating
bytecode, we approach the same speed that could be achieved by generating the source code itself.

All of the same tests as ```JsonFormat``` (besides the differences listed below) pass, so
protobuf-jackson should be mostly compatible with upstream and ready for production. After 
integrating into a larger project and verifying the public API is easy to use, it will probably be
promoted to version ```1.0.0```. Most work after that will be in smaller optimizations.

## Differences with upstream

Differences with JsonFormat are

- No support for ```DynamicMessage```. The library is designed to interact with generated code for
optimal performance. If you don't know what ```DynamicMessage``` is, you are not using it. If you
do, stick with ```JsonFormat```.

- Cannot put unknown enum values in maps. Will be fixed with https://github.com/google/protobuf/issues/2936

- Does not support parsing Any messages where ```@type``` is not the first field.

- Correctly handles invalid array syntax. ```JsonFormat``` does not due to its dependency on GSON.
See [JsonFormatTest](https://github.com/google/protobuf/blob/master/java/util/src/test/java/com/google/protobuf/util/JsonFormatTest.java#L1124).

- Correctly rejects invalid JSON which specifies the same field twice. ```JsonFormat``` does not due
to its dependency on GSON. See [JsonFormatTest](https://github.com/google/protobuf/blob/master/java/util/src/test/java/com/google/protobuf/util/JsonFormatTest.java#L458)

- Fields already present in the passed in ```Message.Builder``` will be overwritten, rather than
causing the parse to fail, as is usual with merge semantics.

## Benchmarks

First, a synthetic benchmark that uses the upstream ```TestAllTypes``` proto which contains all
types of fields.

```
Benchmark                                  Mode  Cnt        Score       Error  Units
JsonParserBenchmark.codegenJson           thrpt   50   114260.817 ±  1162.079  ops/s
JsonParserBenchmark.fromBinary            thrpt   50   448586.946 ±  7946.499  ops/s
JsonParserBenchmark.upstreamJson          thrpt   50    43392.884 ±   715.184  ops/s
JsonSerializeBenchmark.codegenJson        thrpt   50   204768.672 ±  2459.787  ops/s
JsonSerializeBenchmark.toBytes            thrpt   50  1071878.319 ± 12628.059  ops/s
JsonSerializeBenchmark.upstreamJson       thrpt   50    49941.606 ±   707.045  ops/s
```

protobuf-jackson generally shows a 4x speed up in serialization and 3x speed up in parsing vs
upstream JSON parsing. It is about 4-5x slower than protobuf binary marshalling.

For a more real-world example, the JSON response of the [GitHub search API](https://developer.github.com/v3/search/#example)
was used. A proto file corresponding to the format was made to model what an actual data exchange
may look like. This is just one example, and other examples may have different results, but
hopefully the trends are similar

```
Benchmark                                  Mode  Cnt        Score       Error  Units
GithubApiBenchmark.marshallerParseString  thrpt   50     1696.653 ±   170.891  ops/s
GithubApiBenchmark.marshallerWriteString  thrpt   50     3042.433 ±   172.438  ops/s
GithubApiBenchmark.protobufParseBytes     thrpt   50     4599.390 ±    56.570  ops/s
GithubApiBenchmark.protobufToBytes        thrpt   50    21315.212 ±   210.659  ops/s
GithubApiBenchmark.upstreamParseString    thrpt   50      472.902 ±     6.455  ops/s
GithubApiBenchmark.upstreamWriteString    thrpt   50      717.227 ±     8.834  ops/s
```

For both parsing and serializing, protobuf-jackson is about 4x faster.

One other way to look at these results is that JSON marshalling is now less than an order of
magnitude different than protobuf binary marshalling, which was not the case before. While binary
marshalling will always be preferred, for use cases that demand it, JSON should also be acceptable
in production.
