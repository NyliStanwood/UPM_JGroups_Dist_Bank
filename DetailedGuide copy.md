# Distributed Bank System - Detailed Implementation Guide

## 1. System Architecture Deep Dive

### 1.1 Architectural Pattern
The system follows a **Replicated State Machine** pattern where:
- Each node maintains a complete copy of the database
- All nodes process the same operations in the same order
- JGroups ensures reliable, ordered message delivery
- Total Order Multicast guarantees consistency

### 1.2 Component Interaction Diagram
```
User Input → MainBank → ServicesBank → OperationsBank (serialized)
                              ↓
                        SendMessages → JChannel (JGroups)
                              ↓
                    [Network - Multicast to all nodes]
                              ↓
                        JChannel → NodeJG.receive()
                              ↓
                        ProcessMsgBank → ClientDB
                              ↓
                        Local State Update
```

### 1.3 Data Flow Layers
1. **Presentation Layer**: Menu, MenuCommands (user interface)
2. **Application Layer**: MainBank, ServicesBank (business logic)
3. **Distribution Layer**: NodeJG, SendMessages (cluster management)
4. **Communication Layer**: JChannel, OperationsBank (message transport)
5. **Data Layer**: ClientDB, Client (persistence)

---

## 2. Object Communication Flow

### 2.1 OperationsBank Object Structure
```java
public class OperationsBank implements Serializable {
    private String operation;      // "PUT", "GET", "UPDATE", "REMOVE"
    private int accountNumber;     // Client identifier
    private String name;           // Client name (PUT only)
    private double balance;        // Client balance (PUT, UPDATE)
}
```

**Serialization Journey**:
1. Created in ServicesBank with operation parameters
2. Passed to SendMessages
3. Serialized by JGroups (via `Message.setObject()`)
4. Transmitted over network as byte stream
5. Deserialized by receiving JChannel
6. Extracted in NodeJG.receive() via `msg.getObject()`
7. Processed by ProcessMsgBank

### 2.2 Client Object Structure
```java
public class Client implements Serializable {
    private int accountNumber;
    private String name;
    private double balance;
}
```

**Usage Contexts**:
- **In Memory**: Stored in ClientDB's internal data structure (HashMap/List)
- **In Transit**: Part of state transfer during `getState()/setState()`
- **Return Value**: Returned by GET operations to verify client data

### 2.3 ClientDB Object Structure
```java
public class ClientDB implements Serializable {
    private Map<Integer, Client> clients; // or List<Client>
    
    // Core methods
    public void addClient(Client c)
    public Client getClient(int accountNumber)
    public void updateClient(int accountNumber, double balance)
    public void removeClient(int accountNumber)
}
```

**Serialization Critical**:
- Entire database serialized during state transfer
- Must mark all fields and nested objects as Serializable
- Transient fields will NOT be transferred (avoid using them)

---

## 3. Message Passing Mechanics

### 3.1 Message Creation and Sending

**In SendMessages class**:
```java
public void sendMsg(OperationsBank op) {
    try {
        Message msg = new Message(null, op); // null = broadcast to all
        channel.send(msg);
    } catch (Exception e) {
        // Handle send failure
    }
}
```

