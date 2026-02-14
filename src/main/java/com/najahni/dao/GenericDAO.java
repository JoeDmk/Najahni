package com.najahni.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic DAO interface defining standard CRUD operations.
 * @param <T> The entity type
 */
public interface GenericDAO<T> {

    /**
     * Creates a new entity in the database.
     * @param entity The entity to create
     * @return The created entity with generated ID
     */
    T create(T entity);

    /**
     * Retrieves an entity by its ID.
     * @param id The entity ID
     * @return Optional containing the entity if found
     */
    Optional<T> findById(int id);

    /**
     * Retrieves all entities from the database.
     * @return List of all entities
     */
    List<T> findAll();

    /**
     * Updates an existing entity in the database.
     * @param entity The entity to update
     * @return true if update was successful
     */
    boolean update(T entity);

    /**
     * Deletes an entity by its ID.
     * @param id The entity ID
     * @return true if deletion was successful
     */
    boolean delete(int id);
}
