# Graph Specifications

This document contains the workflow diagrams for the Brainiac AI memory system.

## 1. Update STM

```mermaid
graph LR
    Input[update STM request] --> ProcessRequest[process request]
    ProcessRequest -->|request is new event| AppendEvent[append event]
    ProcessRequest -->|request is new insight| AppendInsight[append insight]
    AppendEvent -->|new STM| BuildPrompt[build final prompt]
    AppendInsight -->|new STM| BuildPrompt
    BuildPrompt --> Output[prompt]
```

## 2. Promotion

```mermaid
graph LR
    Start --> DistillLTMs[distill new LTMs]
    DistillLTMs -->|list of LTM candidates| AppendLTMs[append or create new<br/>LTMs in mind map]
    AppendLTMs -->|list of updates| UpdateIndex[update mind map index]
    UpdateIndex --> ClearSTM[clear STM]
    ClearSTM -->|new STM| BuildPrompt[build final prompt]
    BuildPrompt --> Output[prompt]
```

## 3. Thinking Loop

```mermaid
graph TB
    Input[String] --> CallLLM[call llm]
    CallLLM -->|Message.Response is<br/>Tool.Call && tool ==<br/>update STM| UpdateSTM[update STM]
    UpdateSTM --> CallLLM
    UpdateSTM -->|prompt| Output[Output]
    CallLLM -->|Message.Response is Assistant| Output
    CallLLM -->|Message.Response &&<br/>context > max_size| Reflection[reflection]
    Reflection -->|Message.Response is<br/>Tool.Call| CallTool[call tool]
    Reflection -->|Message.Response is<br/>Assistant| Output
    CallTool --> CallLLM
    CallLLM -->|Message.Response is<br/>Tool.Call && tool ==<br/>update STM| UpdateSTM
```

## 4. Fetch LTM

```mermaid
graph LR
    Input[String] --> GetMindMap[get mind map<br/>file]
    GetMindMap -->|mind map file| AskLLM[ask LLM<br/>which files<br/>in mind map<br/>are useful]
    AskLLM -->|list of files| FetchFiles[fetch those<br/>files]
    FetchFiles --> Output[LTM files]
```

## 5. Build Final Prompt

```mermaid
graph LR
    Input[prompt, STM, LTM<br/>files] --> Build[build final prompt]
    Build --> Output[final prompt]
```

## 6. Setup (Overall Flow)

```mermaid
graph TB
    Start[start] -->|Prompt| FetchSTM[fetch STM file]
    FetchSTM -->|Prompt, STM| FetchLTM[Fetch LTM]
    FetchLTM -->|prompt, STM, LTM<br/>files| BuildPrompt[build final prompt]
    BuildPrompt -->|final prompt| ThinkingLoop[thinking loop]
    ThinkingLoop --> Reflection[reflection]
    Reflection --> Promotion[promotion]
    Promotion --> End[end]
```
