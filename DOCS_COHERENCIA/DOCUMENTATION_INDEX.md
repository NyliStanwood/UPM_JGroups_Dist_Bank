# JGroups Bank Replication - Documentation Index

## Overview

This project implements a replicated banking system using **JGroups** to achieve:

- ✓ **Coherence**: All replicas maintain identical state
- ✓ **State Transfer**: New nodes receive current state upon joining

---

## Documentation Map

### 📋 START HERE

- **[IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)**
  - Executive summary
  - Status and verification
  - Key findings
  - 5-minute read

### 🚀 QUICK START (30 minutes)

- **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)**
  - 30-second quick start
  - Menu operations reference
  - Compilation commands
  - Troubleshooting matrix
  - Architecture diagram

### 🧪 TESTING (1-2 hours)

- **[TESTING_GUIDE.md](TESTING_GUIDE.md)**
  - Step-by-step test procedures
  - Coherence test (Requirement 1)
  - State transfer test (Requirement 2)
  - Advanced test scenarios
  - Log pattern reference
  - Verification checklist

### 📚 DEEP UNDERSTANDING (1-2 hours)

- **[JGROUPS_REQUIREMENTS_ANALYSIS.md](JGROUPS_REQUIREMENTS_ANALYSIS.md)**
  - Detailed requirement explanation
  - How coherence is achieved
  - How state transfer is achieved
  - Guarantees and mechanisms
  - Data flow examples
  - Configuration notes

### 🔬 TECHNICAL DETAILS (2-3 hours)

- **[JGROUPS_TECHNICAL_DETAILS.md](JGROUPS_TECHNICAL_DETAILS.md)**
  - JGroups protocol stack breakdown
  - NAKACK2 protocol (ordered multicast)
  - State transfer architecture
  - Serialization protocol
  - Complete execution trace example
  - Why it works mathematically

### 🛡️ FAULT TOLERANCE (1-2 hours)

- **[FAULT_TOLERANCE.md](DOCS_FAULT_TOLERANCE/FAULT_TOLERANCE.md)**
  - Automatic failure detection
  - Quorum-based recovery
  - Process resurrection
  - Configuration flags
  - Testing procedures
  - Performance characteristics

---

## Reading Paths

### Path 1: "I want to run it" (30 minutes)

1. Read: IMPLEMENTATION_SUMMARY.md (overview)
2. Read: QUICK_REFERENCE.md (commands)
3. Execute: Compilation & launch
4. Verify: Run TESTING_GUIDE.md tests

### Path 2: "I want to understand it" (2 hours)

1. Read: IMPLEMENTATION_SUMMARY.md (overview)
2. Read: QUICK_REFERENCE.md (architecture)
3. Read: JGROUPS_REQUIREMENTS_ANALYSIS.md (how requirements are met)
4. Run: TESTING_GUIDE.md (verify understanding)
5. Read: JGROUPS_TECHNICAL_DETAILS.md (deep dive)

### Path 3: "I want to extend/modify it" (3+ hours)

1. Read entire: IMPLEMENTATION_SUMMARY.md
2. Study: QUICK_REFERENCE.md code structure
3. Read: JGROUPS_REQUIREMENTS_ANALYSIS.md data flow
4. Deep study: JGROUPS_TECHNICAL_DETAILS.md
5. Review source code: NodeJG.java, ClientDB.java
6. Experiment: TESTING_GUIDE.md advanced scenarios

---

## Quick Navigation

### By Topic

#### Coherence (Requirement 1)

- Overview: IMPLEMENTATION_SUMMARY.md → "Coherence Requirement"
- How it works: QUICK_REFERENCE.md → "How Coherence Works"
- Detailed: JGROUPS_REQUIREMENTS_ANALYSIS.md → "Requirement 1: COHERENCE"
- Technical: JGROUPS_TECHNICAL_DETAILS.md → "How Coherence is Achieved"
- Testing: TESTING_GUIDE.md → "Requirement 1: COHERENCE Test"

#### State Transfer (Requirement 2)

- Overview: IMPLEMENTATION_SUMMARY.md → "State Transfer Requirement"
- How it works: QUICK_REFERENCE.md → "How State Transfer Works"
- Detailed: JGROUPS_REQUIREMENTS_ANALYSIS.md → "Requirement 2: STATE TRANSFER"
- Technical: JGROUPS_TECHNICAL_DETAILS.md → "How State Transfer Works"
- Testing: TESTING_GUIDE.md → "Requirement 2: STATE TRANSFER Test"

#### Build & Deploy

- Compilation: QUICK_REFERENCE.md → "Compilation"
- Execution: QUICK_REFERENCE.md → "Quick Start"
- Configuration: QUICK_REFERENCE.md → "Configuration Files"
- Debugging: TESTING_GUIDE.md → "Troubleshooting"

#### Architecture & Design

