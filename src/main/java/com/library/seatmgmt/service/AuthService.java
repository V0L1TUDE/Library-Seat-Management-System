package com.library.seatmgmt.service;

import com.library.seatmgmt.dao.ActivityLogDAO;
import com.library.seatmgmt.dao.UserDAO;
import com.library.seatmgmt.dao.impl.ActivityLogDAOImpl;
import com.library.seatmgmt.dao.impl.UserDAOImpl;
import com.library.seatmgmt.exception.AuthenticationException;
import com.library.seatmgmt.model.ActivityLog;
import com.library.seatmgmt.model.Student;
import com.library.seatmgmt.model.User;
import com.library.seatmgmt.util.LoggerUtil;
import com.library.seatmgmt.util.PasswordUtil;
import com.library.seatmgmt.util.SessionContext;
import com.library.seatmgmt.util.ValidationUtil;

import java.util.Optional;

/**
 * Service orchestrating authentication, user registrations, and session tracking.
 */
public class AuthService {

    private final UserDAO userDAO;
    private final ActivityLogDAO logDAO;

    public AuthService() {
        this(new UserDAOImpl(), new ActivityLogDAOImpl());
    }

    public AuthService(UserDAO userDAO, ActivityLogDAO logDAO) {
        this.userDAO = userDAO;
        this.logDAO = logDAO;
    }

    /**
     * Authenticates a user against MySQL database records using SHA-256 password verification.
     */
    public User login(String username, String rawPassword) throws AuthenticationException {
        if (!ValidationUtil.isNotEmpty(username) || !ValidationUtil.isNotEmpty(rawPassword)) {
            throw new AuthenticationException("Username and password cannot be empty.");
        }

        Optional<User> userOpt = userDAO.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            LoggerUtil.warning("Failed login attempt for non-existent username: " + username);
            throw new AuthenticationException("Invalid username or password.");
        }

        User user = userOpt.get();
        if (!PasswordUtil.verifyPassword(rawPassword, user.getPasswordHash())) {
            LoggerUtil.warning("Invalid password attempt for user: " + username);
            throw new AuthenticationException("Invalid username or password.");
        }

        // Set session context
        SessionContext.getInstance().setCurrentUser(user);
        SessionContext.getInstance().getUserPreferences().setRememberedUsername(user.getUsername());
        SessionContext.getInstance().saveUserPreferences();

        // Audit log
        logDAO.save(new ActivityLog(user.getId(), "USER_LOGIN", "User logged in: " + user.getUsername()));
        LoggerUtil.info("User authenticated successfully: " + user.getUsername() + " (" + user.getRole() + ")");

        return user;
    }

    /**
     * Registers a new student account.
     */
    public boolean registerStudent(Student student, String rawPassword) throws AuthenticationException {
        if (!ValidationUtil.isValidUsername(student.getUsername())) {
            throw new AuthenticationException("Username must be 3-30 alphanumeric characters.");
        }
        if (!ValidationUtil.isValidEmail(student.getEmail())) {
            throw new AuthenticationException("Invalid email format.");
        }
        if (!ValidationUtil.isNotEmpty(rawPassword) || rawPassword.length() < 6) {
            throw new AuthenticationException("Password must be at least 6 characters long.");
        }

        if (userDAO.findByUsername(student.getUsername()).isPresent()) {
            throw new AuthenticationException("Username is already taken.");
        }
        if (userDAO.findByEmail(student.getEmail()).isPresent()) {
            throw new AuthenticationException("Email is already registered.");
        }
        if (student.getStudentIdNumber() != null &&
                userDAO.findStudentByRollNumber(student.getStudentIdNumber()).isPresent()) {
            throw new AuthenticationException("Student ID number is already registered.");
        }

        student.setPasswordHash(PasswordUtil.hashPassword(rawPassword));
        boolean saved = userDAO.save(student);
        if (saved) {
            logDAO.save(new ActivityLog(student.getId(), "STUDENT_REGISTER", "New student registered: " + student.getUsername()));
            LoggerUtil.info("New student registered: " + student.getUsername());
        }
        return saved;
    }

    /**
     * Terminates the active session.
     */
    public void logout() {
        User currentUser = SessionContext.getInstance().getCurrentUser();
        if (currentUser != null) {
            logDAO.save(new ActivityLog(currentUser.getId(), "USER_LOGOUT", "User logged out: " + currentUser.getUsername()));
            LoggerUtil.info("User logged out: " + currentUser.getUsername());
        }
        SessionContext.getInstance().clearSession();
    }
}
