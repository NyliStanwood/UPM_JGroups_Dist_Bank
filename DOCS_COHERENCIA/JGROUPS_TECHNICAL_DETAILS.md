# JGroups Implementation Details - Technical Reference

## How Coherence is Achieved

### 1. Ordered Multicast Protocol Stack

JGroups uses a layered protocol architecture. Your system uses the default UDP-based stack:

```
Application Layer (Your Code)
    ↓
[Channel] - Message API entry point
    ↓
[GMS] Group Membership Service
    ├─ Tracks cluster membership
    ├─ Detects node joins/failures
    └─ Maintains consistent view
    ↓
[FRAG] Fragmentation
    ├─ Splits large messages
    └─ Reassembles on receive
    ↓
[UFC/MFC] Flow Control
    ├─ Prevents sender overload
    └─ Regulates message rate
    ↓
[STABLE] Garbage Collection
    ├─ Acknowledges old messages
    └─ Frees memory
    ↓
[GMS] Group Membership Service (again in pipeline)
    ↓
[pbcast.NAKACK2] Reliable Ordered Multicast
    ├─ **CRITICAL FOR COHERENCE**
    ├─ Assigns sequence numbers to messages
    ├─ Detects missing messages
    ├─ Requests retransmission
    └─ Delivers in STRICT ORDER
    ↓
[UNICAST3] Reliable Unicast (Node-to-node)
    ├─ Point-to-point reliability
    └─ Retransmission on loss
    ↓
[VERIFY_SUSPECT] Suspect Verification
    ├─ Validates suspected dead nodes
    └─ Prevents false positives
    ↓
[FD_SOCK] Failure Detection
    ├─ Detects node crashes
    └─ Quick notification
    ↓
[PING] Cluster Discovery
    ├─ Initial cluster membership
    └─ Periodic member discovery
    ↓
[UDP] Network Transport
    └─ Actual network transmission (port 7600 default)
```

### 2. NAKACK2 (Reliable Broadcast with Ordering)

This is the **KEY protocol** that ensures **COHERENCE**:

#### Sequence Number Assignment

```java
// When you call: channel.send(msg)
// NAKACK2 internally:
msg.sequence_number = current_sequence + 1;  // e.g., 1, 2, 3, ...
msg.sender_id = this_node_id;                // e.g., LAPTOP-NYLI1-65077
```

#### Message Format on Wire

```
Message {
  sender_id: LAPTOP-NYLI1-65077
  sequence_number: 42
  type: NAKACK_DATA
  payload: {
    operation: CREATE_CLIENT,
    client: {id: 7, name: "Giovanni", balance: 70}
  }
}
```

#### Ordering Guarantee at Reception

```
Sender's View of Messages:
MSG 1 → [CREATE_CLIENT(A)]
MSG 2 → [CREATE_CLIENT(B)]
MSG 3 → [UPDATE_CLIENT(A)]
MSG 4 → [DELETE_CLIENT(B)]

Receiver 1's View (guaranteed same order):
MSG 1 → [CREATE_CLIENT(A)]
MSG 2 → [CREATE_CLIENT(B)]
MSG 3 → [UPDATE_CLIENT(A)]
MSG 4 → [DELETE_CLIENT(B)]

Receiver 2's View (guaranteed same order):
MSG 1 → [CREATE_CLIENT(A)]
MSG 2 → [CREATE_CLIENT(B)]
MSG 3 → [UPDATE_CLIENT(A)]
MSG 4 → [DELETE_CLIENT(B)]

Result: All nodes execute operations in identical order
        → Identical state evolution
        → COHERENCE ACHIEVED
```

#### Loss Recovery Mechanism

```
Scenario: Network packet loss - Node 2 misses MSG 3

Expected Sequence: 1, 2, 3, 4, 5
Actual Received:   1, 2,    4, 5    ← Gap detected at MSG 3

NAKACK2 Action:
  1. Detects gap in sequence numbers
  2. Identifies missing: MSG 3 from sender LAPTOP-NYLI1-65077
  3. Sends NACK (Negative Acknowledgment): "Resend MSG 3"
  4. Sender retransmits MSG 3
  5. Node 2 receives MSG 3
  6. Node 2 now has: 1, 2, 3, 4, 5

Guarantee: Node 2 receives ALL messages in correct order
           Redundancy detection prevents duplicate processing
```