- Components: QUICK_REFERENCE.md → "Core Classes"
- Design: IMPLEMENTATION_SUMMARY.md → "Implementation Architecture"
- Protocols: QUICK_REFERENCE.md → "Key Takeaways"
- Protocol Stack: JGROUPS_TECHNICAL_DETAILS.md → "Ordered Multicast Protocol Stack"

#### Testing & Verification

- Quick test: QUICK_REFERENCE.md → "Quick Test Checklist"
- Full test: TESTING_GUIDE.md → All sections
- Expected outputs: TESTING_GUIDE.md → "Expected Log Patterns"
- Troubleshooting: TESTING_GUIDE.md → "Troubleshooting"

---

## Document Purpose Summary

| Document                         | Purpose                          | Audience        | Time    |
| -------------------------------- | -------------------------------- | --------------- | ------- |
| IMPLEMENTATION_SUMMARY.md        | Executive overview               | Everyone        | 5 min   |
| QUICK_REFERENCE.md               | Quick lookup & commands          | Developers      | 15 min  |
| JGROUPS_REQUIREMENTS_ANALYSIS.md | Explain how requirements are met | Architects      | 60 min  |
| JGROUPS_TECHNICAL_DETAILS.md     | Deep technical explanation       | Advanced users  | 120 min |
| TESTING_GUIDE.md                 | Step-by-step testing             | QA & Developers | 90 min  |

---

## Key Files in Project

### Source Code

```
src/
├── es/upm/dit/cnvr_fcon/bank_2025/
│   ├── bank/
│   │   └── MainBank.java ..................... Entry point
│   ├── common/
│   │   ├── NodeJG.java ....................... JGroups integration (KEY)
│   │   ├── ClientDB.java ..................... Replicated state (KEY)
│   │   ├── Client.java ....................... Client data structure
│   │   ├── OperationsBank.java ............... Message format (KEY)
│   │   ├── ProcessMsgBank.java ............... Operation executor (KEY)
│   │   ├── SendMessages.java ................. Message broadcaster (KEY)
│   │   └── ServicesBank.java ................. Menu service layer
│   └── interfaces/
│       ├── MenuEnum.java
│       ├── ServiceInterface.java
│       └── ServicesEnum.java
```

### Configuration

```
.vscode/
├── launch.json ....................... Debug configurations (2 options)
└── tasks.json ....................... Build task

.gitignore ........................... Ignore compiled files
lib/
└── jgroups-5.0.0.Final.jar .......... JGroups library (CRITICAL)
```

### Documentation

```
IMPLEMENTATION_SUMMARY.md ............ Executive summary
QUICK_REFERENCE.md .................. Quick lookup guide
JGROUPS_REQUIREMENTS_ANALYSIS.md .... How requirements are met
JGROUPS_TECHNICAL_DETAILS.md ........ Deep technical explanation
TESTING_GUIDE.md .................... Test procedures
DOCUMENTATION_INDEX.md .............. This file
```

---

## Getting Started (TL;DR)

### 1. Compile (30 seconds)

```powershell
javac -d bin -cp "lib\jgroups-5.0.0.Final.jar" `
  src\es\upm\dit\cnvr_fcon\bank_2025\bank\*.java `
  src\es\upm\dit\cnvr_fcon\bank_2025\common\*.java `
  src\es\upm\dit\cnvr_fcon\bank_2025\interfaces\*.java
```

### 2. Launch Node 1 (Terminal 1)

```powershell
java -cp "bin;lib\jgroups-5.0.0.Final.jar" `
  -Djava.net.preferIPv4Stack=true `
  -Djgroups.bind_addr=127.0.0.1 `
  es.upm.dit.cnvr_fcon.bank_2025.bank.MainBank BankCluster
```

Then: Enter `7` (init), then `5` (view)

### 3. Launch Node 2 (Terminal 2)

```powershell
java -cp "bin;lib\jgroups-5.0.0.Final.jar" `
  -Djava.net.preferIPv4Stack=true `
  -Djgroups.bind_addr=127.0.0.1 `
  es.upm.dit.cnvr_fcon.bank_2025.bank.MainBank BankCluster
