# Farm Management Dashboard & Drone Automation
Agricultural drone dashboard for crop and equipment monitoring (CS 420/520 Term Project).

##  Repository Directory Structure

```text
src/
├── model/
│   ├── FarmItemComponent.java  # Common Composite Interface
│   ├── FarmItem.java           # Leaf Node (Items)
│   ├── FarmContainer.java      # Composite Node (Containers)
│   └── ScenarioBuilder.java    # Required Test Hierarchy Setup
├── drone/
│   └── TelloController.java    # UDP Hardware Bridge for Tello TLW004
└── view/                       # UI & Canvas Components
