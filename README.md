# Interpill — Personalised Drug–Drug Interaction Prototype

Designed and built independently as a final-year project, **Interpill** is a Kotlin Multiplatform mobile prototype for personalised drug–drug interaction analysis. The project includes:

- Multi-source data retrieval and validation (RxNorm, OpenFDA, DailyMed)
- Interaction analysis logic
- UX design and iterative prototyping
- Clear, structured outputs for users
- Usability study (N=34, MAUQ-based survey)


**Demo video:** [Interpill Demo](https://youtu.be/krAb9nkezV8)  

**Source code** will be published after academic assessment. This repository currently serves as a project overview and portfolio reference.

---

## 1. Prototype & Architecture

- **Early low-fidelity Figma prototype**  
  ![Prototype](assets/fig_01_prototype.png)


- **High-level architecture of the mobile application**

Illustrates the main functional layers of the mobile application, their interactions, and the boundaries between local processing and external services. The mobile application is structured into several logical layers, each responsible for a distinct aspect of the system’s functionality. This layered organisation is reflected both in the architectural model and in the package structure.

  ![Architecture](assets/fig_02_architecture.png)


- **Overall workflow of the test bed**

Full end-to-end workflow of the processing pipeline illustrates the complete sequence from medicine input (typed or obtained via OCR) through name normalisation, alias resolution, RXCUI lookup, and external-source querying, to the branch between the instructions module and the interaction-analysis module, and finally to the presentation of results on the application screens. The diagram represents the high-level flow of data across the functional components described in this subsection.
  
  ![Workflow](assets/fig_03_workflow.png)


- **Interaction-analysis pipeline**

The user provides two types of input: medication names and patient profile data (allergies and chronic conditions). These inputs follow separate processing paths. Medication names are passed to the InteractionsController component, while profile data are stored within the user profile and subsequently accessed by patient-specific rule sets during analysis. 

The InteractionsController component is responsible for preparing medication data for analysis. It normalises the entered names by mapping brand names and synonyms to standard International Nonproprietary Names (INNs), applies alias resolution, performs RXCUI lookup via RxNorm, and extracts the corresponding lists of active ingredients. As its output, it produces a list of normalised Drug objects, which are then passed to the central interaction engine. 

The core interaction engine, implemented as InteractionServiceDefault, acts as an aggregator of all interaction sources. At this stage, interaction data are obtained in parallel from the RxNav API, OpenFDA, DailyMed, class-based rule sets, and patient-specific rules. Each source returns a set of interaction items describing a drug pair, an estimated risk level, and a brief rationale (such as an official interaction record, a labelling excerpt, a class-based rule, or a user-related constraint). The aggregation stage performs merging and deduplication of overlapping interaction signals originating from different sources. Interaction items referring to the same drug pair are combined, and a consolidated set of notes is formed. These notes may include, for example, indications of duplicate active ingredients, applied heuristic rules, or patient-specific risk factors.

  
  ![Pipeline](assets/fig_04_pipeline.png)

*These diagrams illustrate the project structure, data entities, and the interaction-analysis workflow.*

---

## 2. Key User Screens & Scenarios

- **Combined user screens** (Main screen, OCR-based recognition, User Profile, Medication Schedule etc.)  
  ![User Screens Mix](assets/fig_05_user_screens.png)

*This image demonstrates the core user interface elements and main user flows.*


- **Interaction result scenarios** (risk indicators + AI summary)

  **Scenario 1** illustrates how recorded user allergies are incorporated into the interaction analysis workflow. The screenshots show a user profile containing documented allergy information and a scheduled medication entry in the My Meds Hub. When the medication is analysed, the system takes the allergy data into account, resulting in a contraindication warning displayed on the interaction results screen.

  **Scenario 2** demonstrates how recorded chronic conditions affect the outcome of interaction analysis. The screenshots show a user profile with a documented chronic condition and the subsequent analysis of a selected medication. The presence of the condition is reflected in the interaction results, leading to a contraindication warning and condition-specific explanatory text.
  
  ![Interaction Scenarios](assets/fig_06_interaction_result.png)

*This image shows example scenarios of personalised interaction analysis and risk visualisation.*

---

## 3. Usability Study & Findings

- **Bar charts from Google Forms** (MAUQ subscale scores & standard deviation)  
  ![MAUQ Scores](assets/fig_07_mauq_scores.png)

*These bar charts illustrate quantitative feedback on Ease of Use, Interface & Satisfaction, and Usefulness, collected from 34 participants using the MAUQ-based survey.*

---

## 4. Supporting Repositories

- **AI proxy service** (request routing and key isolation)  
  [https://github.com/olgaleobel/interpill-ai-proxy](https://github.com/olgaleobel/interpill-ai-proxy)  

- **Open data alias mapping for drug name normalisation**  
  [https://github.com/olgaleobel/interpill-open-data/blob/main/assets/aliases_extra.json](https://github.com/olgaleobel/interpill-open-data/blob/main/assets/aliases_extra.json)  

- **Rule-based interaction logic & class-level safety rules (public subset)**  
  [https://github.com/olgaleobel/interpill-open-data/blob/main/assets/class_rules.json](https://github.com/olgaleobel/interpill-open-data/blob/main/assets/class_rules.json)  

---

*Interpill demonstrates end-to-end design, implementation, and evaluation of a personalised drug interaction mobile application.*