```

Then: Enter `5` (view) → See same clients as Node 1!

### 4. Verify Coherence

- Create new client on Node 1 (option 1)
- View clients on Node 2 (option 5)
- New client appears immediately! ✓

---

## Requirements Status

### ✓ REQUIREMENT 1: COHERENCE

**Status:** IMPLEMENTED

"The value of the Bank replicas of the processes must be the same for all replicas"

**Mechanism:** Ordered multicast via JGroups NAKACK2 protocol
**Location:** NodeJG.receive() + FIFO message ordering
**Verification:** See TESTING_GUIDE.md "Requirement 1: COHERENCE Test"

### ✓ REQUIREMENT 2: STATE TRANSFER

**Status:** IMPLEMENTED

"The state of the Bank must be provided to a new process"

**Mechanism:** JGroups STATE_TRANSFER protocol (getState/setState)
**Location:** NodeJG.getState() and NodeJG.setState()
**Verification:** See TESTING_GUIDE.md "Requirement 2: STATE TRANSFER Test"

---

## Architecture at a Glance

```
┌─────────────────────────────────────┐
│      Bank Cluster (BankCluster)    │
│                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐
│  │  Node 1  │  │  Node 2  │  │  Node 3  │
│  │(Replica) │  │(Replica) │  │(Replica) │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘
│       │             │             │
│       │  ClientDB   │  ClientDB   │
│       │  (State)    │  (State)    │
│       │  Identical  │  Identical  │
│       │             │             │
│       └─────────────┼─────────────┘
│                     │
│          Multicast Operations
│      (NAKACK2 - Ordered Delivery)
│                     │
│  [CREATE_CLIENT] [UPDATE_CLIENT] [DELETE_CLIENT]
│
│  All nodes receive operations in SAME ORDER
│  All replicas execute identically
│  ✓ COHERENCE ACHIEVED
│
│  New node joins:
│  1. Requests current state (getState)
│  2. Receives ClientDB copy
│  3. Applies to local state (setState)
│  ✓ STATE TRANSFER ACHIEVED
└─────────────────────────────────────┘
```

---

## Recommended Study Order

1. **First 15 minutes**: Read IMPLEMENTATION_SUMMARY.md
2. **Next 15 minutes**: Read QUICK_REFERENCE.md
3. **Next 30 minutes**: Compile and run basic test
4. **Next 60 minutes**: Follow TESTING_GUIDE.md tests
5. **Next 60 minutes**: Read JGROUPS_REQUIREMENTS_ANALYSIS.md
6. **Next 120 minutes**: Read JGROUPS_TECHNICAL_DETAILS.md
7. **Review source code** with newfound understanding

---

## Support & Troubleshooting

### Common Issues

See: QUICK_REFERENCE.md → "Troubleshooting Matrix"
See: TESTING_GUIDE.md → "Troubleshooting"

### Deep Technical Questions

See: JGROUPS_TECHNICAL_DETAILS.md

### Testing Problems

See: TESTING_GUIDE.md → "Expected Log Patterns"
See: TESTING_GUIDE.md → "Troubleshooting"

### Architecture Questions

See: JGROUPS_REQUIREMENTS_ANALYSIS.md
See: QUICK_REFERENCE.md → "Architecture Diagram"

---

## File Statistics

| Document                         | Lines     | Topics                           | Time        |
| -------------------------------- | --------- | -------------------------------- | ----------- |
| IMPLEMENTATION_SUMMARY.md        | ~200      | Overview, Architecture, Status   | 5 min       |
| QUICK_REFERENCE.md               | ~400      | Commands, Reference, Diagram     | 15 min      |
| JGROUPS_REQUIREMENTS_ANALYSIS.md | ~400      | How requirements met, Guarantees | 60 min      |
| JGROUPS_TECHNICAL_DETAILS.md     | ~600      | Protocols, Stack, Examples       | 120 min     |
| TESTING_GUIDE.md                 | ~500      | Test steps, Expected output      | 90 min      |
| **TOTAL**                        | **~2100** | Comprehensive coverage           | **290 min** |

---

## Quick Links to Key Sections

### Coherence Implementation

- JGROUPS_REQUIREMENTS_ANALYSIS.md#requirement-1-coherence
- JGROUPS_TECHNICAL_DETAILS.md#how-coherence-is-achieved
- TESTING_GUIDE.md#requirement-1-coherence-test

### State Transfer Implementation

- JGROUPS_REQUIREMENTS_ANALYSIS.md#requirement-2-state-transfer
- JGROUPS_TECHNICAL_DETAILS.md#how-state-transfer-works
- TESTING_GUIDE.md#requirement-2-state-transfer-test

### Code Structure

- QUICK_REFERENCE.md#core-classes--responsibilities
- JGROUPS_TECHNICAL_DETAILS.md#message-flow-example

### Execution & Testing

- QUICK_REFERENCE.md#quick-start-30-seconds
- TESTING_GUIDE.md#quick-start-testing
- TESTING_GUIDE.md#expected-log-patterns

---

## Final Notes

- All documentation is current as of 2025-12-16
- Code has been compiled and tested successfully
- Both requirements (Coherence & State Transfer) are implemented and verified
- System is ready for testing and extension
- See IMPLEMENTATION_SUMMARY.md for comprehensive status

---

**Start with:** IMPLEMENTATION_SUMMARY.md (5 minutes)  
**Then read:** QUICK_REFERENCE.md (15 minutes)  
**Then try:** The quick start commands  
**Then verify:** TESTING_GUIDE.md tests

**Status: ✓ READY FOR EVALUATION**
