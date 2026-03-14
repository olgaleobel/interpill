# Interpill — Personalised Drug–Drug Interaction Prototype

Designed and built independently as a final-year project, **Interpill** is a Kotlin Multiplatform mobile prototype for personalised drug–drug interaction analysis. The project includes:

- Multi-source data retrieval and validation (RxNorm, OpenFDA, DailyMed)
- Interaction analysis logic
- Clear, structured outputs for users
- Usability study (N=34, MAUQ-based survey)
- UX design and iterative prototyping

**Demo video:** [Interpill Demo](https://youtu.be/krAb9nkezV8)  

**Source code** will be published after academic assessment. This repository currently serves as a project overview and portfolio reference.

---

## 1. Prototype & Architecture

- **Early low-fidelity Figma prototype**  
  ![Prototype](assets/fig_01_prototype.png)

- **High-level architecture of the mobile application**  
  ![Architecture](assets/fig_02_architecture.png)

- **Overall workflow of the test bed**  
  ![Workflow](assets/fig_03_workflow.png)

- **Interaction-analysis pipeline**  
  ![Pipeline](assets/fig_04_pipeline.png)

*These diagrams illustrate the project structure, data entities, and the interaction-analysis workflow.*

---

## 2. Key User Screens & Scenarios

- **Combined user screens** (Main screen, Medication Schedule, OCR-based recognition, User Profile)  
  ![User Screens Mix](assets/fig_05_user_screens.png)

*This image demonstrates the core user interface elements and main user flows.*

- **Interaction result scenarios** (risk indicators + AI summary)  
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




# interpill

Designed and built the full mobile prototype independently as a final-year project, including:
• data pipeline design
• interaction analysis logic
• API integration
• UX structure
• usability testing

Usability study conducted with 34 participants (MAUQ-based survey).
Quantitative and qualitative feedback analysed.

Source code will be published after academic assessment is completed.
This repository currently serves as a project overview and portfolio reference.

Interpill demo: https://youtu.be/krAb9nkezV8

Supporting components (open repositories)

Some supporting components of the project are available as separate open repositories:

• AI proxy service (request routing and key isolation):
https://github.com/olgaleobel/interpill-ai-proxy

• Open data alias mapping used for drug name normalisation:
https://github.com/olgaleobel/interpill-open-data/blob/main/assets/aliases_extra.json

• Rule-based interaction logic and class-level safety rules (public subset):
https://github.com/olgaleobel/interpill-open-data/blob/main/assets/class_rules.json