### 3. Deterministic State Machine Replication

Your code implements **State Machine Replication Pattern**:

```java
// Each node executes identical state machine

public class StateMachine {
    ClientDB state = new ClientDB();  // Initial state

    void applyOperation(OperationsBank op) {
        switch(op.operation) {
            case CREATE_CLIENT:
                state.createClient(op.getClient());
                break;
            case UPDATE_CLIENT:
                state.updateClient(op.accountNumber, op.balance);
                break;
            // ... all cases
        }
    }
}

// All nodes:
initial_state = {}
receive(CREATE_A)   → state = {A}
receive(CREATE_B)   → state = {A, B}
receive(UPDATE_A)   → state = {A', B}
receive(DELETE_B)   → state = {A'}

// Result: All nodes reach identical state
```

**Mathematical Guarantee:**

```
If:
  - All nodes start with same state
  - All nodes execute same operations
  - Operations execute in same order
  - Operations are deterministic (same input → same output)

Then:
  - All nodes will have same state at all times

Your System: ✓ Satisfies all conditions
```

### 4. Synchronized Access (Thread Safety)

```java
// In NodeJG.receive():
synchronized(stateDB) {
    Client result = processMsg.processOpn(op);
}

// In NodeJG.setState():
synchronized(stateDB) {
    stateDB.createBank(clientDBinput);
}

// Purpose: Multiple JGroups threads may call receive() simultaneously
// Synchronization ensures one operation applies at a time
// Prevents interleaving of state updates
```

---

## How State Transfer Works

### 1. State Transfer Architecture

```
             ┌─────────────────────────┐
             │  New Node Joins Cluster │
             └────────────┬────────────┘
                          │
                          ▼
            ┌─────────────────────────┐
            │  NodeJG Constructor Executes
            │  1. channel = new JChannel()
            │  2. channel.setReceiver(this)
            │  3. channel.connect("BankCluster")
            │  4. stateDB = new ClientDB()  ← Empty!
            └────────────┬────────────┘
                          │
                          ▼
            ┌─────────────────────────┐
            │  Initiate State Request │
            │  channel.getState(null, 10000)
            │  (10 second timeout)
            └────────────┬────────────┘
                          │
                          ▼
          ┌───────────────────────────────┐
          │  JGroups Selects State Provider
          │  (Existing cluster member)
          └────────────┬──────────────────┘
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
    Node 1         Node 2        Node N
    (elected)      (ignored)     (ignored)

    Provider: YES  Provider: NO  Provider: NO
        │
        ▼
    ┌────────────────────────────┐
    │ Call Provider's getState()
    │ if (msg.getType() == GETSTATE_REQ) {
    │     provider_node.getState(output_stream);
    │ }
    └────────┬───────────────────┘
             │
             ▼
    ┌────────────────────────────┐
    │ Provider Serializes State  │
    │ Util.objectToStream(       │
    │   stateDB,                 │
    │   new DataOutputStream()   │
    │ )
    │ Result: Binary stream of   │
    │ complete ClientDB          │
    └────────┬───────────────────┘
             │
             ▼
    ┌────────────────────────────┐
    │  Network Transfer          │
    │  Binary Stream →→→ Network │
    │  "Please copy this data"    │
    └────────┬───────────────────┘
             │
             ▼
    ┌────────────────────────────┐
    │ New Node Receives Stream   │
    │ Automatically invokes:      │
    │ this.setState(input_stream)│
    └────────┬───────────────────┘
             │
             ▼
    ┌────────────────────────────┐
    │ Deserialize Received State │
    │ ClientDB clientDB =        │
    │   Util.objectFromStream(   │
    │     new DataInputStream()  │
    │   )                         │
    │ Result: Complete HashMap   │
    │ with all clients           │
    └────────┬───────────────────┘
             │
             ▼
    ┌────────────────────────────┐
    │ Apply to Local stateDB     │
    │ synchronized(stateDB) {    │
    │   stateDB.createBank(      │
    │     clientDB               │
    │   )                         │
    │ }                           │
    └────────┬───────────────────┘
             │
             ▼
    ┌────────────────────────────┐
    │  State Transfer Complete   │
    │  New node.stateDB ===      │
    │  Provider.stateDB          │
    │  ✓ Fully replicated        │
    └────────────────────────────┘
```

