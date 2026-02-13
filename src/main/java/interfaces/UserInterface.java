package interfaces;

import exceptions.*;
import models.User;

import java.util.List;

public interface UserInterface {
    public void addUser(User p) throws EmptyFieldException, InvalidEmailException, IncorrectPasswordException, InvalidPhoneNumberException,CustomIllegalStateException;
    public void updateUser( User p) throws EmptyFieldException, InvalidPhoneNumberException, InvalidEmailException, IncorrectPasswordException, UserNotFoundException;
    public void deleteUser(int id) throws UserNotFoundException;

    public List<User> getUsers();

    public User getUserbyID(int id) throws UserNotFoundException;
    public User getUserbyEmail(String email) throws UserNotFoundException;}
