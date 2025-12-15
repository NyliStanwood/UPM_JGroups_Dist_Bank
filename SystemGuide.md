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

## 3. Non-Functional Requirements

### Consistency
- All replicated databases must contain identical data
- Clients receive the same response regardless of which server they access

### Fault Tolerance
- System continues operating despite process failures (fail-silent mode)
- Automatic failure detection
- New replicas created to maintain quorum when failures occur
- No erroneous values returned to clients

### Availability
- High service invocation capacity
- Dynamic process addition to replace failed instances
- Maintains minimum number of operational servers

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

### Supporting Classes

#### Menu & MenuCommands
**Purpose**: User interface
- Provide simple text-based interface
- Handle user input and command processing

**Note**: No modifications needed to these classes

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

## 7. Fault Recovery Process

1. **Failure Detection**: System detects process failure
2. **Quorum Check**: Verifies minimum number of active servers
3. **Replica Creation**: New process spawned if needed
4. **State Transfer**: New replica receives current database state
5. **Service Restoration**: System continues normal operations

## 8. Design Principles

- **Replication**: Multiple copies ensure availability
- **Consistency**: All replicas maintain identical state
- **Transparency**: Clients unaware of distribution and failures
- **Scalability**: Dynamic addition/removal of processes
- **Reliability**: Message-based communication with delivery guarantees

## 9. Development Notes

- This design is a suggested template, not mandatory
- Alternative designs are encouraged and may be superior
- Focus on distributed aspects rather than complex banking logic
- Javadoc documentation provided for all classes
- Reference materials available for JGroups toolkit

## 10. References

- JGroups - A Toolkit for Reliable Messaging
- Manual: Reliable group communication with JGroups
- Tutorial: Reliable group communication with JGroups
- JGroups javadoc