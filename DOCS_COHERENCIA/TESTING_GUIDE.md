# JGroups Bank Replication - Testing Guide

## Quick Start Testing

### Requirement 1: COHERENCE Test

#### Step 1: Start First Node (Primary)
```powershell
# Terminal 1
java -cp "bin;lib\jgroups-5.0.0.Final.jar" `
  -Djava.net.preferIPv4Stack=true `
  -Djgroups.bind_addr=127.0.0.1 `
  es.upm.dit.cnvr_fcon.bank_2025.bank.MainBank BankCluster
```

**Expected Output:**
```
GMS: address=<NODE_ID>, cluster=clusterBank, physical address=127.0.0.1:XXXX
---
LAPTOP-...: no members discovered after 3000 ms
** view: [<NODE_ID>|0] (1) [<NODE_ID>]
Joined cluster clusterBank with address <NODE_ID>
>>> Enter option: 1) Put. 2) Get. 3) Remove. 4) Update  5) Values 7) Init 0) Exit
```

#### Step 2: Initialize Test Data
```
Input: 7
```

**Expected Output:**
```
>> Sent operation: Operation: CREATE_CLIENT[1, Angel, 10]
>> Sent operation: Operation: CREATE_CLIENT[2, Bernardo, 20]
... (6 clients created)
```

#### Step 3: Verify Local State
```
Input: 5
```

**Expected Output:**
```
List of values in the bank:
[1, Angel, 10]
[2, Bernardo, 20]
[3, Carlos, 30]
[4, Daniel, 40]
[5, Eugenio, 50]
[6, Zamorano, 60]
```

#### Step 4: Start Second Node (Replica)
```powershell
# Terminal 2
java -cp "bin;lib\jgroups-5.0.0.Final.jar" `
  -Djava.net.preferIPv4Stack=true `
  -Djgroups.bind_addr=127.0.0.1 `
  es.upm.dit.cnvr_fcon.bank_2025.bank.MainBank BankCluster
```

**Expected Output (Node 1 changes):**
```
** view: [<NODE_ID_1>|0] (2) [<NODE_ID_1>, <NODE_ID_2>]  # Cluster size increased!
```

**Expected Output (Node 2):**
```
received state (messages in chat history):
>>> Enter option: 1) Put. 2) Get. 3) Remove. 4) Update  5) Values 7) Init 0) Exit
```

#### Step 5: Verify State Transfer
```
Input (Terminal 2): 5
```

**Expected Output:**
```
List of values in the bank:
[1, Angel, 10]
[2, Bernardo, 20]
[3, Carlos, 30]
[4, Daniel, 40]
[5, Eugenio, 50]
[6, Zamorano, 60]
```

✓ **COHERENCE VERIFIED**: Node 2 has identical state as Node 1 despite fresh start!

---

### Requirement 2: STATE TRANSFER Test (Continued from above)

#### Step 6: Create New Client in Node 1
```
Input (Terminal 1): 1
Enter: 7 Giovanni 70
```

**Expected Output (Terminal 1):**
```
>> Sent operation: Operation: CREATE_CLIENT[7, Giovanni, 70]
[LAPTOP-...]: Client created: [7, Giovanni, 70]
```

**Expected Output (Terminal 2):**
```
>> Sent operation: Operation: CREATE_CLIENT[7, Giovanni, 70]
[LAPTOP-...]: Client created: [7, Giovanni, 70]
```

#### Step 7: Verify Coherence After New Operation
```
Input (Terminal 1): 5
Input (Terminal 2): 5
```

**Both Terminals Should Show:**
```
List of values in the bank:
[1, Angel, 10]
[2, Bernardo, 20]
[3, Carlos, 30]
[4, Daniel, 40]
[5, Eugenio, 50]
[6, Zamorano, 60]
[7, Giovanni, 70]
```

✓ **STATE TRANSFER + COHERENCE VERIFIED**: Both nodes received initial state AND all subsequent operations!

---

### Advanced Test: Node Failure & Recovery

#### Step 1: Run Two Nodes
```
Terminal 1 & 2: Running (see above steps)
```

#### Step 2: Stop Node 2
```
Terminal 2: Press Ctrl+C
```

**Node 1 Output:**
```
** view: [<NODE_ID_1>|1] (1) [<NODE_ID_1>]  # View updated to single node
```

#### Step 3: Create Operations on Remaining Node
```
Input (Terminal 1): 1
Enter: 8 Helena 80
```

#### Step 4: Restart Node 2
```
# Terminal 2 (new instance)
java -cp "bin;lib\jgroups-5.0.0.Final.jar" `
  -Djava.net.preferIPv4Stack=true `
  -Djgroups.bind_addr=127.0.0.1 `
  es.upm.dit.cnvr_fcon.bank_2025.bank.MainBank BankCluster
