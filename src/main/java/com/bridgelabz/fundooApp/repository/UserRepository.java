package com.bridgelabz.fundooApp.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.bridgelabz.fundooApp.model.User;
import java.lang.String;

public interface UserRepository extends MongoRepository<User, String> 
{
	Optional<User> findByEmailId(String emailId);
	Optional<User> findByUserId(String userid);
	
}
