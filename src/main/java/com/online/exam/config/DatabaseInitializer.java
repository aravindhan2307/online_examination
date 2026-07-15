package com.online.exam.config;

import com.online.exam.entity.Question;
import com.online.exam.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final QuestionRepository questionRepository;

    @Autowired
    public DatabaseInitializer(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public void run(String... args) {
        if (questionRepository.count() == 0) {
            seedQuestions();
        }
    }

    private void seedQuestions() {
        List<Question> defaultQuestions = List.of(
             // Java (6)
             new Question(null, "What is the default value of a local variable in Java?", 
                 "null", "0", "Depends on variable type", "No default value (must be initialized)", 3, "java"),
             new Question(null, "Which keyword is used to prevent a method from being overridden in Java?", 
                 "static", "final", "abstract", "private", 1, "java"),
             new Question(null, "What is the size of an int data type in Java?", 
                 "1 byte", "2 bytes", "4 bytes", "8 bytes", 2, "java"),
             new Question(null, "Which of these is NOT a valid access modifier in Java?", 
                 "public", "private", "protected", "internal", 3, "java"),
             new Question(null, "Which class is the root of the class hierarchy in Java?", 
                 "String", "Object", "Class", "System", 1, "java"),
             new Question(null, "Which collection class allows unique elements and preserves insertion order in Java?", 
                 "HashSet", "LinkedHashSet", "TreeSet", "ArrayList", 1, "java"),
 
             // Web Development (6)
             new Question(null, "What does HTML stand for?", 
                 "Hyper Text Markup Language", "High Text Machine Language", "Hyperlink & Text Markup Language", "Hyper Tool Multi Language", 0, "web"),
             new Question(null, "Which CSS property is used to control the text size?", 
                 "text-style", "font-size", "text-size", "font-style", 1, "web"),
             new Question(null, "Inside which HTML element do we put the JavaScript code?", 
                 "<js>", "<script>", "<javascript>", "<scripting>", 1, "web"),
             new Question(null, "What is the correct CSS syntax to select an element with id 'demo'?", 
                 ".demo", "#demo", "demo", "*demo", 1, "web"),
             new Question(null, "Which JavaScript array method adds a new element to the end of an array?", 
                 "push()", "pop()", "shift()", "unshift()", 0, "web"),
             new Question(null, "What does JSON stand for?", 
                 "Java Source Open Network", "JavaScript Object Notation", "JQuery System Order Node", "JavaScript Oriented Name", 1, "web"),
 
             // General Knowledge (6)
             new Question(null, "Which planet is known as the Red Planet?", 
                 "Earth", "Mars", "Jupiter", "Saturn", 1, "general"),
             new Question(null, "Who wrote the play 'Romeo and Juliet'?", 
                 "William Shakespeare", "Charles Dickens", "Jane Austen", "Mark Twain", 0, "general"),
             new Question(null, "What is the capital city of France?", 
                 "London", "Berlin", "Rome", "Paris", 3, "general"),
             new Question(null, "What is the chemical symbol for Gold?", 
                 "Ag", "Fe", "Au", "Gd", 2, "general"),
             new Question(null, "How many continents are there on Earth?", 
                 "5", "6", "7", "8", 2, "general"),
             new Question(null, "Which is the largest ocean on Earth?", 
                 "Atlantic Ocean", "Indian Ocean", "Southern Ocean", "Pacific Ocean", 3, "general")
         );
 
         questionRepository.saveAll(defaultQuestions);
        System.out.println(">>> Seeded " + defaultQuestions.size() + " default questions into H2/MySQL database.");
    }
}
