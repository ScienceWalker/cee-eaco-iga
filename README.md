# Project Title: Improved Genetic Algorithm (IGA)

## Project Overview
This project implements an improved genetic algorithm (IGA) for computation offloading strategies, along with five baseline algorithms (COM, GREEDY, GA, MA-SA, and MA-2OPT) for comparison. 

## Project Information
- Programming Language: Java
- Required JDK Version: Java 8 or higher
- Dependencies: Standard JRE libraries only
- Author: Lintao Duan, duanlintao@cdu.edu.cn

## Algorithms Included
- IGA: The proposed improved genetic algorithm with semantic-guided local search
- COM: The NSGA-III-based method
- GREEDY: Greedy-based offloading strategy
- GA: Standard genetic algorithm
- MA-SA: Memetic algorithm with simulated annealing as local search
- MA-2OPT: Memetic algorithm with 2-OPT as local search
 
## Importing Project into Eclipse
Open Eclipse and navigate to the File menu, then select "Import". From the import dialog, choose "General" followed by "Existing Projects into Workspace". Proceed by clicking "Next", then select "Select root directory" and browse to locate your project folder which contains the src directory, and finally click "Finish" to complete the import process.

## Project Structure
- src/: All source code files
- .classpath: Classpath configuration file
- .project: Metadata file

## Building the Project
- Source files are automatically compiled when saved
- Compilation output is stored in the bin/ directory

## Running the Application
- Locate the class containing the main method
- Right-click the class file -> "Run As" -> "Java Application"