### 2. Serialization Protocol

**ClientDB Serialization Format:**

```
Binary Stream Layout:
┌─────────────────┬──────────────┬───────────────────┐
│ Object Header   │ HashMap Size │ HashMap Entries   │
├─────────────────┼──────────────┼───────────────────┤
│ Serial Version  │ N (e.g., 6)  │ Key1 → Value1     │
│ Class Metadata  │              │ Key2 → Value2     │
│                 │              │ ...               │
│                 │              │ KeyN → ValueN     │
└─────────────────┴──────────────┴───────────────────┘

Example (6 clients):
┌─────────────────────────────────────────────────────┐
│ ClientDB {                                          │
│   1 → Client(1, "Angel", 10)                       │
│   2 → Client(2, "Bernardo", 20)                    │
│   3 → Client(3, "Carlos", 30)                      │
│   4 → Client(4, "Daniel", 40)                      │
│   5 → Client(5, "Eugenio", 50)                     │
│   6 → Client(6, "Zamorano", 60)                    │
│ }                                                   │
└─────────────────────────────────────────────────────┘
     ↓ Serialized via ObjectOutputStream ↓
┌─────────────────────────────────────────────────────┐
│ [Binary Data - Not Human Readable]                  │
│ ACED 0005 7372 0048 6573 2E75 706D ... (hex)       │
│ Size: ~1-5 KB (depending on number of clients)      │
└─────────────────────────────────────────────────────┘
     ↓ Transmitted over Network ↓
┌─────────────────────────────────────────────────────┐
│ Received by New Node                                │
│ Deserialized via ObjectInputStream                  │
│ Result: Complete ClientDB object                    │
│ Ready to use: stateDB.createBank(receivedDB)       │
└─────────────────────────────────────────────────────┘
```

### 3. State Transfer vs Continued Operations

**Timeline:**

```
Provider Node (executing operations):          New Node (receiving state):
T0: state_version_A = {6 clients}
T1: state_version_B = {7 clients}             Begin STATE TRANSFER
T2: state_version_C = {7 clients + update}    Network transfer in progress
T3: [STATE SENT]                              ← Receives StateVersion_A/B/C
T4: state_version_D = {8 clients}             ← Applies received state
                                              ← Now has StateVersion_A/B/C
T5:                                           <- Missing operations D onwards!
```

**JGroups Solution: STATE TRANSFER + RELIABLE MULTICAST**

```
During STATE TRANSFER:
  ✓ Multicast operations CONTINUE (via NAKACK2)
  ✓ All nodes (including new one) receive operations
  ✓ New node buffers received operations
  ✓ After STATE TRANSFER completes
  ✓ New node applies buffered operations
  ✓ Result: Complete and current state!

Timeline Corrected:
T0: state_version_A = {6 clients}
T1: state_version_B = {7 clients}              Begin STATE TRANSFER
T2: version_C = {7 clients + update} [Multicast]→ New node BUFFERS
T3: [STATE TRANSFER sends version_B/C]         New node receives state
T4: version_D = {8 clients} [Multicast]       → New node BUFFERS
T5: [STATE TRANSFER completes]                 New node applies state_B/C
                                              New node applies buffered ops (D)
                                              New node now = Provider node
```

---

## Message Flow Example: CREATE_CLIENT Operation

### Step-by-Step Execution Trace

