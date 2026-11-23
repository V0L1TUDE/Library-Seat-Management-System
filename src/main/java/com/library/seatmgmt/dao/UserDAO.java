package com.library.seatmgmt.dao;

import com.library.seatmgmt.model.Student;
import com.library.seatmgmt.model.User;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for User entities (Student & Admin).
 */
public interface UserDAO {
    Optional<User> findById(int id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<Student> findStudentByRollNumber(String rollNumber);
    boolean save(User user);
    boolean update(User user);
    List<Student> findAllStudents();
    int countTotalStudents();
}