```

**Node 2 will receive state from Node 1:**
```
received state (messages in chat history):
```

#### Step 5: Verify Recovery
```
Input (Terminal 2): 5
```

**Should Show:**
```
[1, Angel, 10]
[2, Bernardo, 20]
[3, Carlos, 30]
[4, Daniel, 40]
[5, Eugenio, 50]
[6, Zamorano, 60]
[7, Giovanni, 70]
[8, Helena, 80]
```

✓ **RECOVERY VERIFIED**: Node 2 recovered all operations including those during its absence!

---

## Expected Log Patterns

### Successful State Transfer
```
[TIMESTAMP][INFO] [local_addr: <ID>, name: <NAME>] [org.jgroups.JChannel setAddress]
---
GMS: address=<ID>, cluster=clusterBank, physical address=127.0.0.1:<PORT>
---
[TIMESTAMP][INFO] [<NAME>: no members discovered after 3000 ms]
** view: [<ID>|0] (1) [<ID>]
[TIMESTAMP][INFO] [Joined cluster clusterBank with address <ID>]
```

### Multicast Message Reception
```
[TIMESTAMP][FINE] [Message received from <SENDER_ID>]
>> Sent operation: Operation: CREATE_CLIENT[...]
[TIMESTAMP][INFO] [<LOCAL_ID> -> Client created: [...]]
[TIMESTAMP][FINE] [Operation CREATE_CLIENT processed for client: [...]]
```

### New Node State Transfer
```
[TIMESTAMP][FINEST] [Invocation to setState]
received state (messages in chat history):
** view: [<PRIMARY_ID>|<VIEW_ID>] (2) [<PRIMARY_ID>, <NEW_ID>]
```

---

## Troubleshooting

### Issue: Nodes not discovering each other
**Symptom:** View remains size 1 after starting second node
**Cause:** Network binding or PING protocol issue
**Solution:** 
- Ensure both using same cluster name: `BankCluster`
- Check both bound to `127.0.0.1` or both to same IP
- Verify firewall not blocking UDP

### Issue: State not transferring
**Symptom:** New node shows empty client list
**Cause:** `getState()` timeout or serialization error
**Solution:**
- Increase timeout: `channel.getState(null, 30000)` (30s)
- Verify ClientDB implements Serializable
- Check logs for exceptions

### Issue: Operations not synchronized
**Symptom:** Different clients visible on different nodes
**Cause:** Message ordering or application logic issue
**Solution:**
- Verify `receive()` method synchronized on stateDB
- Check message serialization in SendMessages
- Look for exceptions in ProcessMsgBank

---

## Verification Checklist

- [ ] Node 1 initializes successfully
- [ ] Node 2 joins and receives state
- [ ] Both nodes show same 6 clients after state transfer
- [ ] New client created on Node 1 appears on Node 2
- [ ] Client operations (update, delete) synchronized
- [ ] View membership updated correctly
- [ ] Multiple operations maintain order
- [ ] Nodes recover after restart

---

## Performance Notes

| Metric | Value |
|--------|-------|
| Initial State Transfer | Milliseconds (6 clients, localhost) |
| Operation Broadcast | Milliseconds (network latency dependent) |
| Cluster Discovery | ~1 second (PING interval) |
| Node Recovery | <5 seconds (includes state transfer + sync) |

---

## Architecture Visualization

```
┌─────────────────┐          ┌─────────────────┐
│   MainBank      │          │   MainBank      │
│    (Node 1)     │          │    (Node 2)     │
├─────────────────┤          ├─────────────────┤
│  ServicesBank   │          │  ServicesBank   │
│   (menu cmds)   │          │   (menu cmds)   │
├─────────────────┤          ├─────────────────┤
│  ClientDB       │          │  ClientDB       │
│  {clients}      │◄────────►│  {clients}      │
├─────────────────┤          ├─────────────────┤
│ ProcessMsgBank  │          │ ProcessMsgBank  │
│   (operation    │          │   (operation    │
│   processor)    │          │   processor)    │
├─────────────────┤          ├─────────────────┤
│   SendMessages  │          │   SendMessages  │
│  (multicast)    │          │  (multicast)    │
├─────────────────┤          ├─────────────────┤
│  NodeJG         │          │  NodeJG         │
│  (JGroups impl) │          │  (JGroups impl) │
├─────────────────┤          ├─────────────────┤
│   JChannel      │◄─Multicast──┤   JChannel      │
│                 │  (UDP)   │                 │
│  OperationsBank ◄──────────┤ OperationsBank  │
│  Messages       │          │ Messages        │
└─────────────────┘          └─────────────────┘
       ↓                              ↑
   Multicast to All Nodes in Cluster
```

