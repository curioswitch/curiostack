---
id: codelab1
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