```
┌─────────────────────────────────────────────────────────────────────┐
│ USER INPUT: Menu Option 1 (Put/Create Client)                      │
│ Input: ID=7, Name="Giovanni", Balance=70                           │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
        ┌──────────────────▼──────────────────┐
        │ MainBank.run()                      │
        │ Scanner reads user input            │
        │ Calls: menu.execCmdService(option)  │
        └──────────────────┬──────────────────┘
                           │
        ┌──────────────────▼──────────────────────────────┐
        │ MenuCommands.execute()                         │
        │ case 1: (Create)                               │
        │   client = new Client(7, "Giovanni", 70)      │
        │   services.createClient(client)               │
        └──────────────────┬──────────────────────────────┘
                           │
        ┌──────────────────▼──────────────────────────────┐
        │ ServicesBank.createClient()                    │
        │ Creates: OperationsBank(CREATE_CLIENT,        │
        │          client,                               │
        │          localName)                            │
        │ Calls: sender.sendMessage(operation)           │
        └──────────────────┬──────────────────────────────┘
                           │
        ┌──────────────────▼──────────────────────────────┐
        │ SendMessages.sendMessage()                     │
        │ msg = new ObjectMessage(null, operation)       │
        │ null = broadcast to ALL cluster nodes          │
        │ channel.send(msg)                              │
        └──────────────────┬──────────────────────────────┘
                           │
        ┌──────────────────▼──────────────────────────────┐
        │ JGroups Protocol Stack (Node 1)               │
        │                                                │
        │ [Application Layer]                            │
        │         ↓                                      │
        │ [PING] Cluster discovery                       │
        │         ↓                                      │
        │ [FD] Failure detection                         │
        │         ↓                                      │
        │ [NAKACK2] Adds sequence #42                   │
        │         ↓                                      │
        │ [FRAG] Fragment if needed                      │
        │         ↓                                      │
        │ [UDP] Serialize → Network bytes                │
        │         ↓                                      │
        │ Multicast to Port 7600                        │
        │ Payload: CreateClient(7, Giovanni, 70)         │
        └──────────────────┬──────────────────────────────┘
                           │
        ┌──────────────────┴──────────────────┬─────────────────────────┐
        │                                     │                         │
   [Network - UDP Multicast]                  │                         │
   Destination: 127.0.0.1:7600               │                         │
   All nodes receive packet                   │                         │
        │                                     │                         │
        ▼                                     ▼                         ▼
┌──────────────────┐                 ┌──────────────────┐      ┌──────────────────┐
│  Node 1 (Self)   │                 │  Node 2 (Replica)│      │  Node 3 (Replica)│
├──────────────────┤                 ├──────────────────┤      ├──────────────────┤
│ [JGroups Stack]  │                 │ [JGroups Stack]  │      │ [JGroups Stack]  │
│         ↓        │                 │         ↓        │      │         ↓        │
│ [UDP] Receive    │                 │ [UDP] Receive    │      │ [UDP] Receive    │
│         ↓        │                 │         ↓        │      │         ↓        │
│ [FRAG] Reassembl │                 │ [FRAG] Reassembl │      │ [FRAG] Reassembl │
│         ↓        │                 │         ↓        │      │         ↓        │
│[NAKACK2]Verify #42│                │[NAKACK2]Verify #42│     │[NAKACK2]Verify #42│
│        YES ✓     │                 │        YES ✓     │      │        YES ✓     │
│         ↓        │                 │         ↓        │      │         ↓        │
│ [GMS] Deliver    │                 │ [GMS] Deliver    │      │ [GMS] Deliver    │
│ to Receiver      │                 │ to Receiver      │      │ to Receiver      │
│         ↓        │                 │         ↓        │      │         ↓        │
│ Deserialized Msg │                 │ Deserialized Msg │      │ Deserialized Msg │
│ OperationsBank   │                 │ OperationsBank   │      │ OperationsBank   │
│ (CREATE_CLIENT)  │                 │ (CREATE_CLIENT)  │      │ (CREATE_CLIENT)  │
└────────┬─────────┘                 └────────┬─────────┘      └────────┬─────────┘
         │                                    │                        │
         │    ┌────────────────────────────────┴────────────────────┐  │
         │    │ All three nodes receive IDENTICAL message           │  │
         │    │ Sequence #42: CREATE_CLIENT(7, Giovanni, 70)       │  │
         │    │ Order GUARANTEED by NAKACK2                         │  │
         └────┼────────────────────────────────┬────────────────────┘  │
              │                                │                       │
         ┌────▼─────────────┐             ┌────▼─────────────┐    ┌────▼─────────────┐
         │ NodeJG.receive() │             │ NodeJG.receive() │    │ NodeJG.receive() │
         │ (Node 1)         │             │ (Node 2)         │    │ (Node 3)         │
         │                  │             │                  │    │                  │
         │ synchronized(    │             │ synchronized(    │    │ synchronized(    │
         │   stateDB) {     │             │   stateDB) {     │    │   stateDB) {     │
         │   obj =          │             │   obj =          │    │   obj =          │
         │    msg.getObject│             │    msg.getObject│    │    msg.getObject│
         │   if (instanceof │             │   if (instanceof │    │   if (instanceof │
         │    OperationsBank)             │    OperationsBank)    │    OperationsBank)
         │   op = (Operatio)             │   op = (Operatio)     │   op = (Operatio)
         └────┬─────────────┘             └────┬─────────────┘    └────┬─────────────┘
              │                                │                       │
         ┌────▼──────────────────────────┐ ┌──▼──────────────────────┐│───────────────┐
         │ ProcessMsgBank.processOpn()   │ │ProcessMsgBank.processOp()││ProcessMsgBank │
         │ switch(op.getOperation())     │ │switch(op.getOperation()) ││.processOpn()  │
         │   case CREATE_CLIENT:         │ │  case CREATE_CLIENT:     ││case CREATE:   │
         │     client =                  │ │    client =              ││  client =     │
         │       op.getClient()          │ │      op.getClient()      ││   op.getClient
         │     clientDB.createClient()   │ │    clientDB.createClient ││ clientDB.     │
         │     return client             │ │    return client         ││ createClient()│
         └────┬──────────────────────────┘ └──┬──────────────────────┘│return client  │
              │                                │                      └────┬──────────┘
         ┌────▼───────────────────────────────▼──────────────────────────┴──┐
         │                 ClientDB State Updated                          │
         │                                                                 │
         │  Node 1.stateDB.clientDB:              Node 2.stateDB.clientDB: │
         │  {                                      {                       │
         │    1 → Client(1, Angel, 10),            1 → Client(1, Angel,10),│
         │    2 → Client(2, Bernardo, 20),        2 → Client(2, Bernardo) │
         │    ...                                  ...                     │
         │    7 → Client(7, Giovanni, 70) ✓       7 → Client(7, Giovanni)│
         │  }                                      }                       │
         │                                                                 │
         │  Node 3.stateDB.clientDB:                                      │
         │  {                                                              │
         │    1 → Client(1, Angel, 10),                                   │
         │    2 → Client(2, Bernardo, 20),                               │
         │    ...                                                          │
         │    7 → Client(7, Giovanni, 70) ✓                              │
         │  }                                                              │
         │                                                                 │
         │ ✓ ALL THREE NODES HAVE IDENTICAL STATE                        │
         │ ✓ COHERENCE ACHIEVED                                          │
         └────────────────────────────────────────────────────────────────┘
```

---

## Summary: Why It Works

### Coherence Mechanism

1. **Multicast Distribution**: All nodes receive same messages
2. **NAKACK2 Ordering**: Messages delivered in strict order
3. **Deterministic Processing**: Same operations + same order = same state
4. **Synchronized Updates**: No interleaving corruption

### State Transfer Mechanism

1. **Receiver Interface**: JGroups calls `getState()` and `setState()`
2. **Serialization**: Complete state object → binary stream
3. **Network Transfer**: Binary stream → network → receiving node
4. **Deserialization**: Binary stream → complete object
5. **Atomic Replace**: New node swaps empty DB with received DB

### Combined Effect

- **New nodes reach current state** (STATE TRANSFER)
- **All changes propagate consistently** (COHERENCE)
- **System remains operational during changes** (Concurrent multicast + state transfer)
