# Distributed Bank System - High Level Guide

## 1. System Overview

This is a distributed application that manages bank client information using replicated processes. The system is built using JGroups for reliable group communication and ensures consistency, availability, and fault tolerance.

### Key Characteristics
- **Replicated Database**: Multiple processes maintain synchronized copies of client data
- **Message-Based Communication**: All inter-process communication via message passing
- **Client Information**: Account number, name, and balance for each client
- **Distributed Architecture**: Can run on single or multiple computers

## 2. System Services

The system provides four core banking operations:

- **Put**: Create a new client in the bank
- **Get**: Retrieve client information
- **Update**: Modify client balance
- **Remove**: Delete a client from the bank

## 4. Class Architecture

### Core Classes

#### MainBank
**Purpose**: Application entry point
- Initializes the system
- Creates required class instances
- Handles service invocations
- Coordinates execution flow

**Note**: No modifications needed to this class

#### NodeJG
**Purpose**: Cluster management and message processing
- Joins the process to the JGroups cluster
- Creates necessary objects
- Broadcasts service invocations from the menu
- Processes received messages
- **Implements**: `org.jgroups.Receiver` interface

#### ServicesBank
**Purpose**: Service operation handling
- Provides methods for each banking operation
- Called when users request services (Put, Get, Update, Remove)
- Contains business logic for client management

#### OperationsBank
**Purpose**: Operation encapsulation for transmission
- Wraps operation information for network transmission
- **Implements**: `Serializable` interface for object serialization
- Enables operations to be sent as messages between processes

#### SendMessages
**Purpose**: Message broadcasting
- Distributes operations to all processes in the group
- Handles reliable multicast communication

#### ProcessMsgBank
**Purpose**: Incoming message processing
- Invoked by `receive` method when messages arrive
- Processes operation requests from other nodes
- Executes operations on local database

#### ClientDB
**Purpose**: Local database management
- Represents the bank's database for a single process
- Stores and manages client records
- Maintains data consistency

#### Client
**Purpose**: Client data representation
- Encapsulates personal client information
- Properties: account number, name, balance


## 5. System Workflow

### Service Invocation Flow
1. User requests a service through the Menu
2. MainBank receives the request
3. ServicesBank prepares the operation
4. OperationsBank encapsulates operation data
5. SendMessages broadcasts to all cluster nodes
6. NodeJG distributes the message via JGroups

### Message Processing Flow
1. NodeJG receives message through JGroups
2. Message passed to ProcessMsgBank
3. Operation extracted and validated
4. ServicesBank executes on ClientDB
5. Client records updated/queried
6. Response sent if required

## 6. Technical Foundation

### JGroups Framework
- **Toolkit**: Reliable messaging framework
- **Features**: Group communication, failure detection, message ordering
- **Protocol Stack**: Ensures reliable delivery and consistency

### Communication Model
- **Multicast**: Operations broadcast to all replicas
- **Synchronization**: Ensures all databases remain consistent
- **State Transfer**: New nodes receive current system state
