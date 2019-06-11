---
id: codelab2
summary: In this codelab, you'll implement the business logic for an API server, which defines its methods as a gRPC service.
status: [published]
author: chokoswitch
categories: API
tags: api
feedback link: https://github.com/curioswitch/curiostack/issues/new

---

# Implementing an API Server

[Codelab Feedback](https://github.com/curioswitch/curiostack/issues/new)


## Introduction

CurioStack encourages developing systems composed of many different server processes, each with a limited, well defined scope of functionality. For example, there may be a server dedicated to providing authentication for users, another may fetch user data from a database and generate recommendations, and another may take login information from a user, verify using the authentication server, retrieve recommendations from the recommendations server, and render them into an HTML page to show to the user.

In such a system, each server exposes an API - the API defines methods that can be run from a different server. This lab walks you through implementing the business logic for an API in Java using gRPC.

This codelab is intended to be started after completing [Defining a gRPC API](../defining-a-grpc-api).

### **What you'll learn**

* How to implement methods of a gRPC API
* Basics of dependency injection
* Overview of curio-server-framework

### **What you'll need**

* A CurioStack-based code repository clone
* IntelliJ
