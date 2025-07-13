📘 Exam Invigilation Planner – Java Desktop App
Welcome to the Exam Invigilation Planner – a smart Java-based desktop application designed to automate and simplify the process of assigning teachers to exams. This project was built as part of a 6-week summer training to apply core concepts of logic building, Data Structures & Algorithms (DSA), and Java GUI development in a real-world educational scenario.

🧠 What It Does
Manually creating an invigilation schedule can be time-consuming and error-prone. This tool helps educational institutions automate the task by:

Taking input of exams and their time slots

Allowing entry of teachers and their unavailable slots

Using a backtracking algorithm to assign teachers in a way that avoids conflicts (like double booking or unavailable slots)

Generating a readable schedule output

Saving and loading past schedules (CSV format)

Providing printing and history features

All wrapped in a clean, dark-themed Java Swing GUI!

🎯 Key Features
✅ Add exams and teachers with time constraints
✅ Conflict-free scheduling using backtracking
✅ History management (save/load to CSV)
✅ Print the generated schedule directly
✅ Stylish and intuitive dark-themed UI
✅ User-friendly alerts, buttons, and validations

🛠️ Tech Stack
Language: Java

GUI Framework: Java Swing

Algorithm: Backtracking (for scheduling)

Data Storage: CSV file handling

Concepts Applied: OOP, DSA, File I/O, Exception Handling, Event-Driven Programming

🖥️ Screenshots
📋 Add here if you have screenshots or GIFs of the app running.

🚀 Getting Started
Prerequisites:
Java JDK (8 or above)

IntelliJ IDEA or any Java IDE

To Run:
Clone the repository

bash
Copy
Edit
git clone https://github.com/yourusername/exam-invigilation-planner.git
Open the project in IntelliJ IDEA

Run the ExamInvigilationPlanner.java file

Start adding exams and teachers, then hit "Generate Schedule" 🎉

📁 Project Structure
pgsql
Copy
Edit
📦 ExamInvigilationPlanner
 ┣ 📄 ExamInvigilationPlanner.java  --> Main application file
 ┣ 📄 ScheduleRecord (Inner class)  --> For saving schedule history
 ┣ 📄 InvigilationScheduler (Inner class) --> Backtracking logic
 ┣ 📄 CSV handling, GUI components, listeners, printing etc.
🎓 What I Learned
During the development of this project, I got hands-on experience with:

Designing modular and reusable code using Object-Oriented Programming

Writing and debugging recursive logic for constraint-based problems

Handling real-time user input and validations in GUI

Managing data persistence through file operations

Building polished interfaces with attention to UX

It was a great way to apply what I learned in my summer training on DSA and logic building, and turn theory into a working solution.

🙌 Acknowledgments
Big thanks to my mentors and trainers during the summer internship who provided guidance and support, and to the open-source community for inspiration and learning resources.

📌 Future Scope
Allow multi-teacher assignment per exam

Add login system for admin access

Export to PDF

Cloud-based storage

Integration with exam timetable systems

📃 License
This project is open-source for learning and educational use. Feel free to fork and modify it!
