---
id: defining-a-grpc-api
summary: In this codelab, you'll write an API from scratch using Protocol Buffers. An API defines methods that a server can expose using gRPC so that other servers can call the methods and execute some functionality.
status: [published]
author: chokoswitch
categories: API
tags: api
feedback link: https://github.com/curioswitch/curiostack/issues/new

---

# Defining a gRPC API

[Codelab Feedback](https://github.com/curioswitch/curiostack/issues/new)


## Introduction



CurioStack encourages developing systems composed of many different server processes, each with a limited, well defined scope of functionality. For example, there may be a server dedicated to providing authentication for users, another may fetch user data from a database and generate recommendations, and another may take login information from a user, verify using the authentication server, retrieve recommendations from the recommendations server, and render them into an HTML page to show to the user.

In such a system, each server exposes an API - the API defines methods that can be run from a different server. Without the ability to define APIs and call methods on them from other servers, there would be no way to compose these many different servers into a single system. This lab walks you through defining a simple API using Protocol Buffers and gRPC.

### **What you'll learn**

* How to generate boilerplate for a new API definition
* How to define messages using protocol buffers
* How to define a service and methods to expose in the API

### **What you'll need**

* A CurioStack-based code repository clone
* A text editor
* A bash shell


## Generating API boilerplate



### Cloning a repository

If you already have a CurioStack-based repository, you are ready to start and skip to the next step. If you don't have one yet, you can clone the upstream repository.

    git clone 'https://github.com/chokoswitch/curiostack'

### Generating the boilerplate

Fire up a terminal in the top level of the repository you are working in and use the following command to generate boilerplate for an API server.

    ./gradlew :generateApiServer

The command will ask several questions to set up the boilerplate code.

1. Name - enter a name for this API. This will generally relate to "what" the API is for. Some common names may be `auth`, `recommendations`, `frontend`, `catfinder`. For this codelab, we will use **`hello-world`**. 
2. Path - the path to output the boilerplate to. Two folders will be created under this path, `api` and `server`. The path is often the same or similar to the name of the API. For this codelab, we will use **`hello/world`**.
3. Java package prefix - this is the Java package under which both the API and the implementation will live. The API will use the Java package specified followed by `.api`. The java package must be unique within the repository and it is usual to use the reverse of your website's domain name followed by the name of the service. For this codelab, we will use **`org.curioswitch.helloworld`**.
4. Proto package - this is the proto package for the API definitions. It is common to make this shorter than the Java package if the API will be used from a language like C++ where long package names are tedious. For this codelab, we will accept the default, which is the same as the Java package.
5. The name of the exposed service - this is the name other servers will use to access the API. It is common convention for this to end with `Service`. For this codelab, we will accept the default, which is upper camel-case of the name followed by `Service`, in this case **`HelloWorldService`**.

With that, you have generated the boilerplate for an API definition! Actually, it also generates the boilerplate for the server that implements the API and can be run right away. For now, we will go through the generated files for the API definition.


## Examining the generated code



The generation script will create two folders, `api` and `server`, as well as add references to them to the top level Gradle configuration so Gradle knows to build them. We will only go through the generated code for the API in this codelab.

### **Gradle configuration**

Take a look at the file `project.settings.gradle.kts` at the top level of the repository. You will see many statements of the form `include(":...")` in this file. `include` tells Gradle that it needs to build the project at the specified path - a path in Gradle is separated by `:` rather than OS-specific path separators like `/` or `\`.

You'll see two new `include` statements for the `api` and `server` directory that were generated.

#### project.settings.gradle.kts

```
// curio-auto-generated DO NOT MANUALLY EDIT
...
include(":hello:world:api")
include(":hello:world:server")
...
// curio-auto-generated DO NOT MANUALLY EDIT
```

CurioStack manages the entries in this file based on the projects in the repository, also making sure to update it anytime a generation command is run. If this file ever gets out of sync with the projects in the repository (usually by manually creating a project), you can run

    ./gradlew :updateProjectSettings

to refresh the configuration. Feel free to run it now, it will have no effect as long as the configuration is in sync, which it generally should be.

### **API folder**

Now go ahead and take a look at the generated API folder, which for us is at `hello/world/api`. You will notice that two files have been created under this folder, `build.gradle.kts` and `src/main/proto/org/curioswitch/helloworld/api/hello-world-service.proto`. 

All projects in Gradle contain a `build.gradle.kts` file to define the project. We've created a separate project for the API here because it will be used in multiple places - naturally the server that implements the API itself but also any clients that will call the API. If the API was in the same project as its implementation server, other servers would not be able to access only the API definition itself for making client calls.

Let's take a look at the `build.gradle.kts` file. Comments explaining each line have been added to this snippet.

#### build.gradle.kts

```
plugins {
    // CurioStack's grpc-api plugin sets this project up to
    // compile proto files into generated code.
    id("org.curioswitch.gradle-grpc-api-plugin")
    // Adding the java plugin makes sure generated Java code is
    // compiled into bytecode.
    java
}

base {
    // This is the name of the archive that will contain the
    // generated code for use in servers. This name is derived
    // from the "name" specified in the generation command. It
    // needs to be unique throughout the codebase.
    archivesBaseName = "hello-world-api"
}
```

Now let's look at the proto file. All proto files go in the `src/main/proto` subdirectory of the Gradle project. Any files not in that directory will not be compiled or usable.

Negative
: Notice that the proto file has been added to a directory corresponding to the proto package. This is required so other proto files can `import` it using the proto package. Don't move the file without also updating its proto package.

The proto file is where all APIs are defined when using gRPC and Protocol Buffers - it is its own syntax for defining structures and methods. The Protocol Buffer compiler will then convert these files into language-specific code (e.g., .java files) which can be used in applications. Before going on, it is a good idea to read the  [Protocol Buffers Overview](https://developers.google.com/protocol-buffers/docs/overview) to understand what and why it is.

#### hello-world-service.proto

```
// There is an old format named proto2 but we never use it
// anymore. This must always be set at the beginning of a
// proto file.
syntax = "proto3";

// This is the proto package. It is used when importing from
// other proto files as well as certain languages like C++.
// If moving the proto file, make sure to update this.
package org.curioswitch.helloworld.api;

// This is the java package. It is used when importing from
// Java code.
option java_package = "org.curioswitch.helloworld.api";
// This allows proto messages to each have their own file.
// It is convenient and recommended to always have this.
option java_multiple_files = true;

// If you use other languages, you may need to add more
// language-specific options.

// This is the API service we are defining int his file. It's
// currently empty and not very useful.
service HelloWorldService {
}
```

Now that we've seen what code gets generated by our boilerplate, let's start adding some actual API definitions.


## Adding an API method



Now we'll add our first API method to our new service. This method will be a very simple method, called `Hello`, which takes an input string such as `Choko` and returns the string `"Hello Choko!"`. The actual string returned depends on the implementation and will not be covered in detail in this codelab.

### **Messages**

In Protocol Buffers, the top-level definition of a set of data is a message. A message has fields, each of which has a type, a name, and a field number. The field number must be unique within the message - it is used to identify the field in the actual transmitted data, whereas the name is only used in application code, not during transmission. The type can itself be a message, allowing arbitrary nesting of structure to create a convenient data type.

Any API method has two messages, a request message and a response message. Since our method will accept a string in the request and return a string in the response, we will define two messages containing these fields.

#### hello-world-service.proto

```
...

// A request for the `Hello` method.
message HelloRequest {
  // The name of the person that wants to say "Hello".
  // If unset, a random name will be used.
  string name = 1;
}

// A response to the `Hello` method.
message HelloResponse {
  // The friendly greeting returned to the user. The actual
  // greeting format is not fixed and may vary randomly or based
  // on user information.
  string greeting = 1;
}

service HelloWorldService {
}
```

Now we have a request and a response that we will be able to use to define our `hello` API method. `HelloRequest` accepts the name of the user and `HelloResponse` returns the greeting. Notice that the messages and fields have detailed comments - API definitions must always have clear comments documenting every message and field. Without documentation, clients will not know how to use the API, leading to at best confusion, and at worst mistakes. Unlike other API definition systems such as a JSON-based REST API, there is no need to have the documentation anywhere else such as a wiki - the proto file is the ground truth of both the definition of the API and its usage documentation.

Positive
: Astute readers may notice that the structure of HelloRequest and HelloResponse are the same. This may lead to believing it's ok to just define one message for both the request and the response. Don't - we **always** define separate messages for requests and responses because

  * The structure of the request and response are intrinsically unrelated. Even if they happen to be the same now, they will likely diverge in the future.
  * Trying to share the same structure for different concepts will likely lead to poor documentation.

For more advanced usage of Protocol Buffers, check out the  [language guide](https://developers.google.com/protocol-buffers/docs/proto3).

### **Methods**

Now that we've defined the request and response messages, we can add our API method to the service. Adding a method is as simple as adding the name, request, and response to the service definition.

#### hello-world-service.proto

```
// `HelloWorldService` provides functionality for greeting users.
service HelloWorldService {
  // Create a greeting message for the requesting user. This method does
  // not perform any sort of authentication and therefore does not have
  // personalized greetings - the `name` of the request will be treated
  // as an opaque identifier of a person.
  rpc Hello (HelloRequest) returns (HelloResponse);
}
```

Again, notice that we have added detailed comments to the service and the method. Without comments, clients would not have any idea of what the `Hello` method could do or its restrictions.

Positive
: The naming and casing conventions are not arbitrary - Protocol Buffers use UpperCamelCase for message, service, and method names and lower_snake_case for field names. Check out the  [style guide](https://developers.google.com/protocol-buffers/docs/style) and always keep it in mind when writing proto files.

### **Building**

Congratulations! You have defined your first API. You can verify the syntax by trying to build the Gradle project.

This will invoke the Protocol Buffers compiler to convert the proto file into Java code as well as compile the Java code for use in applications. A properly configured application would be able to get a greeting with something as simple as

```
var response = helloWorldService.hello(
    HelloRequest.newBuilder().setName("Choko").build());
System.out.println(response.getGreeting());
```

Now you're ready to learn how to implement the API in a server, which we will go over in another codelab.

In the meantime, also take a look at the  [encoding reference](https://developers.google.com/protocol-buffers/docs/encoding) when you have time. While having a strong knowledge of the encoding is not strictly necessary for working with gRPC APIs, it can help get a better understanding of what is going on behind the scenes and help with more advanced use cases.


