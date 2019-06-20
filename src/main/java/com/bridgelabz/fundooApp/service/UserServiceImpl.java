package com.bridgelabz.fundooApp.service;

import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.bridgelabz.fundooApp.dto.LoginDto;
import com.bridgelabz.fundooApp.dto.UserDto;
import com.bridgelabz.fundooApp.exception.UserException;
import com.bridgelabz.fundooApp.model.Email;
import com.bridgelabz.fundooApp.model.User;
import com.bridgelabz.fundooApp.repository.UserRepository;
import com.bridgelabz.fundooApp.response.Response;
import com.bridgelabz.fundooApp.utility.EncryptUtil;
import com.bridgelabz.fundooApp.utility.ITokenGenerator;
import com.bridgelabz.fundooApp.utility.MailUtil;


/**
 * @author admin123
 *
 */
@Service
public class UserServiceImpl implements UserService {

	/**
	 * 
	 */
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ModelMapper mapper;

	@Autowired
	private PasswordEncoder encoder;

	@Autowired
	MailUtil mailsender;

	@Autowired
	private ITokenGenerator tokenGenerator;

	@Override
	public String registrationUser(UserDto userDto, StringBuffer requestUrl) {

		boolean ismail = userRepository.findByEmailId(userDto.getEmailId()).isPresent();
		if (!ismail) {
			User user = mapper.map(userDto, User.class);
			user.setPassword(encoder.encode(userDto.getPassword()));
			//user.setCreationTime(LocalTime.now());
			try {
				User savedUser = userRepository.save(user);
				String token = tokenGenerator.generateToken(savedUser.getUserid());
				String activationUrl = getLink(requestUrl) + "/verification/" + token;
				Email email = new Email();
				email.setTo("musaddikshaikh10@gmail.com");
				email.setSubject("Account verification");
				email.setBody("Please verify your email id by using below link \n" + activationUrl);
				mailsender.send(email);
				return "Verification mail send successfully";
			} catch (Exception e) {
				e.printStackTrace();
				throw new UserException("Something not right");
			}
		} else {
			throw new UserException("User already exist");
		}
	}

	@Override
	public String loginUser(LoginDto loginDto) {

		Optional<User> optUser = userRepository.findByEmailId(loginDto.getEmailId());
		if (optUser.isPresent()) {
			User user = optUser.get();
			try {
				if (EncryptUtil.isPassword(loginDto, user)) {
					if(user.isVerified()) {
						return tokenGenerator.generateToken(user.getUserid());
					}else {
						throw new UserException("please verify your email");
					}
				} else {
					throw new UserException("incorrect password");
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new UserException("something went wrong");
			}
		} else {
			throw new UserException("User not found");
		}

	}

	@Override
	public Response forgetPassword(String emailId, StringBuffer requestUrl) {
		Optional<User> optUser = userRepository.findByEmailId(emailId);
		if (optUser.isPresent()) {
			User user = optUser.get();
			String id = user.getUserid();
			try {
				String token = tokenGenerator.generateToken(id);
				String resetUrl = getLink(requestUrl) + "/resetpassword/" + token;
				Email email = new Email();
				email.setTo("musaddikshaikh5@gmail.com");
				email.setSubject("resetPassword");
				email.setBody("reset your password \n" + resetUrl);
				mailsender.send(email);
				return new Response(200, "Mail Sent", null);

			} catch (Exception e) {

				e.printStackTrace();
				throw new UserException("internal server error");
			}
		} else {
			throw new UserException("User not present..");
		}

	}

	@Override
	public Response restSetPassword(String token, String password) {
		String userid = tokenGenerator.verifyToken(token);
		Optional<User> optUser = userRepository.findByUserId(userid);
		if (optUser.isPresent()) {
			User user = optUser.get();
			user.setPassword(encoder.encode(password));
			userRepository.save(user);
			return new Response(200, "password changed successfully..", null);

		}

		return new Response(400, "token not verified", null);
	}

	@Override
	public Response validateUser(String token) {
		String id = tokenGenerator.verifyToken(token);
		Optional<User> optionalUser = userRepository.findByUserId(id);
		if (optionalUser.isPresent()) {
			User user = optionalUser.get();
			user.setVerified(true);
			userRepository.save(user);
			return new Response(200, "User verified", null);
		} else {
			return new Response(400, "User not verified", null);
		}
	}

	@SuppressWarnings("unused")
	private String getLink(StringBuffer requestUrl) {
		String url = requestUrl.substring(0, requestUrl.lastIndexOf("/"));
		return url;
	}

}