**Message Parameters**:
- **Destination**: `null` means multicast to ALL nodes (including sender)
- **Source**: Automatically set by JChannel (sender's Address)
- **Object**: OperationsBank instance (automatically serialized)

### 3.2 Message Reception Flow

**In NodeJG.receive()**:
```java
public void receive(Message msg) {
    // 1. Extract source address (optional, for logging)
    Address sender = msg.getSrc();
    
    // 2. Deserialize the operation
    OperationsBank op = (OperationsBank) msg.getObject();
    
    // 3. Process on local database (CRITICAL: synchronized)
    synchronized (stateDB) {
        Client result = processMsg.processOpn(op);
    }
}
```

**Key Points**:
- Message delivered to ALL nodes, including the originating node
- Reception is asynchronous (different thread)
- Order guaranteed by JGroups (Total Order protocol)

### 3.3 Message Ordering Guarantees

JGroups provides **Total Order Multicast**:
- All nodes receive messages in the same order
- If Node A sends M1 before M2, all nodes see M1 before M2
- If Node A sends M1 and Node B sends M2 concurrently, all nodes see same order

**Example Scenario**:
```
Node1: PUT(account=1, balance=100)
Node2: UPDATE(account=1, balance=+50)

Guaranteed: All nodes will execute PUT before UPDATE
Result: All databases show balance=150 (not 50 or 100)
```

---

## 4. State Synchronization

### 4.1 State Transfer Trigger Events

State transfer occurs when:
1. **New node joins**: Calls `channel.getState(null, timeout)`
2. **Node requests sync**: Explicit state request
3. **Recovery from partition**: After network split heals

### 4.2 getState() - Sending State

**In NodeJG** (existing node with data):
```java
public void getState(OutputStream output) throws Exception {
    synchronized(stateDB) {
        // Serialize entire database to output stream
        Util.objectToStream(stateDB, new DataOutputStream(output));
    }
}
```

**Critical Considerations**:
- **Lock the database**: Prevent modifications during serialization
- **Complete snapshot**: Entire ClientDB object serialized
- **No partial states**: All-or-nothing transfer
- **Blocking operation**: Node locks database temporarily

### 4.3 setState() - Receiving State

**In NodeJG** (new/joining node):
```java
public void setState(InputStream input) throws Exception {
    // Deserialize the complete database
    ClientDB receivedDB = Util.objectFromStream(new DataInputStream(input));
    
    synchronized(stateDB) {
        // Replace local state with received state
        stateDB.createBank(receivedDB);
        // Or: stateDB.clear(); stateDB.addAll(receivedDB.getClients());
    }
}
```

**Important Notes**:
- **Overwrite local state**: New node's empty DB replaced with cluster state
- **Atomic operation**: Either complete success or failure
- **Timeout handling**: If state transfer fails, node should retry or exit

### 4.4 State Consistency Timeline

```
Time    Node1 (existing)           Node2 (new)
----    ----------------           -----------
T0      DB: [C1, C2, C3]           Not started
T1      Operating normally         Starts, connects to cluster
T2      Receives getState()        Calls channel.getState()
T3      Locks DB, serializes       Waiting...
T4      Sends state to Node2       Receives state stream
T5      Unlocks DB                 Deserializes into local DB
T6      Continues operations       setState() complete
T7      Both nodes have [C1,C2,C3] and process new operations
```

**CRITICAL**: Between T6-T7, operations sent during state transfer are:
- **Buffered by JGroups** and delivered AFTER setState() completes
- **Processed in order** guaranteeing consistency

---

## 5. Thread Safety & Concurrency

### 5.1 Concurrency Sources

**Multiple threads in the system**:
1. **Main thread**: User input, menu processing
2. **JGroups receiver thread**: Calls `receive()` for incoming messages
3. **State transfer threads**: Handle `getState()/setState()`
4. **JGroups internal threads**: Heartbeats, failure detection

### 5.2 Critical Sections

**ClientDB Access**:
```java
// ALWAYS synchronize on stateDB
synchronized (stateDB) {
    stateDB.addClient(client);      // Modification
    Client c = stateDB.getClient(1); // Even reads!
}
```

**Why synchronize reads?**
- Prevent reading during state transfer
- Ensure visibility of latest writes
- Avoid ConcurrentModificationException during iterations

### 5.3 Deadlock Prevention

**CORRECT** (consistent lock ordering):
```java
// ALWAYS acquire locks in the same order
synchronized(stateDB) {
    // All database operations
}
// Use separate locks for independent resources
```

### 5.4 Lock Granularity Trade-offs

**Coarse-grained locking** (current approach):
```java
synchronized(stateDB) {
    // Entire operation under one lock
}
```
- **Pros**: Simple, guaranteed consistency
- **Cons**: Lower concurrency, potential bottleneck

**Fine-grained locking** (advanced):
```java
// Lock individual client records
synchronized(getClientLock(accountNumber)) {
    // Operation on specific client
}
```
- **Pros**: Better concurrency
- **Cons**: Complex, risk of deadlocks

**Recommendation**: Start with coarse-grained, optimize only if performance issues


## 7. Implementation Details by Class

### 7.1 NodeJG Implementation Details

**Constructor Responsibilities**:
```java
public NodeJG(String cluster) {
    // 1. Create JChannel (group communication endpoint)
    channel = new JChannel(); // Default config or JChannel("udp.xml")
    
    // 2. Set this as receiver (implements Receiver interface)
    channel.setReceiver(this);
    
    // 3. Connect to named cluster
    channel.connect(cluster);  // Blocks until joined
    
    // 4. Store local address for logging
    addr = channel.getAddress();
    localName = addr.toString(); // e.g., "192.168.1.5:7800"
    
    // 5. Create collaborating objects
    sender = new SendMessages(channel);      // Needs channel to send
    stateDB = new ClientDB();                // Empty initially
    services = new ServicesBank(sender, stateDB, localName);
    processMsg = new ProcessMsgBank(stateDB, localName);
    
    // 6. Request current state (critical for new nodes)
    channel.getState(null, 10000); // Timeout in ms
}
```

**Method: receive(Message msg)**
```java
public void receive(Message msg) {
    // 1. Log reception (optional but useful)
    LOGGER.fine("Message from " + msg.getSrc());
    
    // 2. Extract and validate payload
    Object obj = msg.getObject();
    if (!(obj instanceof OperationsBank)) {
        LOGGER.severe("Invalid message type");
        return;
    }
    
    // 3. Cast to expected type
    OperationsBank op = (OperationsBank) obj;
    
    // 4. Process with proper synchronization
    synchronized (stateDB) {
        Client result = processMsg.processOpn(op);
        // Log result for debugging
        if (result != null) {
            LOGGER.fine("Operation " + op.getOperation() + 
                       " successful for account " + op.getAccountNumber());
        }
    }
}
```

**Critical Points**:
- **Thread context**: Runs in JGroups receiver thread, NOT main thread
- **Exception handling**: Exceptions should be caught; don't let them propagate
- **Synchronization**: MUST lock stateDB before modification

### 7.2 ServicesBank Implementation Details

**Service Method Pattern**:
```java
public Client put(int accountNumber, String name, double balance) {
    // 1. Create operation object
    OperationsBank op = new OperationsBank();
    op.setOperation("PUT");
    op.setAccountNumber(accountNumber);
    op.setName(name);
    op.setBalance(balance);
    
    // 2. Send to all nodes (including self)
    sender.sendMsg(op);
    
    // 3. Wait for operation to complete (local processing)
    // NOTE: Operation processed in receive() method
    // Return is immediate; actual processing is asynchronous
    
    return null; // or implement synchronous wait
}
```

**Synchronous vs Asynchronous**:

**Asynchronous (simpler)**:
- Service method returns immediately
- Operation processed when message arrives
- No return value (or return null)

**Synchronous (complex but better UX)**:
```java
public Client put(...) {
    OperationsBank op = ...;
    
    // Create a future/promise for this operation
    CompletableFuture<Client> future = new CompletableFuture<>();
    pendingOperations.put(op.getId(), future);
    
    sender.sendMsg(op);
    
    // Wait for completion (with timeout)
    try {
        return future.get(5, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        return null; // Operation timeout
    }
}
```

### 7.3 ProcessMsgBank Implementation Details

**Operation Dispatcher**:
```java
public Client processOpn(OperationsBank op) {
    String operation = op.getOperation();
    
    switch(operation) {
        case "PUT":
            return processPut(op);
        case "GET":
            return processGet(op);
        case "UPDATE":
            return processUpdate(op);
        case "REMOVE":
            return processRemove(op);
        default:
            LOGGER.severe("Unknown operation: " + operation);
            return null;
    }
}

private Client processPut(OperationsBank op) {
    Client client = new Client(
        op.getAccountNumber(),
        op.getName(),
        op.getBalance()
    );
    
    // Check for duplicate
    if (stateDB.exists(op.getAccountNumber())) {
        LOGGER.warning("Account already exists: " + op.getAccountNumber());
        return null; // Or throw exception
    }
    
    stateDB.addClient(client);
    LOGGER.info("Client added: " + op.getAccountNumber());
    return client;
}
```

**Error Handling Strategy**:
- **Invalid operations**: Log error, return null
- **Duplicate keys**: Ignore or return existing client
- **Missing clients**: Return null for GET/UPDATE/REMOVE
- **Never throw exceptions**: Would crash receiver thread

### 7.4 SendMessages Implementation Details

**Simple Wrapper**:
```java
public class SendMessages {
    private JChannel channel;
    
    public SendMessages(JChannel channel) {
        this.channel = channel;
    }
    
    public void sendMsg(OperationsBank op) {
        try {
            Message msg = new Message(null, op);
            channel.send(msg);
        } catch (Exception e) {
            LOGGER.severe("Failed to send message: " + e.getMessage());
            // Consider retry logic or notify user
        }
    }
}
```

### 7.5 ClientDB Implementation Details

**Internal Structure Options**:

**Option 1: HashMap (recommended)**:
```java
public class ClientDB implements Serializable {
    private Map<Integer, Client> clients = new HashMap<>();
    
    public void addClient(Client c) {
        clients.put(c.getAccountNumber(), c);
    }
    
    public Client getClient(int accountNumber) {
        return clients.get(accountNumber);
    }
    
    public boolean exists(int accountNumber) {
        return clients.containsKey(accountNumber);
    }
}
```

**Option 2: ArrayList**:
```java
public class ClientDB implements Serializable {
    private List<Client> clients = new ArrayList<>();
    
    public void addClient(Client c) {
        clients.add(c);
    }
    
    public Client getClient(int accountNumber) {
        return clients.stream()
            .filter(c -> c.getAccountNumber() == accountNumber)
            .findFirst()
            .orElse(null);
    }
}
```

**State Transfer Methods**:
```java
public void createBank(ClientDB source) {
    // Clear existing data
    this.clients.clear();
    
    // Copy all clients from source
    this.clients.putAll(source.clients);
    
    // Or: this.clients = new HashMap<>(source.clients);
}
```

---

## 8. Communication Sequences

### 8.1 PUT Operation Sequence

```
User          MainBank      ServicesBank    SendMessages    JChannel        NodeJG (all nodes)    ProcessMsgBank    ClientDB
 |                |               |               |             |                    |                    |              |
 |--PUT cmd----->|               |               |             |                    |                    |              |
 |               |--put()------->|               |             |                    |                    |              |
 |               |               |--create op--->|             |                    |                    |              |
 |               |               |               |--sendMsg()->|                    |                    |              |
 |               |               |               |             |--multicast-------->|                    |              |
 |               |               |               |             |                    |--receive(msg)----->|              |
 |               |               |               |             |                    |                    |--processOpn->|
 |               |               |               |             |                    |                    |              |--addClient()
 |               |               |               |             |                    |                    |<--Client-----|
 |               |               |               |             |                    |<--Client-----------|              |
 |<--response----|<--return------|<--------------|<------------|<-------------------|                    |              |
```

**Timeline**:
1. User enters PUT command in menu
2. MainBank calls ServicesBank.put()
3. ServicesBank creates OperationsBank object
4. SendMessages wraps in Message and sends
5. JChannel multicasts to ALL nodes (including sender)
6. Each NodeJG receives in separate thread
7. ProcessMsgBank processes operation
8. ClientDB updated on all nodes
9. User sees confirmation

**Duration**: Typically 10-50ms depending on network

### 8.2 GET Operation Sequence

```
User          MainBank      ServicesBank    SendMessages    JChannel    NodeJG (local)    ProcessMsgBank    ClientDB
 |                |               |               |             |             |                    |              |
 |--GET cmd----->|               |               |             |             |                    |              |
 |               |--get()------->|               |             |             |                    |              |
 |               |               |--create op--->|             |             |                    |              |
 |               |               |               |--sendMsg()->|             |                    |              |
 |               |               |               |             |--multicast->|                    |              |
 |               |               |               |             |             |--receive(msg)----->|              |
 |               |               |               |             |             |                    |--processOpn->|
 |               |               |               |             |             |                    |              |--getClient()
 |               |               |               |             |             |                    |<--Client-----|
 |               |               |               |             |             |<--Client-----------|              |
 |               |               |               |             |             |                    |              |
 |<--display-----|<--return------|<--------------|<------------|<------------|                    |              |
```

**Note**: GET is read-only but still uses multicast for consistency.

### 8.3 State Transfer Sequence (New Node Joining)

```
Node1 (existing)                          Node2 (new)                          JGroups Cluster
      |                                         |                                      |
      |--normal operations--                    |                                      |
      |                                         |--new NodeJG()--                      |
      |                                         |                |                     |
      |                                         |                |--JChannel.connect()->|
      |<--view change notification--------------|<---------------|<--------------------|
      |                                         |                |--getState(timeout)->|
      |<--getState() callback-------------------|<---------------|<--------------------|
      |                                         |                                      |
      |--synchronized(stateDB)--                |                                      |
      | |                                       |                                      |
      | |--serialize ClientDB--                 |                                      |
      | |                      |                |                                      |
      | |                      |--send stream-->|                                      |
      | |                                       |--setState() callback--               |
      | |<--unlock--                            | |                     |              |
      |                                         | |--deserialize--------|              |
      |                                         | |                     |              |
      |                                         | |--stateDB.createBank-|              |
      |                                         | |<--------------------|              |
      |                                         |<-state transfer complete             |
      |                                         |                                      |
      |--resume operations--                    |--ready for operations--              |
      |                                         |                                      |
```

**Critical Timing**:
- T1: Node2 connects (view change sent to all)
- T2: Node1 pauses operations, locks database
- T3: Serialization (100ms - 2s depending on DB size)
- T4: Network transfer (10ms - 500ms)
- T5: Node2 deserializes and applies state
- T6: Both nodes resume normal operations

**Messages during transfer**:
- Buffered by JGroups
- Delivered to Node2 AFTER setState() completes
- Ensures Node2 sees consistent state

### 8.4 Concurrent Operations Handling

```
Scenario: Two nodes simultaneously execute operations

Time    Node1                           Node2                       Result (all nodes)
----    -----                           -----                       ------------------
T0      PUT(acct=1, bal=100)            PUT(acct=2, bal=200)       DB: []
        |                               |
T1      |--send op1------------------>  |                          
        |                               |--send op2--------------->
T2      |<--recv op1 (from self)        |<--recv op1 (from Node1)  
        |--process: add acct1           |--process: add acct1
T3      |<--recv op2 (from Node2)       |<--recv op2 (from self)   
        |--process: add acct2           |--process: add acct2
T4      DB: [acct1, acct2]              DB: [acct1, acct2]         DB: [acct1, acct2] ✓

Total Order ensures both nodes process op1 then op2 (or both op2 then op1)
```

**Conflict Resolution**:
```
Scenario: Conflicting operations

Time    Node1                           Node2                       Resolution
----    -----                           -----                       ----------
T0      PUT(acct=1, bal=100)            PUT(acct=1, bal=200)       
        |--send op1------------------>  |--send op2--------------->
        
T2      Total Order Multicast decides:  op1 arrives first         
        |<--recv op1                    |<--recv op1
        |--add acct1, bal=100           |--add acct1, bal=100
        
T3      |<--recv op2                    |<--recv op2
        |--already exists! Ignore       |--already exists! Ignore
        
Result: DB: [acct1: bal=100]            DB: [acct1: bal=100]       Consistent ✓
```

**Key**: First operation wins (deterministic conflict resolution).
